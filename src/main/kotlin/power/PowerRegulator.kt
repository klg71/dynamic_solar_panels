package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.solarman.SolarCommunicator
import de.klg71.solarman_sensor.solarman.setPower
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Service
internal class PowerRegulator(private val objectMapper: ObjectMapper, private val dispatcher: CoroutineDispatcher,
    private val solarCommunicator: SolarCommunicator) {
    private lateinit var client: HomeAssistantClient
    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"
    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(PowerRegulator::class.java)

    @OptIn(ExperimentalAtomicApi::class)
    private val currentSetPower = AtomicInt(1600)
    private val POWER_STEPPING = 50

    @PostConstruct
    fun init() {
        client = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            requestInterceptor {
                it.header("Authorization","Bearer $token")
            }
            target(HomeAssistantClient::class.java, "https://homeassistant.tail592ffe.ts.net")
        }
        scope.launch { controlPowerJob() }
    }

    private suspend fun controlPowerJob() {
        logger.info("Starting power control job")
        while (true) {
            try {
                controlPower()
            } catch (e: Throwable) {
                logger.error("Error while controlling power", e)
            }
            delay(10)
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun controlPower() {
        client.getCurrentPower().state.toInt().let {
            if (it < POWER_STEPPING*2) {
                logger.info("Inverter produces too much power, decreasing now")
                val newPower = currentSetPower.load() - POWER_STEPPING
                if (newPower > 200) {
                    solarCommunicator.setPower(newPower)
                    currentSetPower.store(newPower)
                    logger.info("Decreased to: {}",newPower)
                }
            }
            if(it>POWER_STEPPING*2){
                logger.info("Inverter produces too little power, increasing now")
                val newPower = currentSetPower.load() + POWER_STEPPING
                if (newPower < 2000) {
                    solarCommunicator.setPower(newPower)
                    currentSetPower.store(newPower)
                    logger.info("Increased to: {}",newPower)
                }

            }

        }
    }


}
