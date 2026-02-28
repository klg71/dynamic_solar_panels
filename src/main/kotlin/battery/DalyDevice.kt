package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.solarman.CRC16Modbus
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi


@OptIn(ExperimentalUnsignedTypes::class, ExperimentalAtomicApi::class)
class DalyDevice(
    private val batteryConnector: BatteryConnector, private val dispatcher: CoroutineDispatcher,
    client: MqttClient,
    objectMapper: ObjectMapper,
    private val deviceAddress: String,
    private val heatingPin: Int?
) {
    private val logger = getLogger(DalyDevice::class.java)
    private var found = false
    private lateinit var name: String
    private val numberOfTempSensors = 2
    private val shouldConnect = AtomicBoolean(true)
    private val currentSoc = AtomicReference<Double>(0.0)
    private val mqttPublisher = MqttPublisher(client, "daly", name, objectMapper)

    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        scope.launch { watcher() }
        scope.launch { monitor() }
        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "total-voltage", "totalVoltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.CURRENT, "total-current", "totalCurrent")
        mqttPublisher.homeAssistantDiscovery(Measurement.PERCENTAGE, "soc", "soc")
        mqttPublisher.homeAssistantDiscovery(Measurement.TEMPERATURE, "temperature_0", "temperature_1")
        mqttPublisher.homeAssistantDiscovery(Measurement.TEMPERATURE, "temperature_1", "temperature_2")
        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "min-cell-voltage", "minCellVoltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.VOLT, "max-cell-voltage", "maxCellVoltage")
        mqttPublisher.homeAssistantDiscovery(Measurement.STRING, "state", "state")
        mqttPublisher.homeAssistantDiscovery(Measurement.STRING, "errors", "errors")
        mqttPublisher.homeAssistantDiscovery(Measurement.STRING, "last-update", "lastUpdate")
        mqttPublisher.homeAssistantDiscoverySwitch("charge-mos", "charge-mos/set", "chargeMos")
        mqttPublisher.subscribe("/charge-mos/set", ::setChargeMos)
        mqttPublisher.homeAssistantDiscoverySwitch("discharge-mos", "discharge-mos/set", "dischargeMos")

        mqttPublisher.subscribe("/discharge-mos/set", ::setDischargeMos)

        mqttPublisher.homeAssistantDiscoverySwitch("heating", "heating/set", "heating")
        mqttPublisher.subscribe("/heating/set", ::setHeating)
        mqttPublisher.homeAssistantDiscoverySwitch("connected", "connected/set", "connected")
        mqttPublisher.subscribe("/connected/set", ::setConnected)
    }

    private fun setDischargeMos(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            scope.launch {
                if (fromHaState(it)) {
                    setCommand06(0x0122, 1)
                } else {
                    setCommand06(0x0122, 0)
                }
            }
        }
    }

    private fun setChargeMos(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            scope.launch {
                if (fromHaState(it)) {
                    setCommand06(0x0121, 1)
                } else {
                    setCommand06(0x0121, 0)
                }
            }
        }
    }

    @Suppress("unused")
    private fun setHeating(topic: String, message: MqttMessage) {
        heatingPin?.let { heatingPin ->
            message.payload.toString(Charset.defaultCharset()).let {
                batteryConnector.setPin(heatingPin, fromHaState(it))
                publishHeating()
            }
        }
    }

    @Suppress("unused")
    private fun setConnected(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            shouldConnect.store(fromHaState(it))
            batteryConnector.disconnect()
            publishConnected()
        }
    }


    private suspend fun monitor() {
        while (true) {
            try {
                if (found && shouldConnect.load()) {
                    publishInfo()
                }
            } catch (e: Throwable) {
                logger.error("Error while watching battery", e)
            }
            delay(Duration.ofSeconds(10).toMillis())
        }
    }

    private suspend fun sendRequest(address: Int): List<Int> {
        val buffer = ByteArray(13)
        buffer[0] = 0xA5.toByte()
        buffer[1] = 0x40
        buffer[2] = address.toByte()
        buffer[3] = 0x08
        for (i in 4..12) {
            buffer[i] = 0x00
        }

        var checksum: Byte = 0
        for (i in 0..11) {
            checksum = ((checksum + buffer[i]) % 256).toByte()
        }

        buffer[12] = checksum
        var answer = batteryConnector.sendRequest(deviceAddress, buffer)
        while (answer[2] != address) {
            delay(1000)
            answer = batteryConnector.sendRequest(deviceAddress, buffer, clean = true)
        }
        return answer
    }

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private suspend fun publishInfo() {
        publishSoc()
        publishErrors()
        publishTemp()
        publishMinMaxCellVoltage()
        publishMosState()
        publishHeating()
        publishConnected()
    }

    private fun publishHeating() {
        heatingPin?.let { heatingPin ->
            batteryConnector.getPin(heatingPin).let {
                mqttPublisher.publish(
                    "/heating", if (it) {
                        "ON"
                    } else {
                        "OFF"
                    }
                )
            }
        }
    }

    private fun publishConnected() = mqttPublisher.publish(
        "/connected", shouldConnect.load().toHaState()
    )

    private suspend fun publishSoc() {
        sendRequest(0x90).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x90) {
                return
            }
            val cumVoltage = (it[4] * 256 + it[5]) * 0.1
            (it[6] * 256 + it[7]) * 0.1
            val current = (it[8] * 256 + it[9]) * 0.1 - 3000
            val soc = (it[10] * 256 + it[11]) * 0.1
            mqttPublisher.publish("/soc", soc)
            currentSoc.store(current)
            mqttPublisher.publish("/total-current", current)
            mqttPublisher.publish("/total-voltage", cumVoltage)

            mqttPublisher.publish("/last-update", LocalDateTime.now().format(formatter))
        }
    }

    private suspend fun publishMosState() {
        sendRequest(0x93).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x93) {
                return
            }
            val state = when (it[4]) {
                0 -> "Stationary"
                1 -> "Charge"
                2 -> "Discharge"
                else -> ""
            }
            val chargeMos = (it[5] == 1).toHaState()
            val dischargeMos = (it[6] == 1).toHaState()
            mqttPublisher.publish("/state", state)
            mqttPublisher.publish("/charge-mos", chargeMos, true)
            mqttPublisher.publish("/discharge-mos", dischargeMos, true)
        }
    }

    private suspend fun publishMinMaxCellVoltage() {
        sendRequest(0x91).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x91) {
                return
            }
            val maxCell = (it[4] * 256 + it[5]) * 0.001
            val maxCellNr = it[6]
            val minCell = (it[7] * 256 + it[8]) * 0.001
            val minCellNr = it[9]
            mqttPublisher.publish("/max-cell-voltage", maxCell)
            mqttPublisher.publish("/max-cell-number", maxCellNr)
            mqttPublisher.publish("/min-cell-voltage", minCell)
            mqttPublisher.publish("/min-cell-number", minCellNr)
        }
    }

    private suspend fun publishTemp() {
        sendRequest(0x96).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x96) {
                return
            }
            val len = it.size

            for (k in 0..<(len / 13)) {
                for (tempIndex in 0..7) {
                    val temperature: Int = it[k * 13 + 5 + tempIndex] - 40
                    mqttPublisher.publish("/temperature_${k * 13 + tempIndex}", temperature)
                    if ((k * 13 + tempIndex + 1) >= numberOfTempSensors) {
                        return
                    }
                }
            }
        }
    }

    private suspend fun publishErrors() {
        sendRequest(0x98).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x98) {
                return
            }
            parseErrorCodes(it).joinToString(",").let {
                mqttPublisher.publish("/errors", it)
            }
        }

    }


    suspend fun watcher() {
        while (true) {
            try {
                batteryConnector.devices().firstOrNull { it.address == deviceAddress }?.let {
                    found = true
                    name = it.name
                }
            } catch (e: Throwable) {
                logger.error("Error while watching battery", e)
            }
            delay(Duration.ofMinutes(2).toMillis())
        }
    }

    private suspend fun setCommand06(address: Int, value: Int) {
        val buffer = ByteBuffer.allocate(8)

        buffer.put(0x81.toByte())
        buffer.put(0x06.toByte())
        buffer.put((address shr 8).toByte())
        buffer.put(address.toByte())


        buffer.put((value shr 8).toByte())
        buffer.put(value.toByte())
        CRC16Modbus().let {
            it.update(buffer)
            buffer.put(it.crcBytes)
        }

        batteryConnector.sendRequest(deviceAddress, buffer.array()).let {
            println(it.map { it.toByte() }.toByteArray().toHexString())
        }
    }

    fun currentSoc(): Double = currentSoc.load()


}
