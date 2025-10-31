package de.klg71.solarman_sensor.power

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Component
internal class DynamicConsumerSwitch(private val powerRegulator: PowerRegulator,
                                     private val dispatcher: CoroutineDispatcher,
    private val objectMapper: ObjectMapper) {
    private val scope = CoroutineScope(dispatcher)
    private val logger = getLogger(PowerRegulator::class.java)


    private val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkN2U4MmRlMGM1Zjk0MmU4YmVmNDY4NzA4ZjNhMGQ3NyIsImlhdCI6MTc1NzA4MTUzNywiZXhwIjoyMDcyNDQxNTM3fQ.cYRHOxjm9uprcMhHSfWiqqBxVA79T9bLF_FNQzpUAIY"
    @PostConstruct
    fun init() {

        val client = Feign.Builder().run {
            decoder(JacksonDecoder(objectMapper))
            encoder(JacksonEncoder(objectMapper))
            requestInterceptor {
                it.header("Authorization","Bearer $token")
            }
            target(HomeAssistantClient::class.java, "https://homeassistant.tail592ffe.ts.net")
        }
        scope.launch {
            controlConsumers(client)
        }
    }
    private suspend fun controlConsumers(client: HomeAssistantClient) {
        while(true){
            if(powerRegulator.getCurrentSetPower()<2000  ){

            }
            delay(5000)
        }
    }


}