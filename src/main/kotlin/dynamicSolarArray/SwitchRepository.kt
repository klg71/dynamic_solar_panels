package de.klg71.solarman_sensor.dynamicSolarArray

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.stereotype.Component

@Component
internal class SwitchRepository(private val objectMapper: ObjectMapper) {
    private val switches = listOf(
        Switch("solar-2", "192.168.178.71"),
        Switch("solar-3", "192.168.178.73"),
        Switch("solar-4", "192.168.178.75"),
    )
    private val clients = switches.associate {
        it.name to
                Feign.builder().decoder(JacksonDecoder(objectMapper)).encoder(JacksonEncoder(objectMapper))
                    .target(SwitchClient::class.java, "http://${it.ip}")
    }

    fun byName(name: String) = switches.firstOrNull { it.name == name }

    fun status(name: String) = clients[name]?.switchStatus()?.output ?: false

    fun setStatus(name: String, output: Boolean) = clients[name]?.set(output)
}
