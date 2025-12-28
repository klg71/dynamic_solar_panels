package de.klg71.solarman_sensor.charge

import de.klg71.solarman_sensor.solarman.MTURequestTarget
import de.klg71.solarman_sensor.solarman.RequestFrame
import de.klg71.solarman_sensor.solarman.ResponseFrame
import de.klg71.solarman_sensor.solarman.SolarInfo
import kotlinx.coroutines.delay
import java.net.Socket


suspend fun queryChargingInfo(client: Socket){

    val request = RequestFrame.Companion.readRegister(1, 3, 0.toUShort(),18)
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    delay(1000)
    client.getInputStream().read(buffer, 0, client.getInputStream().available())
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun setPower(power: Int, client: Socket) {

    val request = RequestFrame.Companion.setPower(power)
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    delay(1000)
    client.getInputStream().read(buffer, 0, client.getInputStream().available())
    client.close()
}


fun String.hexToBytes() = chunked(2)
    .map { it.toInt(16).toByte() }.toByteArray()

