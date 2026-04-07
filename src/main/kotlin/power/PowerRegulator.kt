package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.battery.DalyDiscovery
import de.klg71.solarman_sensor.battery.Measurement
import de.klg71.solarman_sensor.battery.MqttPublisher
import de.klg71.solarman_sensor.battery.ReconnectableMqttClient
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.solarman.SolarCommunicator
import de.klg71.solarman_sensor.solarman.SolarInfo
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
internal class PowerRegulator(
    private val objectMapper: ObjectMapper, private val dispatcher: CoroutineDispatcher,
    private val solarCommunicator: SolarCommunicator,
    private val mqttClient: ReconnectableMqttClient,
    private val dalyDiscovery: DalyDiscovery
) {
    private lateinit var smartMeterClient: BitshakeClient
    private val mqttPublisher = MqttPublisher(mqttClient, "solar_inverter", "solar_inverter", objectMapper)

    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(PowerRegulator::class.java)

    @OptIn(ExperimentalAtomicApi::class)
    private val currentSetPower = AtomicInt(1600)
    private val POWER_THRESHOLD = 100

    @OptIn(ExperimentalAtomicApi::class)
    @PostConstruct
    fun init() {
        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "mppt1-voltage", "mppt1-voltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.CURRENT, "mppt1-current", "mppt1-current")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "mppt1-power", "mppt1-power")

        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "mppt2-voltage", "mppt2-voltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.CURRENT, "mppt2-current", "mppt2-current")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "mppt2-power", "mppt2-power")

        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "mppt3-voltage", "mppt3-voltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.CURRENT, "mppt3-current", "mppt3-current")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "mppt3-power", "mppt3-power")

        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "mppt4-voltage", "mppt4-voltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.CURRENT, "mppt4-current", "mppt4-current")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "mppt4-power", "mppt4-power")

        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "ac-voltage", "ac-voltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.ENERGY, "daily-production", "daily-production")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "total-power", "total-power")
        mqttPublisher.homeAssistantDiscovery(Measurement.POWER, "set-power", "set-power")

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
                publishInfo()
            } catch (e: Throwable) {
                logger.error("Error while controlling power", e)
            }
            delay(500)
        }
    }

    private suspend fun publishInfo() {
        solarCommunicator.readSolarInfo()?.let {
            publish(it)
        }
    }

    private fun publish(solarInfo: SolarInfo) {
        mqttPublisher.publish("/total-power", solarInfo.totalPower)
        mqttPublisher.publish("/ac-voltage", solarInfo.acVoltage)
        mqttPublisher.publish("/daily-production", solarInfo.dailyProduction)
        solarInfo.strings.forEach {
            mqttPublisher.publish("/mppt${it.id + 1}-voltage", it.voltage)
            mqttPublisher.publish("/mppt${it.id + 1}-current", it.current)
            mqttPublisher.publish("/mppt${it.id + 1}-power", it.voltage * it.current)
        }
    }

    private val INVERTER_OFFSET = 200

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun controlPower() {
        smartMeterClient.getCurrentPower().statusSNS.SGM.power.let {
            if ((it < -POWER_THRESHOLD && currentSetPower.load() > 200) || currentSetPower.load() > upperPowerLimit()) {
                logger.info("Inverter produces too much power, decreasing now")
                val newPower = (currentSetPower.load() + it).let {
                    limitPower(it - INVERTER_OFFSET)
                }

                solarCommunicator.setPower(newPower)
                mqttPublisher.publish("/set-power", newPower)
                currentSetPower.store(newPower)
                delay(10000)
                logger.info("Decreased to: {}", newPower)
            }
            if (it > POWER_THRESHOLD && currentSetPower.load() < upperPowerLimit()) {
                logger.info("Inverter produces too little power, increasing now")
                val newPower = (currentSetPower.load() + it).let {
                    limitPower(it)
                }
                solarCommunicator.setPower(newPower)
                mqttPublisher.publish("/set-power", newPower)
                currentSetPower.store(newPower)
                logger.info("Increased to: {}", newPower)
                delay(10000)
            }

        }
    }

    private val INVERTER_UPPER_LIMIT = 2000
    private val INVERTER_UPPER_LIMIT_40_SOC = 800
    private val INVERTER_UPPER_LIMIT_25_SOC = 400
    private val INVERTER_LOWER_LIMIT = 200
    private fun limitPower(power: Int) =
        when {
            power < 200 -> INVERTER_LOWER_LIMIT
            power > upperPowerLimit() -> upperPowerLimit()
            else -> power
        }

    private fun upperPowerLimit(): Int = if (dalyDiscovery.socAverage() < 25) {
        INVERTER_UPPER_LIMIT_25_SOC
    } else if (dalyDiscovery.socAverage() < 40) {
        INVERTER_UPPER_LIMIT_40_SOC
    } else {
        INVERTER_UPPER_LIMIT
    }

    @OptIn(ExperimentalAtomicApi::class)
    public fun getCurrentSetPower() = currentSetPower.load()


}
