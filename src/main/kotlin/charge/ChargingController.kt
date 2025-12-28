package de.klg71.solarman_sensor.charge

import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class ChargingMode {
    DISABLED, CONSTANT_CURRENT, CONSTANT_VOLTAGE, CHARGING_ENDED
}

@OptIn(ExperimentalAtomicApi::class)
@Component
class ChargingController(private val dispatcher: CoroutineDispatcher) {

    companion object {
        const val STARTING_VOLTAGE = 6.2f
        const val END_VOLTAGE = 8.4f
        const val MAX_CURRENT = 10
        const val VOLTAGE_POSITIVE_STEP = 0.01f
        const val VOLTAGE_NEGATIVE_STEP = 0.02f
        const val CURRENT_CUTOFF = 0.1f
        const val VOLTAGE_INPUT_THRESHOLD = 80f
    }

    private val logger = getLogger(ChargingController::class.java)
    private var mode: ChargingMode = ChargingMode.CONSTANT_CURRENT

    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        scope.launch {
            chargingJob()
        }
    }

    private suspend fun chargingJob() {
        logger.info("Deactivate charger")
        deactivateCharger()
        while (true) {
            try {
                chargeTick()
                activateCharger()
            } catch (e: Exception) {
                logger.error("Error while charging", e)
            }
            delay(100)
        }
    }

    private fun activateCharger() {
        TODO("Not yet implemented")
    }

    private fun deactivateCharger() {
        TODO("Not yet implemented")
    }

    private suspend fun chargeTick() {
        when (mode) {
            ChargingMode.CONSTANT_CURRENT -> constantCurrentCharging()
            ChargingMode.CONSTANT_VOLTAGE -> constantVoltageCharging()
            ChargingMode.CHARGING_ENDED -> delay(1000)
            ChargingMode.DISABLED -> {
                if (inputVoltage() > VOLTAGE_INPUT_THRESHOLD) {

                }

            }
        }
    }

    private fun constantCurrentCharging() {
        val currentVoltage = currentVoltage()
        if (inputVoltage() < VOLTAGE_INPUT_THRESHOLD) {
            logger.info("Input voltage too low, disabling")
            mode = ChargingMode.DISABLED
        }
        if (currentVoltage < STARTING_VOLTAGE) {
            error("Voltage below starting Voltage!")
        }

        if ((currentVoltage - END_VOLTAGE) < VOLTAGE_POSITIVE_STEP) {
            if (currentCurrent() < MAX_CURRENT) {
                setVoltage(currentVoltage + VOLTAGE_POSITIVE_STEP)
            } else {
                setVoltage(currentVoltage - VOLTAGE_NEGATIVE_STEP)
            }
        } else {
            mode = ChargingMode.CONSTANT_VOLTAGE
        }
    }

    private fun constantVoltageCharging() {
        val currentVoltage = currentVoltage()
        if (inputVoltage() < VOLTAGE_INPUT_THRESHOLD) {
            logger.info("Input voltage too low, disabling")
            mode = ChargingMode.DISABLED
        }
        if (currentCurrent() < CURRENT_CUTOFF) {
            mode = ChargingMode.CHARGING_ENDED
        }
    }

    private fun setVoltage(voltage: Float) {

    }

    fun currentCurrent() = 1f
    fun currentVoltage(): Float = 3f
    fun inputVoltage() = 10f
}