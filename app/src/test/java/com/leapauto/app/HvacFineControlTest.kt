package com.leapauto.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HvacFineControlTest {

    @Test
    fun vehicleCapabilityUsesVerifiedFuncConfigRanges() {
        val capability = HvacCapabilityParser.parse(
            JSONObject(
                """{
                    "HVAC": {
                        "temperature": {"min": "18", "max": 30},
                        "fan": {"min": 1, "max": "6"}
                    }
                }"""
            )
        )

        assertEquals(18, capability.temperatureMinC)
        assertEquals(30, capability.temperatureMaxC)
        assertEquals(1, capability.fanMin)
        assertEquals(6, capability.fanMax)
        assertEquals(1..6, capability.effectiveUiFanRange())
    }

    @Test
    fun invalidOrMissingCapabilityFallsBackToDocumentedUiRanges() {
        val invalid = HvacCapabilityParser.parse(
            JSONObject(
                """{
                    "HVAC": {
                        "temperature": {"min": 50, "max": 10},
                        "fan": {"min": 0, "max": 7}
                    }
                }"""
            )
        )
        val missing = HvacCapabilityParser.parse(null)

        listOf(invalid, missing).forEach { capability ->
            assertEquals(19, capability.temperatureMinC)
            assertEquals(32, capability.temperatureMaxC)
            assertEquals(1..7, capability.effectiveUiFanRange())
        }
    }

    @Test
    fun detailedCommandAlwaysSendsTheSevenStringFields() {
        val command = Commands.buildDetailedAc(
            detailedCommand(
                operation = HvacOperation.ON,
                circle = AirCircle.OUTER,
                windshieldDefogging = true,
                outlet = AirOutlet.ALL
            )
        )
        val state = JSONObject(command.stateJson)

        assertEquals("170", command.cmdid)
        assertEquals(7, state.length())
        assertEquals("manual", state.getString("operate"))
        assertEquals("23", state.getString("temperature"))
        assertEquals("4", state.getString("windlevel"))
        assertEquals("cold", state.getString("mode"))
        assertEquals("out", state.getString("circle"))
        assertEquals("1", state.getString("wshld"))
        assertEquals("all", state.getString("position"))
        state.keys().forEachRemaining { key -> assertTrue(state.get(key) is String) }
    }

    @Test
    fun windshieldDefoggingAndWindshieldOutletRemainIndependent() {
        val defogWithoutWindshieldOutlet = JSONObject(
            Commands.buildDetailedAc(
                detailedCommand(windshieldDefogging = true, outlet = AirOutlet.ALL)
            ).stateJson
        )
        val windshieldOutletWithoutDefog = JSONObject(
            Commands.buildDetailedAc(
                detailedCommand(windshieldDefogging = false, outlet = AirOutlet.WINDSHIELD)
            ).stateJson
        )

        assertEquals("1", defogWithoutWindshieldOutlet.getString("wshld"))
        assertEquals("all", defogWithoutWindshieldOutlet.getString("position"))
        assertEquals("0", windshieldOutletWithoutDefog.getString("wshld"))
        assertEquals("wshld", windshieldOutletWithoutDefog.getString("position"))
    }

    @Test
    fun offCommandKeepsCurrentSettingsButUsesOffMode() {
        val state = JSONObject(
            Commands.buildDetailedAc(detailedCommand(operation = HvacOperation.OFF)).stateJson
        )
        val expectation = Commands.detailedAcExpectation(detailedCommand(operation = HvacOperation.OFF))

        assertEquals("off", state.getString("operate"))
        assertEquals("nohotcold", state.getString("mode"))
        assertEquals("23", state.getString("temperature"))
        assertEquals("4", state.getString("windlevel"))
        assertEquals(false, expectation.acSwitch)
        assertTrue(expectation.hasUnavailableFields)
    }

    @Test
    fun deodorizeCommandUsesTheVerifiedNoHeatNoCoolMode() {
        val command = Commands.buildDetailedAc(
            detailedCommand(
                operation = HvacOperation.DEODORIZE,
                circle = AirCircle.OUTER,
                outlet = AirOutlet.ALL
            )
        )
        val state = JSONObject(command.stateJson)

        assertEquals("快速除味", command.label)
        assertEquals("manual", state.getString("operate"))
        assertEquals("nohotcold", state.getString("mode"))
        assertEquals("out", state.getString("circle"))
        assertEquals("all", state.getString("position"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun commandRejectsTemperatureOutsideVehicleCapability() {
        Commands.buildDetailedAc(detailedCommand(temperatureC = 31))
    }

    @Test(expected = IllegalArgumentException::class)
    fun commandRejectsFanOutsideVehicleCapability() {
        Commands.buildDetailedAc(detailedCommand(windLevel = 7))
    }

    @Test
    fun telemetryExpectationDoesNotClaimOutletReadback() {
        val expectation = Commands.detailedAcExpectation(detailedCommand())

        assertEquals(true, expectation.acSwitch)
        assertEquals(23, expectation.temperatureC)
        assertEquals(4, expectation.windLevel)
        assertEquals(AirCircle.INNER, expectation.circle)
        assertEquals(false, expectation.windshieldDefogging)
        assertTrue(expectation.hasUnavailableFields)
        assertFalse(expectation == ClimateTelemetryExpectation())
    }

    @Test
    fun detailedCommandSelectsHotModeForHeatingTemperatures() {
        val coldCommand = Commands.buildDetailedAc(detailedCommand(temperatureC = 23))
        val hotCommand = Commands.buildDetailedAc(detailedCommand(temperatureC = 28))

        val coldState = JSONObject(coldCommand.stateJson)
        val hotState = JSONObject(hotCommand.stateJson)

        assertEquals("cold", coldState.getString("mode"))
        assertEquals("hot", hotState.getString("mode"))
    }

    @Test
    fun climateDialogStateToneSeparatesVehicleStatusFromEditedTemperature() {
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        val projectDir = generateSequence(java.io.File(workingDirectory)) { it.parentFile ?: return@generateSequence null }
            .first { java.io.File(it, "app").isDirectory }
        val screenSource = java.io.File(projectDir, "app/src/main/java/com/leapauto/app/ui/LeapAutoScreen.kt").readText()

        // 验证弹窗的 climateTone 使用 ClimateTemperatureToneResolver.tone(..., targetTemperature = temperatureTarget.value)
        assertTrue(
            "空调弹窗必须使用 ClimateTemperatureToneResolver 并且依赖车端目标温度，绝不被滑动中的 editedTemperature 绑架",
            screenSource.contains("targetTemperature = temperatureTarget.value")
        )
        assertFalse(
            "空调弹窗严禁使用 editedTemperature < indoorTemp 粗暴盲目篡改工况文字",
            screenSource.contains("editedTemperature < indoorTemp")
        )
    }

    private fun detailedCommand(
        operation: HvacOperation = HvacOperation.ON,
        temperatureC: Int = 23,
        windLevel: Int = 4,
        circle: AirCircle = AirCircle.INNER,
        windshieldDefogging: Boolean = false,
        outlet: AirOutlet = AirOutlet.ALL
    ) = AirConditioningCommand(
        operation = operation,
        temperatureC = temperatureC,
        capability = HvacCapability(temperatureMinC = 18, temperatureMaxC = 30, fanMin = 1, fanMax = 6),
        windLevel = windLevel,
        circle = circle,
        windshieldDefogging = windshieldDefogging,
        outlet = outlet
    )
}
