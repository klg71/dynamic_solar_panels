package de.klg71.solarman_sensor

import kotlinx.coroutines.Dispatchers
import org.eclipse.paho.client.mqttv3.MqttClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class AppConfiguration {
    @Bean
    fun dispatcher() = Dispatchers.Default
    @Bean
    fun mqttClient()= MqttClient("tcp://homeassistant-mosquitto.tail592ffe.ts.net:1883","dynamic-solar-panels").also { it.connect() }

}
