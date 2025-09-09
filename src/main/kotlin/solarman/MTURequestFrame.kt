package de.klg71.solarman_sensor.solarman

import java.nio.ByteBuffer

data class MTURequestFrame(val target: MTURequestTarget, val payload: MTURequestFramePayload,val sensorType:String="0000") {

    data class MTURequestFramePayload(val functionCode: Byte, val address: UShort, val numberOfRegisters: Short,val slaveId:Byte=0x01) {
        fun toBytes(): ByteArray {
            val payload = ByteBuffer.allocate(6).apply {
                put(slaveId)
                put(functionCode)
                putShort(address.toShort())
                putShort(numberOfRegisters)
            }.array()
            return ByteBuffer.allocate(8).apply {
                put(payload)
                CRC16Modbus().let {
                    it.update(payload)
                    put(it.crcBytes)
                }
            }.array()
        }
    }

    private val sensorTypeBytes = sensorType.hexToBytes().toList()
    private val totalWorkingTime = "00000000".hexToBytes().toList()
    private val powerOnTime = "00000000".hexToBytes().toList()
    private val offsetTime = "00000000".hexToBytes().toList()


    fun toBytes(): ByteArray {
        return buildList {
            //header
            add(target.byte)
            addAll(sensorTypeBytes)
            addAll(totalWorkingTime)
            addAll(powerOnTime)
            addAll(offsetTime)

            addAll(payload.toBytes().toList())
        }.toByteArray()
    }
}
