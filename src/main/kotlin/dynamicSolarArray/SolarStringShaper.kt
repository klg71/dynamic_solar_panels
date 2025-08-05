package de.klg71.solarman_sensor.dynamicSolarArray

import de.klg71.solarman_sensor.getLogger
import de.klg71.solarman_sensor.solarman.SolarInfo
import de.klg71.solarman_sensor.solarman.querySolarInfo
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.time.Duration

@Service
internal class SolarStringShaper(private val repository: SolarStringRepository,
    private val switchRepository: SwitchRepository,
    private val dispatcher: CoroutineDispatcher) {
    companion object {
        private const val AMPERE_ENABLE_THRESHOLD = 6L
        private const val AMPERE_DISABLE_THRESHOLD = 14L
    }

    private val scope = CoroutineScope(dispatcher)
    private val logger= getLogger(SolarStringShaper::class.java)

    @PostConstruct
    fun startShaping() {
        logger.info("Starting solar shaping")
        scope.launch { shape() }
    }

    private suspend fun shape() {
        while (true) {
            querySolarInfo()?.let { info ->
                repository.solarStrings().forEach {
                    shapeString(it, info)
                }
            }
            delay(Duration.ofSeconds(10).toMillis())
        }
    }

    private fun shapeString(string: SolarString, solarInfo: SolarInfo) {
        if (solarInfo.strings[string.id].current < AMPERE_ENABLE_THRESHOLD) {
            logger.info("${string.id} string under threshold, enabling switch")
            enableOneSwitch(string)
        }
        if (solarInfo.strings[string.id].current > AMPERE_DISABLE_THRESHOLD) {
            logger.info("${string.id} string over threshold, disabling switch")
            disableOneSwitch(string)
        }

    }

    private fun enableOneSwitch(string: SolarString) {
        string.switches.forEach {
            if (!switchRepository.status(it)) {
                logger.info("Enabling switch: $it")
                switchRepository.setStatus(it, true)
                return
            }
        }
        logger.info("Can't disable enable more switches for: ${string.id}")
    }

    private fun disableOneSwitch(string: SolarString) {
        string.switches.forEach {
            if (switchRepository.status(it)) {
                logger.info("Disabling switch: $it")
                switchRepository.setStatus(it, false)
                return
            }
        }
        logger.info("Can't disable more switches for: ${string.id}")
    }
}
