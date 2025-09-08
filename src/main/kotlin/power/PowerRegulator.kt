package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
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
internal class PowerRegulator(private val objectMapper: ObjectMapper, private val dispatcher: CoroutineDispatcher) {
    private lateinit var client: HomeAssistantClient
    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"
    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(PowerRegulator::class.java)
    @OptIn(ExperimentalAtomicApi::class)
    private val currentSetPower= AtomicInt(1600)

    @PostConstruct
    fun init() {
        client = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            requestInterceptor {
                it.header("Authorization", "Bearer: $token")
            }
            target(HomeAssistantClient::class.java, "https://homeassistant.tail592ffe.ts.net")
        }
        scope.launch { controlPowerJob() }
    }

    private suspend fun controlPowerJob() {
        while (true) {
            try {
                controlPower()
            } catch (e: Throwable) {
                logger.error("Error while controlling power", e)
            }
            delay(10)
        }
    }

    private fun controlPower() {
        client.getCurrentPower().state.toInt().let {
            if(it<0){
                logger.info("Inverter produces too much power, decreasing now")

            }

        }
    }


}
