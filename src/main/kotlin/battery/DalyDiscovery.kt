package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import org.eclipse.paho.client.mqttv3.MqttClient
import org.springframework.stereotype.Component

data class DalyDeviceInfo(val address: String, val heatingPin: Int? = null)

@Component
class DalyDiscovery(
    private val batteryConnector: BatteryConnector,
    private val objectMapper: ObjectMapper,
    private val dispatcher: CoroutineDispatcher,
    private val client: MqttClient
) {
    private val availableDevices = listOf(
        DalyDeviceInfo(
            "D2:19:08:01:16:96"
        )
    )
    private val deviceMap = mutableMapOf<String, DalyDevice>()

    @PostConstruct
    fun init() {
        availableDevices.forEach {
            deviceMap[it.address] =
                DalyDevice(batteryConnector, dispatcher, client, objectMapper, it.address, it.heatingPin)
        }
    }
}