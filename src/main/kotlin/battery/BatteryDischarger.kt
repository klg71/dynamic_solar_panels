package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.springframework.stereotype.Component
import kotlin.concurrent.atomics.ExperimentalAtomicApi

fun Boolean.toHaState() = if (this) {
    "on"
} else {
    "off"
}

fun fromHaState(state: String) = state.lowercase() == "on"

@OptIn(ExperimentalAtomicApi::class)
@Component
class BatteryDischarger(
    private val batteryConnector: BatteryConnector,
    private val mqttClient: MqttClient,
    private val dalyDevice: DalyDevice,
    private val dispatcher: CoroutineDispatcher,
    objectMapper: ObjectMapper
) {

    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(BatteryDischarger::class.java)
    private val mqttPublisher = MqttPublisher(mqttClient, "discharge", "battery-discharger", objectMapper)

    companion object {
        private const val BATTERY_DISCHARGE_PIN = 7
        private const val DISCHARGE_THRESHOLD = 10
        private const val CHARGE_THRESHOLD = 50
    }

    @PostConstruct
    fun init() {

        mqttPublisher.homeAssistantDiscovery(Measurement.BINARY_POWER, "power", "power")

        scope.launch {
            watchSoc()
        }
    }

    private suspend fun watchSoc() {
        while (true) {
            try {
                tick()
            } catch (e: Throwable) {
                logger.error("Error while watching battery soc", e)
            }
            delay(1000)
        }
    }

    private fun tick() {
        val currentState = batteryConnector.getPin(BATTERY_DISCHARGE_PIN)
        val desiredState = if (currentState) {
            dalyDevice.currentSoc() < DISCHARGE_THRESHOLD
        } else {
            dalyDevice.currentSoc() > CHARGE_THRESHOLD
        }
        if (currentState != desiredState) {
            batteryConnector.setPin(BATTERY_DISCHARGE_PIN, desiredState)
            logger.info("Set Discharge Relais to: $desiredState")
            mqttPublisher.publish("/power", desiredState.toHaState())
        }
    }
}