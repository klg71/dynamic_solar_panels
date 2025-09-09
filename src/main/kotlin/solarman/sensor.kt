package de.klg71.solarman_sensor.solarman

import java.net.Socket


fun querySolarInfo(): SolarInfo? {
    val client = Socket("192.168.178.67", 8899)

    val request = RequestFrame.readRegister(MTURequestTarget.solarInverter, 4, 0.toUShort())
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    client.getInputStream().read(buffer)
    return ResponseFrame.parseSolarInfo(buffer).also {
        client.close()
    }
}
@OptIn(ExperimentalStdlibApi::class)
suspend fun setPower(power:Int) {
    val client = Socket("192.168.178.67", 8899)

    val request = RequestFrame.setPower(power)
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    client.getInputStream().read(buffer)
    println(buffer.toHexString(HexFormat.UpperCase))
    client.close()
}

@OptIn(ExperimentalStdlibApi::class)
fun queryHoldingRegisters() {
    val client = Socket("192.168.178.67", 8899)

    val request = RequestFrame.readRegister(MTURequestTarget.dataLoggingStick, 3, 0.toUShort())
    val bytes = request.toBytes()

    client.outputStream.write(bytes)
    val buffer = ByteArray(1024)
    client.getInputStream().read(buffer)
    println(buffer.toHexString(HexFormat.UpperCase))
    ResponseFrame.parseHoldingRegister(buffer).also {
        client.close()
    }
}


fun String.hexToBytes() = chunked(2)
    .map { it.toInt(16).toByte() }.toByteArray()

