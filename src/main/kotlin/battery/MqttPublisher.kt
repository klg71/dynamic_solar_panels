package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import de.klg71.solarman_sensor.getLogger
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttPublisher(
    private val client: MqttClient, private val mqttRootPrefix: String, private val name: String,
    private val objectMapper: ObjectMapper
) {

    private fun mqttRoot() = "$mqttRootPrefix/$name"

    private val logger = getLogger(MqttPublisher::class.java)
    fun homeAssistantDiscovery(measurement: Measurement, stateTopic: String, uniqueId: String) {
        buildMap {
            put("name", stateTopic)
            put("state_topic", "${mqttRoot()}/$stateTopic")
            if (measurement.isMeasurement) {
                put("state_class", "measurement")
            }
            if (measurement.unit.isNotBlank()) {
                put("unit_of_measurement", measurement.unit)
            }
            if (measurement.deviceClass.isNotBlank()) {
                put("device_class", measurement.deviceClass)
            }
            measurement.payloadOn?.let {
                put("payload_on", it)
            }
            measurement.payloadOff?.let {
                put("payload_off", it)
            }
            measurement.precision?.let {
                put("suggested_display_precision", it)
            }
            put("value_template", "{{ value }}")
            put("unique_id", "${name}_${uniqueId}")
            put("device", device())
            put("platform", "mqtt")
        }.let {
            if (measurement.isBinary) {
                client.publish("homeassistant/binary_sensor/daly_${name}_$uniqueId/config", it)
            } else {
                client.publish("homeassistant/sensor/daly_${name}_$uniqueId/config", it)
            }
        }
    }

    fun homeAssistantDiscoverySwitch(stateTopic: String, commandTopic: String, uniqueId: String) {
        mapOf(
            "name" to stateTopic,
            "state_topic" to "${mqttRoot()}/$stateTopic",
            "command_topic" to "${mqttRoot()}/$commandTopic",
            "unique_id" to "${name}_${uniqueId}",
            "device" to device(),
            "platform" to "switch"
        ).let {
            client.publish("homeassistant/switch/daly_${name}_$uniqueId/config", it)
        }
    }

    fun subscribe(topic: String, callback: (String, MqttMessage) -> Unit) {
        client.subscribe("${mqttRoot()}$topic", callback)
    }


    private fun device(): Map<String, Any> = mapOf(
        "identifiers" to listOf(
            "daly-battery-${name}"
        ),
        "name" to "DALY-Battery-${name}",
        "model" to "BMS",
        "manufacturer" to "DALY"
    )

    public fun <T> publish(topic: String, payload: T, retain: Boolean = false) {
        client.publish("${mqttRoot()}$topic", payload, retain)
    }

    private fun <T> MqttClient.publish(topic: String, payload: T, retain: Boolean = false) {
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

