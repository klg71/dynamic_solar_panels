package de.klg71.solarman_sensor.api

import de.klg71.solarman_sensor.solarman.queryHoldingRegisters
import de.klg71.solarman_sensor.solarman.querySolarInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class SolarmanController {
    @GetMapping
    fun data() = querySolarInfo()

    @GetMapping("/holding")
    fun holdingRegister() = queryHoldingRegisters()
}
