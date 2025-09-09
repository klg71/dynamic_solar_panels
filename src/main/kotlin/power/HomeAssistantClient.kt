package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.annotation.JsonKey
import com.fasterxml.jackson.annotation.JsonProperty
import feign.RequestLine
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

data class HomeAssistantState(val attributes: Map<String, JvmType.Object>,
                              @JsonProperty("entity_id") val entityId: String,
                              @JsonProperty("last_changed") val lastChanged: String,

                              @JsonProperty("last_updated") val lastUpdated: String,
                              val state: String)

internal interface HomeAssistantClient {
    @RequestLine("GET /api/states/sensor.bitshake_smartmeterreader_sgm_power")
    fun getCurrentPower(): HomeAssistantState
}
