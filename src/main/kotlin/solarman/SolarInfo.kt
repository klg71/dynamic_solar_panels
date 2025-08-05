package de.klg71.solarman_sensor.solarman

data class SolarStringInfo(val id: Int, val voltage: Float, val current: Float)
data class SolarInfo(
    val strings: List<SolarStringInfo>,
    val totalPower: Float,
    val dailyProduction: Float,
    val acVoltage: Float,
)
