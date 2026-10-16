package com.leapauto.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetVehicleAppearanceTest {

    @Test
    fun `widget uses configured model and color only for display appearance`() {
        val appearance = ControlWidget.resolveWidgetAppearance(
            config = SessionStore.VehicleConfig(
                modelYear = "2026",
                powerType = SessionStore.VehiclePowerType.PURE_ELECTRIC,
                model = "C10",
                color = "metal_black"
            ),
            reportedCarType = "C16 纯电"
        )

        assertEquals("C10", appearance.model)
        assertEquals("metal_black", appearance.color.id)
    }

    @Test
    fun `widget normalizes B05 to shared Lafa5 appearance`() {
        val appearance = ControlWidget.resolveWidgetAppearance(
            config = SessionStore.VehicleConfig(
                model = "B05",
                color = "speed_orange"
            ),
            reportedCarType = "B05 纯电"
        )

        assertEquals("Lafa5", appearance.model)
        assertEquals("speed_orange", appearance.color.id)
    }

    @Test
    fun `widget falls back to reported model when configuration has not been saved`() {
        val appearance = ControlWidget.resolveWidgetAppearance(
            config = SessionStore.VehicleConfig(),
            reportedCarType = "C16 纯电"
        )

        assertEquals("C16", appearance.model)
        assertEquals("liquid_silver", appearance.color.id)
    }

    @Test
    fun `widget vehicle image scaling uses retina resolution and smooth filtering`() {
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        val projectDir = generateSequence(java.io.File(workingDirectory)) { it.parentFile }
            .first { java.io.File(it, "app").isDirectory }
        val widgetSource = java.io.File(projectDir, "app/src/main/java/com/leapauto/app/ControlWidget.kt").readText()
        val compactSource = java.io.File(projectDir, "app/src/main/java/com/leapauto/app/CompactControlWidget.kt").readText()

        org.junit.Assert.assertTrue(widgetSource.contains("targetWidth: Int = 680"))
        org.junit.Assert.assertTrue(widgetSource.contains("scaleRatio: Float = 0.94f"))
        org.junit.Assert.assertTrue(widgetSource.contains("FILTER_BITMAP_FLAG"))
        org.junit.Assert.assertTrue(widgetSource.contains("isDither = true"))
        org.junit.Assert.assertTrue(widgetSource.contains("scaleBitmapForWidget(remoteBitmap, 680, 0.94f)"))
        org.junit.Assert.assertTrue(compactSource.contains("scaleBitmapForWidget(remoteBitmap, 520, 0.94f)"))
    }
}
