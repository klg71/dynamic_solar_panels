package de.klg71.solarman_sensor.dynamicSolarArray

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.switches.ShellySwitch
import de.klg71.solarman_sensor.switches.SmartSwitch
import org.springframework.stereotype.Component

@Component
internal class SwitchRepository(private val objectMapper: ObjectMapper) {
    private val switches: List<SmartSwitch> = listOf(
        ShellySwitch("solar-2", "192.168.178.71", objectMapper),
        ShellySwitch("solar-3", "192.168.178.73", objectMapper),
        ShellySwitch("solar-4", "192.168.178.75", objectMapper),
    )

    fun byName(id: String) = switches.firstOrNull { it.id() == id }

    fun status(id: String) = byName(id)?.status() ?: false

    fun setStatus(id: String, output: Boolean) = byName(id)?.setStatus(output)
}
