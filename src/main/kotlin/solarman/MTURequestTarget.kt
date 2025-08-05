package de.klg71.solarman_sensor.solarman

enum class MTURequestTarget(val byte: Byte) {

    solarInverter(0x02),
    dataLoggingStick(0x01)
}
