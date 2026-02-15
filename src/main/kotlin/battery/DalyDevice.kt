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
import java.time.Duration
import kotlin.experimental.and

@Component
class DalyDevice(
    private val batteryConnector: BatteryConnector, private val dispatcher: CoroutineDispatcher,
    private val client: MqttClient,
    private val objectMapper: ObjectMapper,
) {
    private val logger = getLogger(DalyDevice::class.java)

    private val deviceAddress = "D2:19:08:01:16:96"
    private val name = "1"
    private var found = false;

    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        scope.launch { watcher() }
        scope.launch { monitor() }
    }

    private suspend fun monitor() {
        while (true) {
            try {
                if (found) {
                    publishInfo()
                }
            } catch (e: Throwable) {
                logger.error("Error while watching battery", e)
            }
            delay(Duration.ofSeconds(1).toMillis())
        }
    }

    private fun sendRequest(address: Int): ByteArray {
        val buffer = ByteArray(13)
        buffer[0] = 0xA5.toByte();
        buffer[1] = 0x40;
        buffer[2] = address.toByte();
        buffer[3] = 0x08;
        for (i in 4..12) {
            buffer[i] = 0x00
        }

        var checksum: Byte = 0;
        for (i in 0..11) {
            checksum = ((checksum + buffer[i]) % 256).toByte()
        }

        buffer[12] = checksum;
        return batteryConnector.sendRequest(deviceAddress, buffer)
    }

    private fun publishInfo() {
        publishSoc()
    }

    private fun publishSoc() {
        sendRequest(0x90).let {
            if (it[2] != 0x90.toByte()) {
                return
            }
            val cumVoltage = (it[4] * 256 + it[5]) * 0.1
            val gatherVoltage = (it[6] * 256 + it[7]) * 0.1
            val current = (it[8] * 256 + it[9]) * 0.1 - 3000
            val soc = (it[10] * 256 + it[11]) * 0.1
            client.publish("tinkerboard/daly/${name}/soc", soc)
            client.publish("tinkerboard/daly/${name}/battery-current", current)
            client.publish("tinkerboard/daly/${name}/battery-voltage", cumVoltage)
        }
    }
    fun bitRead(inByte: Byte, bitIndex:Int)=inByte and(  (1 shl bitIndex).toByte() and (1 shl bitIndex).toByte())
    private fun publishErrors(){
        sendRequest(0x98).let {
            buildList { errors ->

                if bitRead(x[4], 1):
                errors.append("Cell Voltage high l2,");
                if bitRead(x[4], 0):
                errors.append("Cell Voltage high l1,");
                if bitRead(x[4], 3):
                errors.append("Cell Voltage low l2,");
                if bitRead(x[4], 2):
                errors.append("Cell voltage low l1,");
                if bitRead(x[4], 5):
                errors.append("Sum Voltage high l2,");
                if bitRead(x[4], 4):
                errors.append("Sum Voltage high l1,");
                if bitRead(x[4], 7):
                errors.append("Sum Voltage low l2,");
                if bitRead(x[4], 6):
                errors.append("Sum Voltage low l1,");
                # 0x01
                if bitRead(x[5], 1):
                errors.append("Charge Temperature high l2,");
                if bitRead(x[5], 0):
                errors.append("Charge Temperature high l1,");
                if bitRead(x[5], 3):
                errors.append("Charge Temperature low l2,");
                if bitRead(x[5], 2):
                errors.append("Charget Temperature low l1,");
                if bitRead(x[5], 5):
                errors.append("Discharge Temperature high l2,");
                if bitRead(x[5], 4):
                errors.append("Discharge Temperature high l1,");
                if bitRead(x[5], 7):
                errors.append("Discharge Temperature low l2,");
                if bitRead(x[5], 6):
                errors.append("Discharge Temperature low l1,");
                # 0x02
                if bitRead(x[6], 1):
                errors.append("Charge OverCurrent l1,");
                if bitRead(x[6], 0):
                errors.append("Charge OverCurrent l1,");
                if bitRead(x[6], 3):
                errors.append("Discharge OverCurrent l2,");
                if bitRead(x[6], 2):
                errors.append("Discharge OverCurrent l1,");
                if bitRead(x[6], 5):
                strcat(g_errorString, "SOC high l2,");
                if bitRead(x[6], 4):
                errors.append("SOC high l1,");
                if bitRead(x[6], 7):
                errors.append("SOC low l2,");
                if bitRead(x[6], 6):
                errors.append("SOC low l1,");
                # 0x03
                if bitRead(x[7], 1):
                errors.append("Cell Diff oltage l2,")
                if bitRead(x[7], 0):
                errors.append("Cell Diff Voltage l1,");
                if bitRead(x[7], 3):
                errors.append("Diff Temperature l2,");
                if bitRead(x[7], 2):
                errors.append("Diff Temperature l1,");
                # 0x04
                if bitRead(x[8], 0):
                errors.append("Charge MOS Temperature high,");
                if bitRead(x[8], 1):
                errors.append("Discharge MOS Temperature high,");
                if bitRead(x[8], 2):
                errors.append("Charge MOS Temperature sensor err,");
                if bitRead(x[8], 3):
                errors.append("Discharge MOS Temperature sensor err,");
                if bitRead(x[8], 4):
                errors.append("Charge MOS adh err,");
                if bitRead(x[8], 5):
                errors.append("Discharge MOS adh err,");
                if bitRead(x[8], 6):
                errors.append("Charge MOS open circuit err,");
                if bitRead(x[8], 7):
                errors.append("Discharge MOS open circuit err,");
                # 0x05
                if bitRead(x[9], 0):
                errors.append("AFE collect chip err,");
                if bitRead(x[9], 1):
                errors.append("Volt collect dropped,");
                if bitRead(x[9], 2):
                errors.append("Cell tempt sensor err,");
                if bitRead(x[9], 3):
                errors.append("EEPROM err,");
                if bitRead(x[9], 4):
                errors.append("RTC err,");
                if bitRead(x[9], 5):
                errors.append("Precharge fail,");
                if bitRead(x[9], 6):
                errors.append("Comm fail,");
                if bitRead(x[9], 7):
                errors.append("Int comm fail,");
                # 0x06
                if bitRead(x[10], 0):
                errors.append("Current module fault,");
                if bitRead(x[10], 1):
                errors.append("S Volt detect fault,");
                if bitRead(x[10], 2):
                errors.append("S Current protect fault,");
                if bitRead(x[10], 3):
                errors.append("L Voltage forb chg fault,");

                client.publish("tinkerboard/daly/errors", "".join(errors))
                print("".join(errors))
            }
        }

    }

    private fun <T> MqttClient.publish(topic: String, payload: T) {

        publish(topic, MqttMessage(objectMapper.writeValueAsBytes(payload)))
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


}