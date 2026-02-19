package de.klg71.solarman_sensor.battery

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.springframework.stereotype.Component
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


@Component
class BatteryConnector(private val dispatcher: CoroutineDispatcher) {
    private val client = SSHClient()
    private var session: Session? = null
    private lateinit var scanner: Scanner
    private lateinit var outputStream: OutputStreamWriter
    private lateinit var exec: Session.Shell


    @PostConstruct
    fun init() {
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connect("garage-pi")
        client.authPassword("lukas", "1805Rh")

    }

    @PreDestroy
    fun tearDown() {
        session?.close()
        client.disconnect()
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
        ensureOpenSession(deviceAddress)
        val start = System.currentTimeMillis()
        var answer=""

        while((System.currentTimeMillis()-start) < Duration.ofSeconds(5).toMillis()&&answer.isEmpty()) {
            outputStream.write(buffer.toHexString() + "\n")
            outputStream.flush()
            delay(500)
            while(exec.inputStream.available() > 0) {
                val line= scanner.nextLine()
                if(line.startsWith("a5")&&!line.contains(buffer.toHexString())){
                    answer=line
                    break
                }
            }
            delay(500)
        }
        println(answer)
        return answer.hexToUByteArray().map { it.toInt() }
    }

    private suspend fun ensureOpenSession(deviceAddress: String) {
        if (!(session?.isOpen ?: false)) {
            session = client.startSession()?.also {
                it.allocateDefaultPTY()
                exec = it.startShell()
                outputStream = OutputStreamWriter(exec.outputStream)
                outputStream.write(
                    "/usr/bin/python /home/lukas/repositories/battery-manager/bluetooth-client-continuous.py $deviceAddress\n"
                )
                outputStream.flush()
                scanner = Scanner(exec.inputStream)
                delay(500)
                while (exec.inputStream.available() != 0) {
                    scanner.nextLine()
                }
            }
        }
    }
}