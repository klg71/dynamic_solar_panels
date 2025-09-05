package de.klg71.solarman_sensor.power

import feign.RequestLine
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

data class HomeAssistantState(val attributes: Map<String, JvmType.Object>,
    val entityId: String, val lastChanged: String, val lastUpdated: String, val state: String)

internal interface HomeAssistantClient {
    @RequestLine("GET /api/states/sensor.bitshake_smartmeterreader_sgm_power")
    fun getCurrentPower(): HomeAssistantState
}
