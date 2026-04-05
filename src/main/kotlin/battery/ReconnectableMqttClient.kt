package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import kotlinx.coroutines.sync.Mutex
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class ReconnectableMqttClient(private val client: MqttClient, private val objectMapper: ObjectMapper) {
    private val logger = getLogger(ReconnectableMqttClient::class.java)
    private val reconnectMutex = Mutex()

    fun publish(topic: String, message: MqttMessage) {
        try {

            client.publish(topic, message)
        } catch (e: MqttException) {
            if (e.message?.contains("Client is not connected") == true) {
                reconnect()
            } else {
                throw e
            }
        }
    }

    fun subscribe(topic: String, subscribe: (topic: String, message: MqttMessage) -> Unit) {
        client.subscribe(topic, subscribe)
    }

    private fun reconnect() {
        if (reconnectMutex.tryLock()) {
            try {
                client.reconnect()
            } catch (e: Exception) {
                logger.warn("Error reconnecting", e)
            }
            reconnectMutex.unlock()
        }
    }

    public fun <T> publish(topic: String, payload: T, retain: Boolean = false) {
        try {

            if (payload is String) {
                publish(topic, MqttMessage(payload.toByteArray()).also { it.isRetained = retain })
            } else {
                publish(topic, MqttMessage(objectMapper.writeValueAsBytes(payload)).also { it.isRetained = retain })
            }
        } catch (e: MqttException) {
            if (e.message?.contains("Client is not connected") == true) {
                logger.info("Client is not connected, trying to reconnect")
                reconnect()
            } else {
                throw e
            }
        }


    }
}