package de.klg71.solarman_sensor.dynamicSolarArray

import de.klg71.solarman_sensor.getLogger
import org.springframework.stereotype.Component

@Component
internal class SolarStringRepository(private val switchRepository: SwitchRepository) {
    companion object {
        private val strings = mapOf(2 to listOf("solar-2", "solar-3", "solar-4"))
    }

    private val logger = getLogger(SolarStringRepository::class.java)

    private val solarStrings = strings.map { string ->
        string.value.forEach {
            if (switchRepository.byName(it) == null) {
                logger.error("Could not find switch $it")
                error("Could not find switch $it")
            }
        }
        SolarString(string.key, string.value)
    }

    fun solarStrings() = solarStrings
}
