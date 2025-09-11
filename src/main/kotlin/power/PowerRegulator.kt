package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.solarman.SolarCommunicator
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
    private lateinit var smartMeterClient: BitshakeClient

    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(PowerRegulator::class.java)

    @OptIn(ExperimentalAtomicApi::class)
    private val currentSetPower = AtomicInt(1600)
    private val POWER_THRESHOLD = 100

    @OptIn(ExperimentalAtomicApi::class)
    @PostConstruct
    fun init() {
        smartMeterClient = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            target(BitshakeClient::class.java, "http://192.168.178.79")
        }
        scope.launch {
            solarCommunicator.readSolarInfo()?.let {
                currentSetPower.store(it.totalPower.toInt())
            }
            controlPowerJob()
        }
    }

    private suspend fun controlPowerJob() {
        logger.info("Starting power control job")
        while (true) {
            try {
                controlPower()
            } catch (e: Throwable) {
                logger.error("Error while controlling power", e)
            }
            delay(500)
        }
    }

    private val INVERTER_OFFSET = 200

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun controlPower() {
        smartMeterClient.getCurrentPower().statusSNS.SGM.power.let {
            if (it < -POWER_THRESHOLD && currentSetPower.load() > 200) {
                logger.info("Inverter produces too much power, decreasing now")
                val newPower = (currentSetPower.load() + it).let {
                    limitPower(it - INVERTER_OFFSET)
                }

                solarCommunicator.setPower(newPower)
                currentSetPower.store(newPower)
                delay(10000)
                logger.info("Decreased to: {}", newPower)
            }
            if (it > POWER_THRESHOLD && currentSetPower.load() < 2000) {
                logger.info("Inverter produces too little power, increasing now")
                val newPower = (currentSetPower.load() + it).let {
                    limitPower(it)
                }
                solarCommunicator.setPower(newPower)
                currentSetPower.store(newPower)
                logger.info("Increased to: {}", newPower)
                delay(10000)
            }

        }
    }

    private val INVERTER_UPPER_LIMIT = 2000
    private val INVERTER_LOWER_LIMIT = 200
    private fun limitPower(power: Int) =
        when {
            power < 200 -> INVERTER_LOWER_LIMIT
            power > 2000 -> INVERTER_UPPER_LIMIT
            else -> power
        }

    @OptIn(ExperimentalAtomicApi::class)
    public fun getCurrentSetPower() = currentSetPower.load()


}
