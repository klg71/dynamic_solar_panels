package de.klg71.solarman_sensor.solarman

import de.klg71.solarman_sensor.getLogger
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Duration

@Component
class SolarCommunicator {
    private val logger = getLogger(SolarCommunicator::class.java)

    suspend fun readSolarInfo(): SolarInfo? {
        var info: SolarInfo? = null
        try {
            val client = newSocket()
            withTimeoutOrNull(Duration.ofSeconds(5).toMillis()) {
                info = querySolarInfo(client)
            }.let {
                if (it == null) {
                    logger.info("Solar Inverter not reachable")
                }
            }
            client.close()
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                logger.debug("Device offline")
            } else {
                logger.warn("Unable to query solar info", e)
            }
        }
        return info
    }

    suspend fun setPower(power: Int) {
        try {
            val client = newSocket()
            withTimeoutOrNull(Duration.ofSeconds(5).toMillis()) {
                setPower(power, client)
            }.let {
                if (it == null) {
                    logger.info("Solar Inverter not reachable")
                }
            }
            client.close()
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                logger.debug("Device offline")
            } else {
                logger.warn("Unable to set power", e)
            }
        }
    }

    private fun newSocket(): Socket = Socket().also {
        it.connect(InetSocketAddress("192.168.178.67", 8899), Duration.ofSeconds(2).toMillis().toInt())
    }


}
