package de.klg71.solarman_sensor.charge

import de.klg71.solarman_sensor.getLogger
import feign.Feign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalUnsignedTypes::class)
class Charger(
    private val name: String, private val config: ChargingConfig, private val scope: CoroutineScope,
    private val mqttStore: MQTTStore
) {
    private val logger = getLogger(ChargingController::class.java)


    companion object {
        const val VOLTAGE_STEP = 0.1f
    }

    private var buckConverterConnected = false
    private var ipBuckConverter: String = ""


    public fun start() {
        mqttStore.subscribe(config.mqqtBuckConverterTopic + "/#")
        mqttStore.subscribe(config.mqqtBMSTopic + "/#")
        scope.launch {
            run()
        }
    }

    suspend fun run() {
        while (!chargerOnline() || chargerIp() == null) {
            delay(1000)
        }
        val client = Feign.Builder().run {
            target(OpenBekenClient::class.java, "http://${chargerIp()}")
        }
        client.setChargerOnState(false)
        client.command("ZK10022_Set_PowerOnSwitch 0")
        client.command("ZK10022_Set_OverVoltageProtection ${config.maxVoltage}")
        client.command("ZK10022_Set_LowVoltageProtection ${config.minVoltage}")
        while (!bmsOnline() || bmsIp() == null) {
            delay(1000)
        }
        while (true) {
            while (bmsError() != null) {
                logger.error("BMS: ${config.mqqtBMSTopic} has an error waiting to clear")
                client.setChargerOnState(false)
                delay(1000)
            }
            waitForValidBatteryVoltage()
            while ((waitForValidBatteryVoltage()) <= config.maxVoltage) {
                val setVoltage = (waitForValidBatteryVoltage() + VOLTAGE_STEP)
                    .coerceAtMost(config.maxVoltage)
                client.command("ZK10022_Set_Current ${config.chargingCurrent}")
                client.command("ZK10022_Set_Voltage $setVoltage")
                client.setChargerOnState(true)
            }
        }
    }

    private fun OpenBekenClient.setChargerOnState(state: Boolean) {
        if (state) {
            command("ZK10022_Set_Switch 1")
        } else {
            command("ZK10022_Set_Switch 0")
        }
    }

    private fun chargerOnline(): Boolean = mqttStore.getString(config.mqqtBuckConverterTopic + "/connected") == "online"
    private fun chargerIp(): String? = mqttStore.getString(config.mqqtBuckConverterTopic + "/ip")

    private fun bmsOnline(): Boolean = mqttStore.getString(config.mqqtBMSTopic + "/connected") == "online"
    private fun bmsIp(): String? = mqttStore.getString(config.mqqtBMSTopic + "/ip")
    private fun bmsError(): String? = mqttStore.getString(config.mqqtBMSTopic + "/daly_bms_error")
    private fun bmsVoltage(): Float? = mqttStore.getFloat(config.mqqtBMSTopic + "/daly_bms_cum_voltage")
    private suspend fun waitForValidBatteryVoltage(): Float {
        while (true) {
            val voltage = bmsVoltage()
            if (voltage != null && voltage > 0.0f) {
                return voltage
            }
            delay(1000)
            logger.error("Battery voltage null, waiting")
        }
    }
}