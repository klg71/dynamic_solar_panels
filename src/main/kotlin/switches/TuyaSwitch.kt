package de.klg71.solarman_sensor.switches

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder

internal class TuyaSwitch(val id: String, val ip: String, val localKey: String, objectMapper: ObjectMapper) :
    SmartSwitch {
    private data class TuyaCommand(val gwId: String, val devId: String, val uid: String, val t: String)

    private val client: ShellySwitchClient =
        Feign.builder().decoder(JacksonDecoder(objectMapper)).encoder(JacksonEncoder(objectMapper))
            .target(ShellySwitchClient::class.java, "http://${ip}")

    override fun id() = id
    override fun status() = client.switchStatus().output
    override fun setStatus(boolean: Boolean) {
        client.set(boolean)
    }
}
