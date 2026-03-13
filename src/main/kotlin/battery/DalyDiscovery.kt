package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import org.eclipse.paho.client.mqttv3.MqttClient
import org.springframework.stereotype.Component

data class DalyDeviceInfo(val address: String, val heatingPin: Int? = null)

@Component
class DalyDiscovery(
    private val objectMapper: ObjectMapper,
    private val dispatcher: CoroutineDispatcher,
    private val client: MqttClient
) {
    private val heatingPins = mapOf<String, Int>()
    private val deviceMap = mutableMapOf<String, DalyDevice>()
    private val logger = getLogger(DalyDiscovery::class.java)
    private val mutex = Mutex()

    @PostConstruct
    fun init() {
        val batteryConnector = BatteryConnector("", mutex)
        batteryConnector.init()
        batteryConnector.devices().filter {
            it.name.startsWith("JHB-")
        }.forEach {
            logger.info("Starting to monitor device: ${it.name}")
            deviceMap[it.address] =
                DalyDevice(
                    dispatcher,
                    client,
                    objectMapper,
                    it.address,
                    it.name.replace("-", "_"),
                    mutex,
                    heatingPins[it.address]
                ).also {
                    it.init()
                }
        }
    }
    @PreDestroy
    fun tearDown(){
        deviceMap.forEach { (_, device) ->device.tearDown() }
    }
}