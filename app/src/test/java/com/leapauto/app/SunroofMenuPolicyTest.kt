package com.leapauto.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SunroofMenuPolicyTest {

    @Test
    fun sunroofMenuItemsHaveDistinctOpenAndCloseIcons() {
        val items = SunroofMenuPolicy.items
        assertEquals(2, items.size)

        val openItem = items.first { it.command == "sunroofOpen" }
        val closeItem = items.first { it.command == "sunroofClose" }

        assertEquals("打开天窗", openItem.label)
        assertEquals("关闭天窗", closeItem.label)

        assertEquals(R.drawable.ic_sunroof_open, openItem.iconRes)
        assertEquals(R.drawable.ic_sunroof_close, closeItem.iconRes)

        // 核心门禁：打开和关闭按钮必须使用不同图标，防止视觉同质化
        assertNotEquals(openItem.iconRes, closeItem.iconRes)
    }

    @Test
    fun sunroofMenuPolicyResolvesIconsByCommand() {
        assertEquals(R.drawable.ic_sunroof_open, SunroofMenuPolicy.iconForCommand("sunroofOpen"))
        assertEquals(R.drawable.ic_sunroof_close, SunroofMenuPolicy.iconForCommand("sunroofClose"))
        assertEquals(R.drawable.ic_sunroof, SunroofMenuPolicy.iconForCommand("unknown"))
    }

    @Test
    fun sunroofMenuItemsMatchQuickCommandExecutionPolicy() {
        SunroofMenuPolicy.items.forEach { item ->
            assertTrue(
                "Command ${item.command} should be recognized by QuickCommandExecutionPolicy",
                QuickCommandExecutionPolicy.isCommandInProgress("sunroofGroup", item.command)
            )
        }
    }

    @Test
    fun sunroofCommandsBuildValidProtocolPayloads() {
        val openCmd = Commands.build("sunroofOpen")
        assertEquals("300", openCmd.cmdid)
        assertEquals("""{"value":"1"}""", openCmd.stateJson)
        assertEquals("天窗打开", openCmd.label)

        val closeCmd = Commands.build("sunroofClose")
        assertEquals("300", closeCmd.cmdid)
        assertEquals("""{"value":"0"}""", closeCmd.stateJson)
        assertEquals("天窗关闭", closeCmd.label)
    }

    @Test
    fun sunroofFeedbackFormattingProvidesAccurateStatusText() {
        assertEquals("开启天窗中...", ControlFeedbackFormatter.inProgress("sunroofOpen", "打开天窗"))
        assertEquals("关闭天窗中...", ControlFeedbackFormatter.inProgress("sunroofClose", "关闭天窗"))

        assertEquals("天窗已开启", ControlFeedbackFormatter.success("sunroofOpen", "打开天窗"))
        assertEquals("天窗已关闭", ControlFeedbackFormatter.success("sunroofClose", "关闭天窗"))
    }

    @Test
    fun signalTableMapsSunroofOpeningSignal1718() {
        assertEquals("sunroofOpening", SignalTable.MAP["1718"])

        val raw = JSONObject().apply {
            put("1718", 10)
            put("1724", 10)
            put("vin", "TESTVIN")
        }
        val decoded = SignalTable.decode(raw)
        assertEquals(10, decoded.optInt("sunroofOpening"))
        assertEquals(10, decoded.optInt("roofOpening"))
    }

    @Test
    fun sunroofCapabilityPolicyRejectsSnaPlaceholdersAndValidatesOpening() {
        // 150 和 15 为未选配车型的 CAN 协议 SNA 占位符，必须严格判定为不可用且未开启
        assertFalse(SunroofCapabilityPolicy.isValidSignal(150))
        assertFalse(SunroofCapabilityPolicy.isValidSignal(15))
        assertFalse(SunroofCapabilityPolicy.isValidSignal(255))
        assertFalse(SunroofCapabilityPolicy.isValidSignal(-1))
        assertFalse(SunroofCapabilityPolicy.isValidSignal(null))

        assertFalse(SunroofCapabilityPolicy.isAvailable(150))
        assertFalse(SunroofCapabilityPolicy.isAvailable(15))
        assertFalse(SunroofCapabilityPolicy.isAvailable(null))
        assertFalse(SunroofCapabilityPolicy.isOpen(150))
        assertFalse(SunroofCapabilityPolicy.isOpen(15))
        assertNull(SunroofCapabilityPolicy.parseOpeningPercent(150))
        assertNull(SunroofCapabilityPolicy.parseOpeningPercent(15))

        // 0 为装配车型的全关状态
        assertTrue(SunroofCapabilityPolicy.isValidSignal(0))
        assertTrue(SunroofCapabilityPolicy.isAvailable(0))
        assertFalse(SunroofCapabilityPolicy.isOpen(0))
        assertEquals(0, SunroofCapabilityPolicy.parseOpeningPercent(0))

        // 1..10 为装配车型的开启状态
        assertTrue(SunroofCapabilityPolicy.isValidSignal(5))
        assertTrue(SunroofCapabilityPolicy.isAvailable(5))
        assertTrue(SunroofCapabilityPolicy.isOpen(5))
        assertEquals(50, SunroofCapabilityPolicy.parseOpeningPercent(5))

        assertTrue(SunroofCapabilityPolicy.isValidSignal(10))
        assertTrue(SunroofCapabilityPolicy.isAvailable(10))
        assertTrue(SunroofCapabilityPolicy.isOpen(10))
        assertEquals(100, SunroofCapabilityPolicy.parseOpeningPercent(10))
    }
}
