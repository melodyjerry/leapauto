package com.leapauto.app.weather

import com.leapauto.app.ClimatePresetAction
import com.leapauto.app.Commands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimateSmartRecommendationPolicyTest {
    private fun weather(temperature: String = "15", humidity: String = "50", sky: String = "晴") =
        LiveWeather(city = "测试", temperature = temperature, humidity = humidity, weather = sky)

    @Test
    fun `only absolute cabin thresholds trigger cooling or heating`() {
        for (acOn in listOf(false, true)) {
            assertEquals("quickCool", ClimateSmartRecommendationPolicy.resolve("32℃", null, acOn)?.command)
            assertEquals("quickHeat", ClimateSmartRecommendationPolicy.resolve("12°C", null, acOn)?.command)
            for (temp in listOf("12.1", "21", "31.9")) {
                assertNull(ClimateSmartRecommendationPolicy.resolve(temp, weather(), acOn))
                assertNull(ClimateSmartRecommendationPolicy.resolve(temp, weather("-10"), acOn))
            }
        }
    }

    @Test
    fun `missing invalid or nonfinite cabin readings never infer thermal advice`() {
        for (temp in listOf(null, "", "--", "NaN", "Infinity", "-Infinity", "81", "-41")) {
            for (outdoor in listOf("-10", "40")) {
                assertNull(ClimateSmartRecommendationPolicy.resolve(temp, weather(outdoor), false))
            }
            assertEquals("defrost", ClimateSmartRecommendationPolicy.resolve(temp, weather(humidity = "80"), true)?.command)
        }
    }

    @Test
    fun `thermal advice precedes ordinary humidity but rain and snow remain first`() {
        assertEquals("quickCool", ClimateSmartRecommendationPolicy.resolve("35", weather(humidity = "90"), false)?.command)
        assertEquals("quickHeat", ClimateSmartRecommendationPolicy.resolve("10", weather(humidity = "90"), false)?.command)
        for (sky in listOf("小雨", "大雪")) {
            val suggestion = requireNotNull(ClimateSmartRecommendationPolicy.resolve("35", weather(sky = sky), true))
            assertEquals("defrost", suggestion.command)
            assertTrue(suggestion.description.contains("若出现起雾"))
            assertFalse(suggestion.title.contains("已起雾"))
        }
        assertNull(ClimateSmartRecommendationPolicy.resolve("21", weather(humidity = "79"), false))
        assertEquals("defrost", ClimateSmartRecommendationPolicy.resolve("21", weather(humidity = "80"), false)?.command)
        assertNull(ClimateSmartRecommendationPolicy.resolve("21", weather(humidity = "101"), false))
    }

    @Test
    fun `presets match enum names and supported commands without suppressing unrelated advice`() {
        val cases = listOf(
            Triple("35", weather(), ClimatePresetAction.QUICK_COOL),
            Triple("10", weather(), ClimatePresetAction.QUICK_HEAT),
            Triple("21", weather(sky = "小雨"), ClimatePresetAction.WINDSHIELD_DEFROST)
        )
        for ((temp, liveWeather, preset) in cases) {
            val suggestion = requireNotNull(ClimateSmartRecommendationPolicy.resolve(temp, liveWeather, true))
            assertEquals(preset, suggestion.preset)
            assertEquals("170", Commands.build(suggestion.command).cmdid)
            for (active in listOf(preset.name, suggestion.command)) {
                assertNull(ClimateSmartRecommendationPolicy.resolve(temp, liveWeather, true, active))
            }
            assertEquals(suggestion, ClimateSmartRecommendationPolicy.resolve(temp, liveWeather, true, ClimatePresetAction.DEODORIZE.name))
            assertEquals(suggestion, ClimateSmartRecommendationPolicy.resolve(temp, liveWeather, false, preset.name))
            assertFalse(suggestion.title.contains("暴晒"))
        }
        assertEquals("quickCool", ClimateSmartRecommendationPolicy.resolve("35", weather(sky = "雨"), true, ClimatePresetAction.WINDSHIELD_DEFROST.name)?.command)
        assertEquals("defrost", ClimateSmartRecommendationPolicy.resolve("35", weather(humidity = "90"), true, ClimatePresetAction.QUICK_COOL.name)?.command)
    }
}
