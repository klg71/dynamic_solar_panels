package de.klg71.solarman_sensor.battery

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttPublisher(
    private val client: MqttClient, private val mqttRootPrefix: String, private val name: String,
    private val objectMapper: ObjectMapper
) {

    private fun mqttRoot()="$mqttRootPrefix/$name"

    fun homeAssistantDiscovery(measurement: Measurement, stateTopic: String, uniqueId: String) {
        val unit = if (measurement.unit.isNotBlank()) {
            mapOf(
                "unit_of_measurement" to measurement.unit,
            )
        } else {
            emptyMap()
        }
        val deviceClass = if (measurement.deviceClass.isNotBlank()) {
            mapOf("device_class" to measurement.deviceClass)
        } else {
            emptyMap()
        }
        (mapOf(
            "name" to "Daly-${name}-$uniqueId",
            "state_topic" to "${mqttRoot()}/$stateTopic",
            "state_class" to "measurement",
            "value_template" to "{{ value }}",
            "unique_id" to "${name}_${uniqueId}",
            "device" to device(),
            "platform" to "mqtt"
        ) + unit + deviceClass).let {
            client.publish("homeassistant/sensor/daly_${name}_$uniqueId/config", it)
        }
    }

    fun homeAssistantDiscoverySwitch(stateTopic: String, commandTopic: String, uniqueId: String) {
        mapOf(
            "name" to "Daly-${name}-$uniqueId",
            "state_topic" to "${mqttRoot()}/$stateTopic",
            "command_topic" to "${mqttRoot()}/$commandTopic",
            "unique_id" to "_${uniqueId}",
            "device" to device(),
            "platform" to "switch"
        ).let {
            client.publish("homeassistant/switch/daly_${name}_$uniqueId/config", it)
        }
    }
    fun subscribe(topic:String,callback:(String, MqttMessage)->Unit){
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
        if (payload is String) {
            publish(topic, MqttMessage(payload.toByteArray()).also { it.isRetained = retain })
        } else {
            publish(topic, MqttMessage(objectMapper.writeValueAsBytes(payload)).also { it.isRetained = retain })
        }

    }
}

