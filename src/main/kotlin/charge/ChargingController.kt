package de.klg71.solarman_sensor.charge

import de.klg71.solarman_sensor.getLogger
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class ChargingMode {
    DISABLED, CONSTANT_CURRENT, CONSTANT_VOLTAGE, CHARGING_ENDED
}

@OptIn(ExperimentalAtomicApi::class, ExperimentalUnsignedTypes::class)
@Component
class ChargingController(private val dispatcher: CoroutineDispatcher) {


    private val chargers = mutableMapOf<String, ChargingConfig>(
        "1" to ChargingConfig("zk_10022_2", "daly_bms_2", 24.8f, 18f, 10f),
    )

    private val logger = getLogger(ChargingController::class.java)
    private var mode: ChargingMode = ChargingMode.CONSTANT_CURRENT

    private val scope = CoroutineScope(dispatcher)

    @PostConstruct
    fun init() {
        chargers.forEach {
            scope.launch {
                Charger(it.key, it.value, this).start()
            }
        }
    }
}

