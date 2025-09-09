package de.klg71.solarman_sensor.api

import de.klg71.solarman_sensor.solarman.SolarCommunicator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class SolarmanController(private val solarCommunicator: SolarCommunicator) {
    @GetMapping
    suspend fun data() = solarCommunicator.readSolarInfo()
}
