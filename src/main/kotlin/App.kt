package de.klg71.solarman_sensor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}

fun getLogger(classInstance: Class<out Any>) = LoggerFactory.getLogger(classInstance.name) ?: error(
    "Could not get logger!"
)
