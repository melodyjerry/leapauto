package com.leapauto.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundShadowStripperTest {

    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    private fun alphaOf(pixel: Int): Int = (pixel ushr 24) and 0xFF

    @Test
    fun `semi transparent pixels below the body are cleared`() {
        val width = 10
        val height = 100
        val pixels = IntArray(width * height)

        // 实心车身占据上半部分（alpha=255），其下方是半透明地面杂影（alpha=100）。
        for (y in 0 until 60) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(255, 200, 30, 30)
            }
        }
        for (y in 60 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(100, 20, 20, 20)
            }
        }

        val result = GroundShadowStripper.strip(pixels, width, height)

        assertNotEquals("expected the shadow band to be modified", pixels, result)
        assertEquals("body pixels must be preserved", 255, alphaOf(result[30 * width + 5]))
        assertEquals("shadow pixels below the body must be cleared", 0, alphaOf(result[70 * width + 5]))
    }

    @Test
    fun `opaque pixels below the body are preserved`() {
        val width = 10
        val height = 100
        val pixels = IntArray(width * height)

        for (y in 0 until 60) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(255, 200, 30, 30)
            }
        }
        // 实心轮胎底缘：位于车身轮廓最底部的实心像素必须保留。
        for (x in 0 until width) {
            pixels[60 * width + x] = argb(255, 10, 10, 10)
        }
        for (y in 61 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(120, 20, 20, 20)
            }
        }

        val result = GroundShadowStripper.strip(pixels, width, height)

        assertEquals("solid tire edge must be preserved", 255, alphaOf(result[60 * width + 5]))
        assertEquals("shadow under the tire must be cleared", 0, alphaOf(result[70 * width + 5]))
    }

    @Test
    fun `dark semi transparent pixels inside the body outline are preserved`() {
        val width = 10
        val height = 100
        val pixels = IntArray(width * height)

        // 车身主体位于阴影带之上。
        for (y in 0 until 65) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(255, 200, 30, 30)
            }
        }
        // 深色半透明车漆延伸进阴影带，但仍在轮胎/包围底缘之上，不得按颜色误删。
        for (y in 65 until 79) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(150, 12, 12, 12)
            }
        }
        // 真正的实心车身底缘。
        for (y in 79 until 86) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(255, 10, 10, 10)
            }
        }
        for (y in 86 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(100, 20, 20, 20)
            }
        }

        val result = GroundShadowStripper.strip(pixels, width, height)

        assertEquals("in-body dark translucent pixels inside the band must be preserved", 150, alphaOf(result[70 * width + 5]))
        assertEquals("shadow below the body must still be cleared", 0, alphaOf(result[92 * width + 5]))
    }

    @Test
    fun `columns without any solid structure are fully cleared in the ground band`() {
        val width = 10
        val height = 100
        val pixels = IntArray(width * height)

        // 只有中间两列存在车身，其余列仅有地面杂影。
        for (y in 0 until 60) {
            pixels[y * width + 4] = argb(255, 200, 30, 30)
            pixels[y * width + 5] = argb(255, 200, 30, 30)
        }
        for (y in 60 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = argb(90, 30, 30, 30)
            }
        }

        val result = GroundShadowStripper.strip(pixels, width, height)

        assertEquals("shadow outside the body must be cleared", 0, alphaOf(result[80 * width + 0]))
        assertEquals("shadow under the body must be cleared", 0, alphaOf(result[80 * width + 4]))
    }

    @Test
    fun `images without any shadow are returned unchanged`() {
        val width = 8
        val height = 40
        val pixels = IntArray(width * height)
        for (index in pixels.indices) {
            pixels[index] = argb(255, 10, 120, 240)
        }

        val result = GroundShadowStripper.strip(pixels, width, height)

        assertSame("a fully opaque image must not be copied", pixels, result)
    }

    @Test
    fun `the ground band starts below the vehicle body`() {
        assertTrue(GroundShadowStripper.GROUND_BAND_RATIO > 0.5f)
        assertTrue(GroundShadowStripper.GROUND_BAND_RATIO < 1f)
        assertTrue(GroundShadowStripper.OPAQUE_ALPHA in 1..255)
    }
}
