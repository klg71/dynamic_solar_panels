package de.klg71.solarman_sensor.solarman

import kotlinx.coroutines.delay
import java.net.Socket


suspend fun querySolarInfo(client: Socket): SolarInfo? {

    val request = RequestFrame.readRegister(MTURequestTarget.solarInverter, 4, 0.toUShort())
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    delay(1000)
    client.getInputStream().read(buffer, 0, client.getInputStream().available())
    return ResponseFrame.parseSolarInfo(buffer)
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun setPower(power: Int, client: Socket) {

    val request = RequestFrame.setPower(power)
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    delay(1000)
    client.getInputStream().read(buffer, 0, client.getInputStream().available())
    client.close()
}


fun String.hexToBytes() = chunked(2)
    .map { it.toInt(16).toByte() }.toByteArray()

