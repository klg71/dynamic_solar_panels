package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.annotation.JsonProperty
import feign.RequestLine
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

data class HomeAssistantState(val attributes: Map<String, JvmType.Object>,
    @JsonProperty("entity_id") val entityId: String,
    @JsonProperty("last_changed") val lastChanged: String,

    @JsonProperty("last_updated") val lastUpdated: String,
    val state: String)

data class BitshakeState(
    @JsonProperty("StatusSNS") val statusSNS: StatusSNS) {
    data class StatusSGM(
        @JsonProperty("E_in") val energyUsed: Double,
        @JsonProperty("E_out") val energySupplied: Double,
        @JsonProperty("Power") val power: Int,
    )

    data class StatusSNS(@JsonProperty("Time") val time: String, val SGM: StatusSGM)
}

internal interface HomeAssistantClient {
    @RequestLine("GET /api/states/sensor.obk5398d833_apparent_power")
    fun getCurrentInverterPower(): HomeAssistantState

    @RequestLine("GET /api/states/sensor.switch.switch_kitchen_heating_1")
    fun getCurrentKitchenState(): HomeAssistantState
}

internal interface BitshakeClient {
    @RequestLine("GET /cm?cmnd=status%2010")
    fun getCurrentPower(): BitshakeState
}
