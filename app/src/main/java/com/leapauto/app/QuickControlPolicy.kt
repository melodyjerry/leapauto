package com.leapauto.app

import java.util.Locale

/** Verified quick-control capability and telemetry policies. */
object VehicleQuickControlCapabilities {
    private val d19Pattern = Regex("(?i)(^|[^A-Za-z0-9])D19([^A-Za-z0-9]|$)")

    fun supportsFrunk(vehicleModel: String): Boolean =
        d19Pattern.containsMatchIn(vehicleModel)
}

/**
 * Verified activation gates for the home quick-action battery preheat toggle.
 *
 * The car-side BMS only engages the PTC heating circuit when a cell is colder than
 * [MIN_ACTIVATION_TEMP_C], and it refuses to start preheat below [MIN_SOC_PERCENT]
 * while the vehicle is neither charging nor plugged in.
 */
object BatteryPreheatGatePolicy {
    const val MIN_ACTIVATION_TEMP_C = 10.0
    const val MIN_SOC_PERCENT = 20

    enum class Decision {
        /** Temperature and state allow the command to be sent straight away. */
        ALLOW,

        /** Cell temperature is above the BMS activation window; the car may reject it. */
        WARN_TEMPERATURE,

        /** State of charge is too low without a charging connection; the car will refuse it. */
        BLOCK_LOW_SOC
    }

    /** Parses the formatted `minBatteryTemp` telemetry, e.g. `"28 °C"` or `"-8 °C"`. */
    fun parseTemperatureC(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().replace("°", "").replace("℃", "").replace("C", "", ignoreCase = true)
        return cleaned.trim().trimEnd('%').trim().toDoubleOrNull()
    }

    /** Parses a formatted SOC percentage, e.g. `"42%"` or `"42"`. */
    fun parseSocPercent(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removeSuffix("%").trim()
        return cleaned.toDoubleOrNull()?.toInt()
    }

    fun decide(
        minCellTempC: Double?,
        socPercent: Int?,
        chargingOrPlugged: Boolean
    ): Decision = when {
        // 低电量是不可强发的硬保护，必须优先于温度提示判定。
        !chargingOrPlugged && socPercent != null && socPercent < MIN_SOC_PERCENT -> Decision.BLOCK_LOW_SOC
        minCellTempC != null && minCellTempC > MIN_ACTIVATION_TEMP_C -> Decision.WARN_TEMPERATURE
        else -> Decision.ALLOW
    }
}

/** Maps only the verified battery thermal request values to a toggle state. */
object BatteryPreheatState {
    private const val HEATING_REQUEST = "4"
    private const val IDLE_REQUEST = "0"

    fun fromRaw(raw: Any?): Boolean? {
        if (raw == null) return null
        val value = raw.toString().trim()
        return when (value.lowercase(Locale.US)) {
            HEATING_REQUEST -> true
            IDLE_REQUEST -> false
            else -> null
        }
    }
}
