package de.klg71.solarman_sensor.charge

data class ChargingConfig(
    val mqqtBuckConverterTopic: String,
    val mqqtBMSTopic: String,
    val maxVoltage: Float,
    val minVoltage: Float,
    val chargingCurrent: Float,
)