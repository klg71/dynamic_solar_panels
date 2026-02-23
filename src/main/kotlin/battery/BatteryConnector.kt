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
    private var client = SSHClient()
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

    fun setPin(nr: Int, up: Boolean) {
        val session = client.startSession()
        val cmd = if (up) {
            session.exec("pinctrl set $nr op dh")
        } else {
            session.exec("pinctrl set $nr op dl")
        }
        cmd.join(10, TimeUnit.SECONDS)
        session.close()
    }

    fun getPin(nr: Int): Boolean {
        val session = client.startSession()
        val cmd = session.exec("pinctrl get $nr")
        cmd.join(10, TimeUnit.SECONDS)
        val output =
            cmd.inputStream.readAllBytes().toString(Charset.defaultCharset()).contains("hi")
        session.close()
        return output
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    suspend fun sendRequest(deviceAddress: String, buffer: ByteArray, clean: Boolean = false): List<Int> {
        ensureOpenSession(deviceAddress)
        val start = System.currentTimeMillis()
        var answer = ""
        if (clean) {
            while (exec.inputStream.available() > 0) {
                scanner.nextLine()
            }
        }

        while ((System.currentTimeMillis() - start) < Duration.ofSeconds(5).toMillis() && answer.isEmpty()) {
            outputStream.write(buffer.toHexString() + "\n")
            outputStream.flush()
            delay(500)
            while (exec.inputStream.available() > 0) {
                var wholeLine = scanner.nextLine()
                while (wholeLine.contains("a5")) {
                    val next = wholeLine.indexOf("a5", 2)
                    val line = if (next != -1) {
                        wholeLine.take(next)
                    } else {
                        wholeLine
                    }
                    wholeLine = if (next != -1) {
                        wholeLine.substring(next)
                    } else {
                        ""
                    }
                    if (line.startsWith("a5") && !line.contains(buffer.toHexString())) {
                        answer = line
                        break
                    }
                }
            }
            delay(1000)
        }
        return answer.hexToUByteArray().map { it.toInt() }
    }

    private suspend fun ensureOpenSession(deviceAddress: String) {
        if (!client.isConnected) {
            client = SSHClient()
            client.addHostKeyVerifier(PromiscuousVerifier())
            client.connect("garage-pi")
            client.authPassword("lukas", "1805Rh")
        }
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

    fun disconnect() {
        session?.close()
        client.disconnect()
    }
}