package de.klg71.solarman_sensor.charge

data class ChargerInfo(
    val setOutputVoltage: Float,
    val setOutputCurrent: Float,
    val inputVoltage: Float,
    val temperature: Float,
    val outputPower: Float,
    val constantVoltageState: Boolean,
    val powerOnState: Boolean,
)
