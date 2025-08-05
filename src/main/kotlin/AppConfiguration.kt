package de.klg71.solarman_sensor

import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class AppConfiguration {
    @Bean
    fun dispatcher() = Dispatchers.Default

}
