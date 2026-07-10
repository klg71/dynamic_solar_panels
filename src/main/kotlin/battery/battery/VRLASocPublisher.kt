package de.klg71.solarman_sensor.battery.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.battery.Measurement
import de.klg71.solarman_sensor.battery.MqttPublisher
import de.klg71.solarman_sensor.battery.ReconnectableMqttClient
import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.power.HomeAssistantClient
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
internal class VRLASocPublisher(
    dispatcher: CoroutineDispatcher,
    private val objectMapper: ObjectMapper,
    mqttClient: ReconnectableMqttClient,
) {

    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(VRLASocPublisher::class.java)
    private val mqttPublisher = MqttPublisher(mqttClient, "vrla", "vrla", objectMapper)

    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"

    @PostConstruct
    fun init() {
        mqttPublisher.homeAssistantDiscovery(Measurement.ENERGY, "remaining-capacity", "remaining-capacity")
        mqttPublisher.homeAssistantDiscovery(Measurement.PERCENTAGE, "soc", "soc")
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
                    watchSoc(client)
                } catch (e: Exception) {
                    logger.error("Error while watching vrla soc", e)
                }
            }
        }
    }

    private fun watchSoc(client: HomeAssistantClient) {
        client.getVRLAVoltage().state.toFloat().let { voltage ->
            ((voltage / 8 / 3) - 1.91) / 0.34 * 100
        }.let {
            mqttPublisher.publish("/remaining-capacity", it*344/1000)
            mqttPublisher.publish("/soc", it)
        }
    }
}