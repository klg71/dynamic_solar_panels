package de.klg71.solarman_sensor.charge

import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

@Component
@OptIn(ExperimentalUnsignedTypes::class)
class MQTTStore(private val dispatcher: CoroutineDispatcher) {
    private val scope = CoroutineScope(dispatcher)

    private val client = MQTTClient(
        MQTTVersion.MQTT5,
        "homeassistant-mosquitto.tail592ffe.ts.net",
        1883,
        null,
        publishReceived = ::publishReceived
    )

    @PostConstruct
    fun init() {
        scope.let {
            runBlocking {
                while (true) {
                    client.step()
                    delay(100)
                }
            }
        }
    }

    @PreDestroy
    fun tearDown() {
        scope.cancel()
    }

    private val cache = ConcurrentHashMap<String, ByteArray>()

    private fun publishReceived(publish: MQTTPublish) {
        cache[publish.topicName] = publish.payload?.toByteArray() ?: ByteArray(0)
    }

    fun subscribe(topic: String) {
        client.subscribe(listOf(Subscription(topic)))
    }

    fun getString(topic: String): String? {
        return cache[topic]?.decodeToString()
    }

    fun getFloat(topic: String): Float? {
        return cache[topic]?.let {
            ByteBuffer.wrap(it)
        }?.getFloat()
    }

    fun getInt(topic: String): Int? {
        return cache[topic]?.let {
            ByteBuffer.wrap(it)
        }?.getInt()
    }

}