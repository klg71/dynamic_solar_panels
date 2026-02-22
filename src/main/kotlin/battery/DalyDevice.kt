package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDate
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class Measurement(val unit: String, val deviceClass: String) {
    VOLT("V", "voltage"),
    CURRENT("A", "current"),
    POWER("W", "power"),
    PERCENTAGE("%", "battery"),
    TEMPERATURE("°C", "temperature"),
    STRING("", "enum"),
}

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalAtomicApi::class)
@Component
class DalyDevice(
    private val batteryConnector: BatteryConnector, private val dispatcher: CoroutineDispatcher,
    private val client: MqttClient,
    private val objectMapper: ObjectMapper,
) {
    private val logger = getLogger(DalyDevice::class.java)

    private val deviceAddress = "D2:19:08:01:16:96"
    private val name = "1"
    private val heatingPin = 14
    private var found = false
    private val numberOfTempSensors = 2
    private val shouldConnect = AtomicBoolean(true)

    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        scope.launch { watcher() }
        scope.launch { monitor() }
        homeAssistantDiscovery(Measurement.VOLT, "total-voltage", "totalVoltage")
        homeAssistantDiscovery(Measurement.CURRENT, "total-current", "totalCurrent")
        homeAssistantDiscovery(Measurement.PERCENTAGE, "soc", "soc")
        homeAssistantDiscovery(Measurement.TEMPERATURE, "temperature_0", "temperature_1")
        homeAssistantDiscovery(Measurement.TEMPERATURE, "temperature_1", "temperature_2")
        homeAssistantDiscovery(Measurement.VOLT, "min-cell-voltage", "minCellVoltage")
        homeAssistantDiscovery(Measurement.VOLT, "max-cell-voltage", "maxCellVoltage")
        homeAssistantDiscovery(Measurement.STRING, "state", "state")
        homeAssistantDiscovery(Measurement.STRING, "errors", "errors")
        homeAssistantDiscovery(Measurement.STRING, "last-update", "lastUpdate")
        homeAssistantDiscoverySwitch("charge-mos", "charge-mos/set", "chargeMos")
        client.subscribe("${mqttRoot()}/charge-mos/set", ::setChargeMos)
        homeAssistantDiscoverySwitch("discharge-mos", "discharge-mos/set", "dischargeMos")

        client.subscribe("${mqttRoot()}/discharge-mos/set", ::setDischargeMos)

        homeAssistantDiscoverySwitch("heating", "heating/set", "heating")
        client.subscribe("${mqttRoot()}/heating/set", ::setHeating)
        homeAssistantDiscoverySwitch("connected", "connected/set", "connected")
        client.subscribe("${mqttRoot()}/connected/set", ::setConnected)
    }

    private fun setDischargeMos(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            if (it == "ON") {
                scope.launch {
                    setCommand06(0x0122, 1)
                }
            }
            if (it == "OFF") {
                scope.launch {
                    setCommand06(0x0122, 0)
                }
            }
        }
    }

    private fun setChargeMos(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            if (it == "ON") {
                scope.launch {
                    setCommand06(0x0121, 1)
                }
            }
            if (it == "OFF") {
                scope.launch {
                    setCommand06(0x0121, 0)
                }
            }
        }
    }

    @Suppress("unused")
    private fun setHeating(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            batteryConnector.setPin(heatingPin, it == "ON")
            publishHeating()
        }
    }

    @Suppress("unused")
    private fun setConnected(topic: String, message: MqttMessage) {
        message.payload.toString(Charset.defaultCharset()).let {
            shouldConnect.store(it == "ON")
            batteryConnector.disconnect()
            publishConnected()
        }
    }

    private fun homeAssistantDiscoverySwitch(stateTopic: String, commandTopic: String, uniqueId: String) {
        mapOf(
            "name" to "Daly-${name}-$uniqueId",
            "state_topic" to "${mqttRoot()}/$stateTopic",
            "command_topic" to "${mqttRoot()}/$commandTopic",
            "unique_id" to "daly_bat_${uniqueId}",
            "device" to device(),
            "platform" to "switch"
        ).let {
            client.publish("homeassistant/switch/daly_${name}_$uniqueId/config", it)
        }
    }

    private fun homeAssistantDiscovery(measurement: Measurement, stateTopic: String, uniqueId: String) {
        val unit = if (measurement.unit.isNotBlank()) {
            mapOf(
                "unit_of_measurement" to measurement.unit,
            )
        } else {
            emptyMap()
        }
        val deviceClass = if (measurement.deviceClass.isNotBlank()) {
            mapOf("device_class" to measurement.deviceClass)
        } else {
            emptyMap()
        }
        (mapOf(
            "name" to "Daly-${name}-$uniqueId",
            "state_topic" to "${mqttRoot()}/$stateTopic",
            "state_class" to "measurement",
            "value_template" to "{{ value }}",
            "unique_id" to "daly_bat_${uniqueId}",
            "device" to device(),
            "platform" to "mqtt"
        ) + unit + deviceClass).let {
            client.publish("homeassistant/sensor/daly_${name}_$uniqueId/config", it)
        }
    }

    private fun device(): Map<String, Any> = mapOf(
        "identifiers" to listOf(
            "daly-battery-${name}"
        ),
        "name" to "DALY-Battery-${name}",
        "model" to "BMS",
        "manufacturer" to "DALY"
    )

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

    private suspend fun publishInfo() {
        publishSoc()
        publishErrors()
        publishTemp()
        publishMinMaxCellVoltage()
        publishMosState()
        publishHeating()
        publishConnected()

        client.publish("${mqttRoot()}/last-update", LocalDate.now().toString())
    }

    private fun publishHeating() {
        batteryConnector.getPin(heatingPin).let {
            client.publish(
                "${mqttRoot()}/heating", if (it) {
                    "ON"
                } else {
                    "OFF"
                }
            )
        }
    }

    private fun publishConnected() {

        client.publish(
            "${mqttRoot()}/connected", if (shouldConnect.load()) {
                "ON"
            } else {
                "OFF"
            }
        )
    }

    private fun mqttRoot() = "daly/${name}"

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
            client.publish("${mqttRoot()}/soc", soc)
            client.publish("${mqttRoot()}/total-current", current)
            client.publish("${mqttRoot()}/total-voltage", cumVoltage)
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
            val chargeMos = if (it[5] == 1) {
                "ON"
            } else {
                "OFF"
            }
            val dischargeMos = if (it[6] == 1) {
                "ON"
            } else {
                "OFF"
            }
            client.publish("${mqttRoot()}/state", state)
            client.publish("${mqttRoot()}/charge-mos", chargeMos, true)
            client.publish("${mqttRoot()}/discharge-mos", dischargeMos, true)
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
            client.publish("${mqttRoot()}/max-cell-voltage", maxCell)
            client.publish("${mqttRoot()}/max-cell-number", maxCellNr)
            client.publish("${mqttRoot()}/min-cell-voltage", minCell)
            client.publish("${mqttRoot()}/min-cell-number", minCellNr)
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
                    client.publish("${mqttRoot()}/temperature_${k * 13 + tempIndex}", temperature)
                    if ((k * 13 + tempIndex + 1) >= numberOfTempSensors) {
                        return
                    }
                }
            }
        }
    }

    fun bitRead(inByte: Int, bitIndex: Int): Boolean =
        inByte == ((1 shl bitIndex) and (1 shl bitIndex))

    private suspend fun publishErrors() {
        sendRequest(0x98).let {
            if (it.isEmpty()) {
                return
            }
            if (it[2] != 0x98) {
                return
            }
            buildList {
                if (bitRead(it[4], 1)) {
                    add("Cell Voltage high l2,")
                }
                if (bitRead(it[4], 0)) {
                    add("Cell Voltage high l1,")
                }
                if (bitRead(it[4], 3)) {
                    add("Cell Voltage low l2,")
                }
                if (bitRead(it[4], 2)) {
                    add("Cell voltage low l1,")
                }
                if (bitRead(it[4], 5)) {
                    add("Sum Voltage high l2,")
                }
                if (bitRead(it[4], 4)) {
                    add("Sum Voltage high l1,")
                }
                if (bitRead(it[4], 7)) {
                    add("Sum Voltage low l2,")
                }
                if (bitRead(it[4], 6)) {
                    add("Sum Voltage low l1,")
                }
                if (bitRead(it[5], 1)) {
                    add("Charge Temperature high l2,")
                }
                if (bitRead(it[5], 0)) {
                    add("Charge Temperature high l1,")
                }
                if (bitRead(it[5], 3)) {
                    add("Charge Temperature low l2,")
                }
                if (bitRead(it[5], 2)) {
                    add("Charge Temperature low l1,")
                }
                if (bitRead(it[5], 5)) {
                    add("Discharge Temperature high l2,")
                }
                if (bitRead(it[5], 4)) {
                    add("Discharge Temperature high l1,")
                }
                if (bitRead(it[5], 7)) {
                    add("Discharge Temperature low l2,")
                }
                if (bitRead(it[5], 6)) {
                    add("Discharge Temperature low l1,")
                }
                if (bitRead(it[6], 1)) {
                    add("Charge OverCurrent l1,")
                }
                if (bitRead(it[6], 0)) {
                    add("Charge OverCurrent l1,")
                }
                if (bitRead(it[6], 3)) {
                    add("Discharge OverCurrent l2,")
                }
                if (bitRead(it[6], 2)) {
                    add("Discharge OverCurrent l1,")
                }
                if (bitRead(it[6], 5)) {
                    add("SOC high l2,")
                }
                if (bitRead(it[6], 4)) {
                    add("SOC high l1,")
                }
                if (bitRead(it[6], 7)) {
                    add("SOC low l2,")
                }
                if (bitRead(it[6], 6)) {
                    add("SOC low l1,")
                }
                if (bitRead(it[7], 1)) {
                    add("Cell Diff oltage l2,")
                }
                if (bitRead(it[7], 0)) {
                    add("Cell Diff Voltage l1,")
                }
                if (bitRead(it[7], 3)) {
                    add("Diff Temperature l2,")
                }
                if (bitRead(it[7], 2)) {
                    add("Diff Temperature l1,")
                }
                if (bitRead(it[8], 0)) {
                    add("Charge MOS Temperature high,")
                }
                if (bitRead(it[8], 1)) {
                    add("Discharge MOS Temperature high,")
                }
                if (bitRead(it[8], 2)) {
                    add("Charge MOS Temperature sensor err,")
                }
                if (bitRead(it[8], 3)) {
                    add("Discharge MOS Temperature sensor err,")
                }
                if (bitRead(it[8], 4)) {
                    add("Charge MOS adh err,")
                }
                if (bitRead(it[8], 5)) {
                    add("Discharge MOS adh err,")
                }
                if (bitRead(it[8], 6)) {
                    add("Charge MOS open circuit err,")
                }
                if (bitRead(it[8], 7)) {
                    add("Discharge MOS open circuit err,")
                }
                if (bitRead(it[9], 0)) {
                    add("AFE collect chip err,")
                }
                if (bitRead(it[9], 1)) {
                    add("Volt collect dropped,")
                }
                if (bitRead(it[9], 2)) {
                    add("Cell tempt sensor err,")
                }
                if (bitRead(it[9], 3)) {
                    add("EEPROM err,")
                }
                if (bitRead(it[9], 4)) {
                    add("RTC err,")
                }
                if (bitRead(it[9], 5)) {
                    add("Precharge fail,")
                }
                if (bitRead(it[9], 6)) {
                    add("Comm fail,")
                }
                if (bitRead(it[9], 7)) {
                    add("Int comm fail,")
                }
                if (bitRead(it[10], 0)) {
                    add("Current module fault,")
                }
                if (bitRead(it[10], 1)) {
                    add("S Volt detect fault,")
                }
                if (bitRead(it[10], 2)) {
                    add("S Current protect fault,")
                }
                if (bitRead(it[10], 3)) {
                    add("L Voltage forb chg fault,")
                }
            }.joinToString("").let {
                client.publish("${mqttRoot()}/errors", it)
            }
        }

    }

    private fun <T> MqttClient.publish(topic: String, payload: T, retain: Boolean = false) {
        if (payload is String) {
            publish(topic, MqttMessage(payload.toByteArray()).also { it.isRetained = retain })
        } else {
            publish(topic, MqttMessage(objectMapper.writeValueAsBytes(payload)).also { it.isRetained = retain })
        }

    }

    suspend fun watcher() {
        while (true) {
            try {
                found = batteryConnector.devices().any { it.address == deviceAddress }
            } catch (e: Throwable) {
                logger.error("Error while watching battery", e)
            }
            delay(Duration.ofMinutes(2).toMillis())
        }
    }


    fun MODBUS_CRC16(buffer: UByteArray, len: Int): UInt {
        val table: List<UShort> = listOf(
            0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
            0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
            0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
            0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
            0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
            0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
            0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
            0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
            0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
            0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
            0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
            0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
            0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
            0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
            0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
            0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
            0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
            0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
            0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
            0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
            0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
            0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
            0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
            0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
            0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
            0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
            0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
            0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
            0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
            0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
            0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
            0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
        ).map { it.toUShort() }

        var xorV: UShort = 0x00.toUShort();
        var crc: UInt = 0xFFFF.toUInt();

        repeat(len) {

            xorV = (buffer[it].toUInt().xor(crc)).toUShort()
            crc = crc shr 8;
            crc = crc xor table[xorV.toInt()].toUInt();
        }

        return crc;
    }

    private suspend fun setCommand06(address: Int, value: Int) {
        val buffer = UByteArray(8)
        buffer[0] = 0x81.toUByte()
        buffer[1] = 0x06.toUByte()
        buffer[2] = (address shl 8).toUByte()
        buffer[3] = address.toUByte();

        buffer[4] = (value shl 8).toUByte();
        buffer[5] = value.toUByte();
        val crc = MODBUS_CRC16(buffer, 6);

        buffer[6] = (crc and 0xFF.toUInt()).toUByte();
        buffer[7] = ((crc shr 8) and 0xFF.toUInt()).toUByte();


        batteryConnector.sendRequest(deviceAddress, buffer.toByteArray())
    }


}
