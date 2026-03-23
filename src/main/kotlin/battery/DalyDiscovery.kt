package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.eclipse.paho.client.mqttv3.MqttClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DalyDeviceInfo(val address: String, val heatingPin: Int? = null)

@Component
class DalyDiscovery(
    private val objectMapper: ObjectMapper,
    private val dispatcher: CoroutineDispatcher,
    private val client: MqttClient
) {
    private val heatingPins = mapOf<String, Int>()
    private val deviceMap = ConcurrentHashMap<String, DalyDevice>()
    private val logger = getLogger(DalyDiscovery::class.java)
    private val mutex = Mutex()
    private val scope = CoroutineScope(dispatcher)

    fun socAverage() = deviceMap.values.map { it.currentSoc() }.average().toInt()


    suspend fun reset() {
        val session = SSHClient().also { client ->
            client.addHostKeyVerifier(PromiscuousVerifier())
            client.connect("garage-pi")
            client.authPassword("lukas", "1805Rh")
        }.startSession()

        val cmd = session.exec("/bin/bash /home/lukas/bluetooth_reset.sh")
        cmd.join(10, TimeUnit.SECONDS)
        session.close()
        logger.info("Resetted Bluetooth")
    }

    @PostConstruct
    fun init() {
        val batteryConnector = BatteryConnector("", mutex)
        runBlocking {
            reset()
            batteryConnector.init()
        }
        scope.launch {
            discoveryJob(batteryConnector)
        }
    }

    private suspend fun discoveryJob(batteryConnector: BatteryConnector) {
        while (true) {
            try {
                updateDevices(batteryConnector)
            } catch (e: Exception) {
                logger.warn("Error while updating devices", e)
            }
            delay(Duration.ofMinutes(5).toMillis())
        }
    }

    private suspend fun updateDevices(batteryConnector: BatteryConnector) {
        batteryConnector.devices().filter {
            it.name.startsWith("JHB-")
        }.filter { !deviceMap.containsKey(it.address) }.forEach {
            logger.info("Starting to monitor device: ${it.name}")
            deviceMap[it.address] = initDaly(it)
        }
        deviceMap.forEach { (address, device) ->
            if ((System.currentTimeMillis() - device.getLastUpdated()) > Duration.ofMinutes(5).toMillis()) {
                device.tearDown()
                deviceMap.remove(address)
                logger.info("Removing device ${device.name} because it was not updated.")
            }
        }
    }

    private fun initDaly(device: BtDevice): DalyDevice = DalyDevice(
        dispatcher,
        client,
        objectMapper,
        device.address,
        device.name.replace("-", "_"),
        mutex,
        heatingPins[device.address]
    ).also {
        it.init()
    }

    @PreDestroy
    fun tearDown() {
        scope.cancel()
        deviceMap.forEach { (_, device) -> device.tearDown() }
    }
}