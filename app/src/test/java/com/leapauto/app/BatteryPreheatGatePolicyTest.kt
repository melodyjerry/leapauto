package com.leapauto.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryPreheatGatePolicyTest {

    @Test
    fun `parseTemperatureC reads formatted telemetry values`() {
        assertEquals(28.0, BatteryPreheatGatePolicy.parseTemperatureC("28 °C")!!, 0.001)
        assertEquals(-8.0, BatteryPreheatGatePolicy.parseTemperatureC("-8 °C")!!, 0.001)
        assertEquals(12.5, BatteryPreheatGatePolicy.parseTemperatureC("12.5 °C")!!, 0.001)
        assertEquals(28.0, BatteryPreheatGatePolicy.parseTemperatureC("28℃")!!, 0.001)
        assertEquals(28.0, BatteryPreheatGatePolicy.parseTemperatureC(" 28 °C ")!!, 0.001)
    }

    @Test
    fun `parseTemperatureC tolerates missing or malformed telemetry`() {
        assertNull(BatteryPreheatGatePolicy.parseTemperatureC(null))
        assertNull(BatteryPreheatGatePolicy.parseTemperatureC(""))
        assertNull(BatteryPreheatGatePolicy.parseTemperatureC("--"))
        assertNull(BatteryPreheatGatePolicy.parseTemperatureC("n/a"))
    }

    @Test
    fun `parseSocPercent reads percentage strings with and without the percent sign`() {
        assertEquals(42, BatteryPreheatGatePolicy.parseSocPercent("42%"))
        assertEquals(42, BatteryPreheatGatePolicy.parseSocPercent("42"))
        assertEquals(0, BatteryPreheatGatePolicy.parseSocPercent("0%"))
        assertEquals(100, BatteryPreheatGatePolicy.parseSocPercent("100%"))
        assertEquals(19, BatteryPreheatGatePolicy.parseSocPercent("19.6%"))
    }

    @Test
    fun `parseSocPercent tolerates missing or malformed values`() {
        assertNull(BatteryPreheatGatePolicy.parseSocPercent(null))
        assertNull(BatteryPreheatGatePolicy.parseSocPercent(""))
        assertNull(BatteryPreheatGatePolicy.parseSocPercent("--"))
    }

    @Test
    fun `warm cells warn before the command is sent`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = 28.0,
            socPercent = 80,
            chargingOrPlugged = false
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.WARN_TEMPERATURE, decision)
    }

    @Test
    fun `low state of charge without a charging connection blocks the command`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = -5.0,
            socPercent = 15,
            chargingOrPlugged = false
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.BLOCK_LOW_SOC, decision)
    }

    @Test
    fun `low state of charge is allowed while charging or plugged in`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = -5.0,
            socPercent = 15,
            chargingOrPlugged = true
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.ALLOW, decision)
    }

    @Test
    fun `low state of charge takes precedence over the temperature warning`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = 12.0,
            socPercent = 15,
            chargingOrPlugged = false
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.BLOCK_LOW_SOC, decision)
    }

    @Test
    fun `warm cells with low charge while plugged in still only warn`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = 12.0,
            socPercent = 15,
            chargingOrPlugged = true
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.WARN_TEMPERATURE, decision)
    }

    @Test
    fun `cold cells with sufficient charge are allowed`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = -5.0,
            socPercent = 80,
            chargingOrPlugged = false
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.ALLOW, decision)
    }

    @Test
    fun `unknown telemetry does not block the command`() {
        val decision = BatteryPreheatGatePolicy.decide(
            minCellTempC = null,
            socPercent = null,
            chargingOrPlugged = false
        )
        assertEquals(BatteryPreheatGatePolicy.Decision.ALLOW, decision)
    }

    @Test
    fun `exactly at the thresholds is treated as allowed`() {
        assertEquals(
            BatteryPreheatGatePolicy.Decision.ALLOW,
            BatteryPreheatGatePolicy.decide(
                minCellTempC = BatteryPreheatGatePolicy.MIN_ACTIVATION_TEMP_C,
                socPercent = BatteryPreheatGatePolicy.MIN_SOC_PERCENT,
                chargingOrPlugged = false
            )
        )
    }

    @Test
    fun `vehicle status soc fields are percentage strings that the policy can parse`() {
        // VehicleStatus.soc / preciseSoc are produced by VehicleStatusMapper.displayPreciseSoc,
        // which renders values such as "42%".
        val statusSoc = VehicleStatusMapper.displayPreciseSoc("42")
        assertEquals("42%", statusSoc)
        assertEquals(42, BatteryPreheatGatePolicy.parseSocPercent(statusSoc))
    }

    @Test
    fun `resolved soc prefers the precise signal when both are present`() {
        val resolved = VehicleHomeStatus.resolvedSoc("38%", "42%")
        assertEquals(38, BatteryPreheatGatePolicy.parseSocPercent(resolved))
    }

    @Test
    fun `battery preheat stop command is reported as in progress while running`() {
        assertTrue(
            QuickCommandExecutionPolicy.isCommandInProgress("batteryPreheat", "batteryPreheat")
        )
        assertTrue(
            QuickCommandExecutionPolicy.isCommandInProgress("batteryPreheat", "batteryPreheatOff")
        )
        assertFalse(
            QuickCommandExecutionPolicy.isCommandInProgress("batteryPreheat", "trunkOpen")
        )
    }
}
