package de.klg71.solarman_sensor.dynamicSolarArray

import feign.Param
import feign.RequestLine

data class SwitchStatus(val id: Int, val source: String, val output: Boolean, val temperature: Map<String, Float>)

internal interface SwitchClient {
    @RequestLine("GET /rpc/Switch.GetStatus?id=0")
    fun switchStatus(): SwitchStatus

    @RequestLine("GET /rpc/Switch.Set?id=0&on={status}")
    fun set(@Param("status") status: Boolean): SwitchStatus
}
