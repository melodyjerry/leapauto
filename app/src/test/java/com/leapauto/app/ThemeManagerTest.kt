package com.leapauto.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThemeManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `sanitizeKey sanitizes special characters`() {
        assertEquals("theme_7", ThemeManager.sanitizeKey("theme-7"))
        assertEquals("20260920004", ThemeManager.sanitizeKey("20260920004"))
        assertEquals("custom_v1_0", ThemeManager.sanitizeKey("custom/v1.0"))
    }

    @Test
    fun `preset themes list contains 4 official fallback themes`() {
        val themes = ThemeManager.PRESET_THEMES
        assertEquals(4, themes.size)
        val defaultTheme = themes.first()
        assertEquals(ThemeManager.DEFAULT_THEME_ID, defaultTheme.themeId)
        assertTrue(defaultTheme.isDefault)
        assertEquals("默认主题", defaultTheme.name)

        val wuchuan = themes.find { it.themeId == "7" }
        assertNotNull(wuchuan)
        assertEquals("雾川雪镜", wuchuan?.name)
        assertEquals("e3a513f907c4df15b942d177dcf04547", wuchuan?.fileMd5)

        val yuhui = themes.find { it.themeId == "5" }
        assertNotNull(yuhui)
        assertEquals("余晖石筑", yuhui?.name)

        val qingkong = themes.find { it.themeId == "8" }
        assertNotNull(qingkong)
        assertEquals("晴空广场", qingkong?.name)
    }

    @Test
    fun `findThemeFile resolves files directly or in resource subfolder`() {
        val themeDir = tempFolder.newFolder("theme_test")
        assertNull(ThemeManager.findThemeFile(themeDir, "lpcar_pic_3d_bg.png"))

        // Case 1: In resource subfolder
        val resDir = File(themeDir, "resource").apply { mkdirs() }
        val subFile = File(resDir, "lpcar_pic_3d_bg.png").apply { writeText("image_data") }
        val foundSub = ThemeManager.findThemeFile(themeDir, "lpcar_pic_3d_bg.png")
        assertNotNull(foundSub)
        assertEquals(subFile.absolutePath, foundSub?.absolutePath)

        // Case 2: Directly in root folder
        val directFile = File(themeDir, "direct_bg.png").apply { writeText("direct_data") }
        val foundDirect = ThemeManager.findThemeFile(themeDir, "direct_bg.png")
        assertNotNull(foundDirect)
        assertEquals(directFile.absolutePath, foundDirect?.absolutePath)

        // Case 3: Priority - resource subfolder takes precedence over small root file
        val rootSmall = File(themeDir, "lpcar_pic_3d_bg.png").apply { writeText("small_blurred_thumb") }
        val foundPreferSub = ThemeManager.findThemeFile(themeDir, "lpcar_pic_3d_bg.png")
        assertNotNull(foundPreferSub)
        assertEquals(subFile.absolutePath, foundPreferSub?.absolutePath)
    }

    @Test
    fun `isThemeReady returns true for default theme and checks ready marker for custom themes`() {
        val baseDir = tempFolder.newFolder("CarThemes")
        // Default theme is always ready
        assertTrue(ThemeManager.isThemeReady(baseDir, ThemeManager.DEFAULT_THEME_ID))

        val customDir = File(baseDir, "7")
        customDir.mkdirs()
        // Not ready without .ready marker
        val bgFile = File(customDir, "resource/lpcar_pic_3d_bg.png").apply {
            parentFile?.mkdirs()
            writeText("dummy_bytes")
        }
        assertFalse(ThemeManager.isThemeReady(baseDir, "7"))

        File(customDir, ".ready").createNewFile()
        assertTrue(ThemeManager.isThemeReady(baseDir, "7"))
    }

    @Test
    fun `vehicle theme fromJson correctly parses real packet data`() {
        val jsonStr = """
        {
          "themeId": "7",
          "themeCode": "20260920004",
          "name": "雾川雪镜",
          "description": "云雾缭绕的巍峨雪山与静谧冰河相映，宛如一幅空灵悠远的冬日水墨画卷。",
          "tags": ["主题上新"],
          "resource": {
            "affiliatedResourceType": 0,
            "resourceBg": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/bg.png",
            "resourceBgName": "bg.png",
            "affiliatedResource": [
              {
                "name": "首页",
                "preview": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/home_preview.png",
                "thumbnail": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/home_thumb.png"
              },
              {
                "name": "小组件",
                "preview": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/widget_preview.png",
                "thumbnail": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/widget_thumb.png"
              }
            ],
            "resourcePkg": "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/pkg.zip",
            "resourcePkgName": "resource - 雾川雪境.zip",
            "fileSize": 2199406,
            "fileMd5": "e3a513f907c4df15b942d177dcf04547"
          },
          "status": "ONLINE",
          "isDefault": false
        }
        """.trimIndent()

        val theme = VehicleTheme.fromJson(JSONObject(jsonStr))
        assertEquals("7", theme.themeId)
        assertEquals("20260920004", theme.themeCode)
        assertEquals("雾川雪镜", theme.name)
        assertFalse(theme.isDefault)
        assertEquals(listOf("主题上新"), theme.tags)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/bg.png", theme.resourceBg)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/pkg.zip", theme.resourcePkg)
        assertEquals("resource - 雾川雪境.zip", theme.resourcePkgName)
        assertEquals(2199406L, theme.fileSize)
        assertEquals("e3a513f907c4df15b942d177dcf04547", theme.fileMd5)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/home_preview.png", theme.homePreview)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/home_thumb.png", theme.homeThumbnail)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/widget_preview.png", theme.widgetPreview)
        assertEquals("http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/theme/7/widget_thumb.png", theme.widgetThumbnail)
    }

    @Test
    fun `vehicle picture meta extracts h5Whole srcWhole and currentTheme`() {
        val jsonStr = """
        {
          "h5Key": "3D-a6d2ca11-8388-4371-a308-72b2d86482e4",
          "srcKey": "3D-d3e0fbce-0755-441e-8381-7512fd49bc70",
          "h5Whole": "BG5gS2SQwHKC/5v2yDk/Ew==",
          "srcWhole": "pqxd6vFtMpktN6IfOpw9wA==",
          "shareBindUrl": "https://cdn.leapmotor.com/car.png",
          "currentTheme": {
            "themeId": "7",
            "themeCode": "20260920004",
            "name": "雾川雪镜",
            "description": "雪山冰河",
            "isDefault": false
          }
        }
        """.trimIndent()

        val meta = VehiclePictureMeta(
            pictureKey = "key_test",
            shareBindUrl = "https://cdn.leapmotor.com/car.png",
            rawData = JSONObject(jsonStr)
        )

        assertEquals("3D-a6d2ca11-8388-4371-a308-72b2d86482e4", meta.h5Key)
        assertEquals("3D-d3e0fbce-0755-441e-8381-7512fd49bc70", meta.srcKey)
        assertEquals("BG5gS2SQwHKC/5v2yDk/Ew==", meta.h5Whole)
        assertEquals("pqxd6vFtMpktN6IfOpw9wA==", meta.srcWhole)
        assertNotNull(meta.currentTheme)
        assertEquals("7", meta.currentTheme?.themeId)
        assertEquals("雾川雪镜", meta.currentTheme?.name)
    }

    @Test
    fun `theme tone correctly classifies light and dark themes`() {
        assertEquals(ThemeManager.ThemeTone.LIGHT, ThemeManager.getThemeTone("7"))
        assertEquals(ThemeManager.ThemeTone.LIGHT, ThemeManager.getThemeTone("8"))
        assertEquals(ThemeManager.ThemeTone.DARK, ThemeManager.getThemeTone("5"))
        assertEquals(ThemeManager.ThemeTone.AUTO, ThemeManager.getThemeTone("4"))
        assertEquals(ThemeManager.ThemeTone.AUTO, ThemeManager.getThemeTone(""))
    }

    @Test
    fun `isHeroBackgroundDark correctly determines background luminosity for all themes`() {
        // 余晖石筑（5）：无论系统是深色还是浅色，展台始终为暗色暮色背景
        assertTrue(ThemeManager.isHeroBackgroundDark("5", isSystemDark = true))
        assertTrue(ThemeManager.isHeroBackgroundDark("5", isSystemDark = false))

        // 雾川雪镜（7）与晴空广场（8）：背景始终为高亮浅色
        assertFalse(ThemeManager.isHeroBackgroundDark("7", isSystemDark = true))
        assertFalse(ThemeManager.isHeroBackgroundDark("7", isSystemDark = false))
        assertFalse(ThemeManager.isHeroBackgroundDark("8", isSystemDark = true))
        assertFalse(ThemeManager.isHeroBackgroundDark("8", isSystemDark = false))

        // 默认主题（4）或空：跟随系统
        assertTrue(ThemeManager.isHeroBackgroundDark("4", isSystemDark = true))
        assertFalse(ThemeManager.isHeroBackgroundDark("4", isSystemDark = false))
        assertTrue(ThemeManager.isHeroBackgroundDark("", isSystemDark = true))
        assertFalse(ThemeManager.isHeroBackgroundDark("", isSystemDark = false))
    }

    @Test
    fun `shouldApplyNightMoodFilter applies only to light themes in dark mode`() {
        // 在深色模式下，浅色主题（7、8）需要夜巡微暗滤镜
        assertTrue(ThemeManager.shouldApplyNightMoodFilter("7", isSystemDark = true))
        assertTrue(ThemeManager.shouldApplyNightMoodFilter("8", isSystemDark = true))

        // 在浅色模式下，不应叠加夜巡微暗滤镜
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("7", isSystemDark = false))
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("8", isSystemDark = false))

        // 暗色主题（5）和默认主题无论何时都不施加夜巡滤镜
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("5", isSystemDark = true))
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("5", isSystemDark = false))
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("4", isSystemDark = true))
        assertFalse(ThemeManager.shouldApplyNightMoodFilter("4", isSystemDark = false))
    }
}
