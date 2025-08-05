package de.klg71.solarman_sensor.solarman

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

class ResponseFrame {

    companion object {

        fun parse(bytes: ByteArray): SolarInfo? {
            val reader = ByteArrayInputStream(bytes)
            val header = reader.read()

            val length = ByteBuffer.wrap(reader.readNBytes(2)).getShort()
            val controlCode = ByteBuffer.wrap(reader.readNBytes(2)).getShort()
            val serial = ByteBuffer.wrap(reader.readNBytes(2)).getShort()
            val loggerSerial = ByteBuffer.wrap(reader.readNBytes(4)).getInt()

            val frameType = reader.read()
            val status = reader.read()
            val totalWorkingTime = ByteBuffer.wrap(reader.readNBytes(4)).getInt()
            val powerOnTime = ByteBuffer.wrap(reader.readNBytes(4)).getInt()
            val offSetTime = ByteBuffer.wrap(reader.readNBytes(4)).getInt()

            val mtuId = reader.read()
            val functionCode = reader.read()
            val incomingData = mutableListOf<UShort>()
            val fields = mutableMapOf<SolarmanField, Float>()
            if (functionCode > 0) {
                val lengthMtu = reader.read()
                repeat(lengthMtu) {
                    incomingData.add(ByteBuffer.wrap(reader.readNBytes(2)).getShort().toUShort())
                }
                SolarmanField.entries.forEach {
                    fields[it] = (incomingData[it.address].toFloat() * it.ratio)
                }
                return SolarInfo(
                    listOf(
                        SolarStringInfo(
                            0, fields[SolarmanField.pvVoltage1] ?: 0f, fields[SolarmanField.pvCurrent1] ?: 0f
                        ),
                        SolarStringInfo(
                            1, fields[SolarmanField.pvVoltage2] ?: 0f, fields[SolarmanField.pvCurrent2] ?: 0f
                        ),
                        SolarStringInfo(
                            2, fields[SolarmanField.pvVoltage3] ?: 0f, fields[SolarmanField.pvCurrent3] ?: 0f
                        ),
                        SolarStringInfo(
                            3, fields[SolarmanField.pvVoltage4] ?: 0f, fields[SolarmanField.pvCurrent4] ?: 0f
                        ),
                    ), fields[SolarmanField.totalPower] ?: 0f, fields[SolarmanField.dailyProduction] ?: 0f,
                    fields[SolarmanField.acVoltage] ?: 0f
                )
            } else {
                return null
            }
        }
    }

}
