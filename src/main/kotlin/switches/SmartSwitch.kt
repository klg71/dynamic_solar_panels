package de.klg71.solarman_sensor.switches

internal interface SmartSwitch {
    fun id(): String

    fun status(): Boolean
    fun setStatus(boolean: Boolean)
}
