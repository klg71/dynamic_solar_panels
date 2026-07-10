package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.power.HomeAssistantClient
import de.klg71.solarman_sensor.power.PowerRegulator
import de.klg71.solarman_sensor.power.SwitchTurnPayload
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
internal class BatteryDischargeDisconnector(
    dispatcher: CoroutineDispatcher,
    private val objectMapper: ObjectMapper,
) {

    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(BatteryDischargeDisconnector::class.java)
    private val lastLiIon = AtomicBoolean(false)
    private val lastVRLA = AtomicBoolean(false)

    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"

    @PostConstruct
    fun init() {
        val client = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            requestInterceptor {
                it.header("Authorization", "Bearer $token")
            }
            target(HomeAssistantClient::class.java, "http://homeassistant:8123")
        }
        scope.launch {
            while (isActive) {
                try {
                    watchBatteryVoltage(client)
                } catch (e: Exception) {
                    logger.error("Error while controlling battery voltage", e)
                }
            }
        }
    }

    private fun watchBatteryVoltage(client: HomeAssistantClient) {
        client.enableLiIon(client.getLiIonVoltage().state.toFloat() > 50)
        client.enableVRLA(client.getVRLAVoltage().state.toFloat() > 46)
    }

    private fun HomeAssistantClient.enableLiIon(on: Boolean) {
        if (on == lastLiIon.load()) {
            return
        }
        if (on) {
            turnOn(SwitchTurnPayload("switch.battery_discharge"))
        } else {
            turnOff(SwitchTurnPayload("switch.battery_discharge"))
        }
        lastLiIon.store(on)
    }

    private fun HomeAssistantClient.enableVRLA(on: Boolean) {
        if (on == lastVRLA.load()) {
            return
        }
        if (on) {
            turnOn(SwitchTurnPayload("switch.battery_discharge_vrla"))
        } else {
            turnOff(SwitchTurnPayload("switch.battery_discharge_vrla"))
        }
        lastVRLA.store(on)
    }
}