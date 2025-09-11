package de.klg71.solarman_sensor.solarman

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.random.Random

data class RequestFrame(val payload: MTURequestFrame) {
    private val header: Byte = 0xA5.toByte()
    private val requestControlCode: Short = 0x1045
    private val loggerSerial = "968779e4".hexToBytes()
    private val footer: Byte = 0x15
    fun toBytes(): ByteArray {
        val payload = payload.toBytes()
        val length: Int = 1 + 2 + 2 + 2 + 4 + (payload.size.toShort())
        val mtuBody = ByteBuffer.allocate(length).apply {
            put(header)
            put(
                ByteBuffer.allocate(Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(payload.size.toShort())
                    .array()
            )
            putShort(requestControlCode)

            put(Random(0).nextBytes(2))
            put(loggerSerial)
            put(payload)
        }.array()
        return ByteBuffer.allocate(length + 2).apply {
            put(mtuBody)
            var byteCheck: Byte = 0

            mtuBody.forEachIndexed() { i, it ->
                if (i > 0) {
                    byteCheck = ((byteCheck + it).toByte()).and(0xFF.toByte())
                }
            }
            put(byteCheck)

            put(footer)
        }.array()
    }

    companion object {
        fun readRegister(target: MTURequestTarget, functionCode: Byte, startAddress: UShort): RequestFrame {
            return MTURequestFrame.MTURequestFramePayload(functionCode, startAddress, 0x1).let {
                MTURequestFrame(target, it)
            }.let {
                RequestFrame(it)
            }
        }

        suspend fun setPower(power: Int): RequestFrame {
            if (power !in 200..<2001) {
                error("Power can only be set between 200 and 2000, you tried to set $power")
            }
            return MTURequestFrame.MTURequestFramePayload(
                0x02,
                0x47D.toUShort(),
                (((power-200) / 20)).toShort()
            ).let {
                MTURequestFrame(MTURequestTarget.solarInverter, it, "d002")
            }.let {
                RequestFrame(it)
            }
        }

    }
}
