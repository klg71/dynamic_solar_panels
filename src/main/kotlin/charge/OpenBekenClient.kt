package de.klg71.solarman_sensor.charge

import feign.Param
import feign.RequestLine
import org.springframework.web.bind.annotation.ResponseStatus

interface OpenBekenClient {

    @RequestLine("GET /cmd_tool?cmd={command}")
    fun command(@Param("command") command: String): ResponseStatus
}