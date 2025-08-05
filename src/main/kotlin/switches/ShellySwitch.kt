package de.klg71.solarman_sensor.switches

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder

internal class ShellySwitch(val id: String, val ip: String, objectMapper: ObjectMapper) : SmartSwitch {
    private val client: ShellySwitchClient = Feign.builder().decoder(JacksonDecoder(objectMapper)).encoder(JacksonEncoder(objectMapper))
        .target(ShellySwitchClient::class.java, "http://${ip}")

    override fun id() = id
    override fun status() = client.switchStatus().output
    override fun setStatus(boolean: Boolean) {
        client.set(boolean)
    }
}
