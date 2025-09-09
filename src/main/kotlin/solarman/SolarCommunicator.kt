package de.klg71.solarman_sensor.solarman

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SolarCommunicator {
    private val solarManMutex = Mutex()

    suspend fun readSolarInfo(): SolarInfo? {
        var info: SolarInfo? = null
        withTimeout(Duration.ofSeconds(5).toMillis()) {
            solarManMutex.withLock {
                info = querySolarInfo()
            }
        }
        delay(1000)
        return info
    }

    suspend fun setPower(power: Int) {
        withTimeout(Duration.ofSeconds(5).toMillis()) {
            solarManMutex.withLock {
                de.klg71.solarman_sensor.solarman.setPower(power)
            }
        }
        delay(1000)
    }


}