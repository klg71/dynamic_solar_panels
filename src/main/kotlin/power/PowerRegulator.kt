package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
internal class PowerRegulator(private val objectMapper: ObjectMapper, private val dispatcher: CoroutineDispatcher) {
    private lateinit var client: HomeAssistantClient
    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"
    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        client = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            requestInterceptor {
                it.header("Authorization", "Bearer: $token")
            }
            target(HomeAssistantClient::class.java, "https://homeassistant.tail592ffe.ts.net")
        }
        scope.launch { controlPower() }
    }

    private suspend fun controlPower() {
        TODO("Not yet implemented")
    }


}
