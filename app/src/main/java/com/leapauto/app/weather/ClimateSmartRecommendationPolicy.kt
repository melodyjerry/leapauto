package com.leapauto.app.weather

import com.leapauto.app.ClimatePresetAction
import com.leapauto.app.R

data class ClimateSmartSuggestion(
    val title: String,
    val description: String,
    val actionLabel: String,
    val preset: ClimatePresetAction,
    val command: String,
    val iconRes: Int
)

object ClimateSmartRecommendationPolicy {
    fun resolve(
        indoorTempStr: String?,
        weather: LiveWeather?,
        isAcOn: Boolean,
        activePreset: String? = null
    ): ClimateSmartSuggestion? {
        val indoor = indoorTempStr?.replace("°C", "", ignoreCase = true)
            ?.replace("℃", "")?.replace("°", "")?.trim()?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in -40.0..80.0 }
        val rainOrSnow = weather?.isRainy == true || weather?.isSnowy == true
        val humid = weather?.humidityInt?.let { it in 80..100 } == true
        val candidates = buildList {
            if (rainOrSnow) add(defrostSuggestion(true))
            if (indoor != null && indoor >= 32.0) add(
                ClimateSmartSuggestion(
                    "车内偏热 · 建议降温", "车内 ${indoor}℃，可开启极速降温",
                    "极速降温", ClimatePresetAction.QUICK_COOL, "quickCool",
                    R.drawable.ic_phosphor_snowflake
                )
            )
            if (indoor != null && indoor <= 12.0) add(
                ClimateSmartSuggestion(
                    "车内偏冷 · 建议升温", "车内 ${indoor}℃，可开启极速升温",
                    "舒适升温", ClimatePresetAction.QUICK_HEAT, "quickHeat",
                    R.drawable.ic_phosphor_sun
                )
            )
            if (humid && !rainOrSnow) add(defrostSuggestion(false))
        }
        return candidates.firstOrNull {
            !isAcOn || (activePreset != it.preset.name && activePreset != it.command)
        }
    }

    private fun defrostSuggestion(rainOrSnow: Boolean) = ClimateSmartSuggestion(
        title = if (rainOrSnow) "雨雪天气 · 留意风挡" else "空气潮湿 · 留意风挡",
        description = "请留意前风挡，若出现起雾可开启除雾",
        actionLabel = "一键除雾",
        preset = ClimatePresetAction.WINDSHIELD_DEFROST,
        command = "defrost",
        iconRes = R.drawable.ic_windshield_defrost
    )
}
