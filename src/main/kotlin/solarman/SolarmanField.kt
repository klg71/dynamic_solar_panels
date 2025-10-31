package de.klg71.solarman_sensor.solarman

enum class SolarmanField(val address: Int, val ratio: Float, val unit: String) {

    pvVoltage1(0, 0.1f, "V"),
    pvVoltage2(1, 0.1f, "V"),
    pvVoltage3(2, 0.1f, "V"),
    pvVoltage4(3, 0.1f, "V"),

    pvCurrent1(5, 0.05f, "A"),
    pvCurrent2(6, 0.05f, "A"),
    pvCurrent3(7, 0.05f, "A"),
    pvCurrent4(8, 0.05f, "A"),

    acVoltage(11, 0.1f, "V"),
    totalPower(15, 0.1f, "W"),
    dailyProduction(20, 0.01f, "kWh"),
}
