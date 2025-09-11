package de.klg71.solarman_sensor.api

import de.klg71.solarman_sensor.power.PowerRegulator
import de.klg71.solarman_sensor.solarman.SolarCommunicator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class SolarmanController(private val solarCommunicator: SolarCommunicator,
    private val powerRegulator: PowerRegulator) {
    @GetMapping
    suspend fun data() = solarCommunicator.readSolarInfo()

    @GetMapping("/currentSetPower")
    suspend fun setPower() = powerRegulator.getCurrentSetPower()
}
