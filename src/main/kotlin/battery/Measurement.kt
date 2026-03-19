package de.klg71.solarman_sensor.battery

enum class Measurement(
    val unit: String, val deviceClass: String, val isMeasurement: Boolean = true,
    val payloadOn: String? = null, val payloadOff: String? = null, val isBinary: Boolean = false,
    val precision: Int? = null
) {
    VOLT("V", "voltage", precision = 3),
    NUMBER("", ""),
    CURRENT("A", "current", precision = 3),
    POWER("W", "power", precision = 3),
    ENERGY("kWh", "energy", precision = 3),
    PERCENTAGE("%", "battery", precision = 3),
    TEMPERATURE("°C", "temperature", precision = 3),
    STRING("", "enum", isMeasurement = false),
    BINARY_POWER("", "opening", false, payloadOn = "ON", payloadOff = "OFF", isBinary = true),
}