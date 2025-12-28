package de.klg71.solarman_sensor.charge

enum class ChargerField(val address: Int, val ratio: Float, val unit: String) {

    setVoltage(0, 0.01f, "V"),
    setCurrent(1, 0.01f, "A"),
    outputVoltage(2, 0.01f, "V"),
    outputCurrent(3, 0.01f, "A"),
    outputPower(4, 0.1f, "W"),

    inputVoltage(5, 0.01f, "V"),
    temperature(13, 0.1f, "C"),
    constantVoltageProtection(17, 1.0f, ""),
    switchOutput(18, 1.0f, ""),

}