package de.klg71.solarman_sensor.battery

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalUnsignedTypes::class)
@Component
class BatteryConnector(private val dispatcher: CoroutineDispatcher) {
    private val client = SSHClient()


    @PostConstruct
    fun init() {
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connect("garage-pi")
        client.authPassword("lukas", "1805Rh")
    }

    fun devices(): List<BtDevice> {
        val session = client.startSession()
        val cmd = session.exec("/usr/bin/python /home/lukas/repositories/battery-manager/device-scanner.py")
        cmd.join(10, TimeUnit.SECONDS)
        val devices =
            cmd.inputStream.readAllBytes().toString(Charset.defaultCharset()).lines().filterNot { it.isBlank() }.map {
                it.split("\t").let { split ->
                    BtDevice(split[0], split[1])
                }
            }
        session.close()
        return devices
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    suspend fun sendRequest(deviceAddress: String, buffer: ByteArray): List<Int> {
        val session = client.startSession()
        val exec = session.exec(
            "/usr/bin/python /home/lukas/repositories/battery-manager/bluetooth-client.py " +
                    deviceAddress + " " +
                    buffer.toHexString()
        )
        var answer = ""
        val start = System.currentTimeMillis()
        while (answer.isEmpty()) {
            answer = exec.inputStream.readAllBytes().toString(Charset.defaultCharset()).trim()
            delay(500)
            if (System.currentTimeMillis() - start > Duration.ofSeconds(5).toMillis()) {
                break
            }
        }
        session.close()
        return answer.hexToUByteArray().map { it.toInt() }
    }

}