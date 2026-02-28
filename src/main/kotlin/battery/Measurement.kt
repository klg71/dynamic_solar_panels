package de.klg71.solarman_sensor.battery

enum class Measurement(val unit: String, val deviceClass: String) {
    VOLT("V", "voltage"),
    CURRENT("A", "current"),
    POWER("W", "power"),
    PERCENTAGE("%", "battery"),
    TEMPERATURE("°C", "temperature"),
    STRING("", "enum"),
    BINARY_POWER("", "power"),
}