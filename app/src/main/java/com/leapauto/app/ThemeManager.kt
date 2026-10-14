package com.leapauto.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object ThemeManager {
    private const val TAG = "ThemeManager"
    const val DEFAULT_THEME_ID = "4"

    // 官方 4 套预设主题内置兜底清单（弱网/离线即可呈现）
    val PRESET_THEMES: List<VehicleTheme> by lazy {
        listOf(
            VehicleTheme(
                themeId = "4",
                themeCode = "20260920001",
                name = "默认主题",
                description = "车模型默认 3D 主题背景",
                isDefault = true
            ),
            VehicleTheme(
                themeId = "7",
                themeCode = "20260920004",
                name = "雾川雪镜",
                description = "云雾缭绕的巍峨雪山与静谧冰河相映，宛如一幅空灵悠远的冬日水墨画卷。",
                tags = listOf("主题上新"),
                resourceBg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/7/resource_bg/bg.png?Expires=4943522631&OSSAccessKeyId=LTAI4Fmo2WXExVH9PecXNnpy&Signature=4JHJKx3YHDb8qQQOWIe%2Fm94wJck%3D",
                resourcePkg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/7/resource_pkg/resource%20-%20%E9%9B%BE%E5%B7%9D%E9%9B%AA%E5%A2%83.zip",
                resourcePkgName = "resource - 雾川雪境.zip",
                fileSize = 2199406L,
                fileMd5 = "e3a513f907c4df15b942d177dcf04547"
            ),
            VehicleTheme(
                themeId = "5",
                themeCode = "20260920002",
                name = "余晖石筑",
                description = "沐浴在柔和余晖下的现代石筑，于空旷原野中诉说静谧与沉稳的高级感。",
                tags = listOf("主题上新"),
                resourceBg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/5/resource_bg/bg.png?Expires=4943522277&OSSAccessKeyId=LTAI4Fmo2WXExVH9PecXNnpy&Signature=o3RFgGR0LOB9SDP2c6w2MQGe2PY%3D",
                resourcePkg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/5/resource_pkg/resource%20-%20%E4%BD%99%E6%99%96%E7%9F%B3%E7%AD%91.zip",
                resourcePkgName = "resource - 余晖石筑.zip",
                fileSize = 1415005L,
                fileMd5 = "acfe744b0d89a42b9bbd8a209c7ae3a6"
            ),
            VehicleTheme(
                themeId = "8",
                themeCode = "20260920005",
                name = "晴空广场",
                description = "湛蓝天空下的一片开阔空地，树木静静伫立，尽显极简纯粹的留白之美。",
                tags = listOf("主题上新"),
                resourceBg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/8/resource_bg/bg.png?Expires=4943521186&OSSAccessKeyId=LTAI4Fmo2WXExVH9PecXNnpy&Signature=oSHuFRtrtnneiQZcGAMlTL9XMs4%3D",
                resourcePkg = "http://lp-carnet.oss-cn-hangzhou.aliyuncs.com/leapmotor-app-theme-prod/8/resource_pkg/resource%20-%20%E6%99%B4%E7%A9%BA%E5%B9%BF%E5%9C%BA.zip",
                resourcePkgName = "resource - 晴空广场.zip",
                fileSize = 1495987L,
                fileMd5 = "ad42b2013c19f0741f6750906e950964"
            )
        )
    }

    fun getBaseDir(context: Context): File {
        val persistent = File(context.filesDir, "CarThemes").apply {
            if (!exists()) mkdirs()
        }
        // 自动从旧版 cacheDir 迁移已有主题文件到永久 filesDir，确保无缝平滑过渡
        val legacy = File(context.cacheDir, "CarThemes")
        if (legacy.exists() && legacy.isDirectory) {
            legacy.listFiles()?.forEach { item ->
                val dest = File(persistent, item.name)
                if (!dest.exists()) {
                    runCatching {
                        if (item.isDirectory) {
                            item.copyRecursively(dest, overwrite = true)
                        } else {
                            item.copyTo(dest, overwrite = true)
                        }
                        Log.i(TAG, "已成功自动迁移主题资源: ${item.name} -> filesDir")
                    }
                }
            }
        }
        return persistent
    }

    fun sanitizeKey(key: String): String {
        return key.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    }

    fun getThemeDir(baseDir: File, themeId: String): File {
        return File(baseDir, sanitizeKey(themeId))
    }

    fun getThemeDir(context: Context, themeId: String): File {
        return getThemeDir(getBaseDir(context), themeId)
    }

    fun isThemeReady(baseDir: File, themeId: String): Boolean {
        if (themeId == DEFAULT_THEME_ID || themeId.isBlank()) return true
        val dir = getThemeDir(baseDir, themeId)
        if (!File(dir, ".ready").exists()) return false
        val bgFile = findThemeFile(dir, "lpcar_pic_3d_bg.png")
        return bgFile != null && bgFile.exists() && bgFile.length() > 0L
    }

    fun isThemeReady(context: Context, themeId: String): Boolean {
        return isThemeReady(getBaseDir(context), themeId)
    }

    /** 智能兼容 ZIP 根目录或带有 resource/ 子目录的资源文件寻址（优先寻址解压后高清实景大图） */
    fun findThemeFile(themeDir: File, fileName: String): File? {
        val inSub = File(File(themeDir, "resource"), fileName)
        if (inSub.exists() && inSub.isFile && inSub.length() > 0L) return inSub
        val direct = File(themeDir, fileName)
        if (direct.exists() && direct.isFile && direct.length() > 0L) {
            // 根目录下若存在小于 300KB 的旧版缩略图，避免误读为 3D 展台实景大图
            if (fileName == "lpcar_pic_3d_bg.png" && direct.length() < 300_000L && inSub.exists()) {
                return inSub
            }
            return direct
        }
        return null
    }

    fun get3dBgFile(context: Context, themeId: String): File? {
        if (themeId == DEFAULT_THEME_ID || themeId.isBlank()) return null
        val dir = getThemeDir(context, themeId)
        return findThemeFile(dir, "lpcar_pic_3d_bg.png")
    }

    fun getWidget4x2BgFile(context: Context, themeId: String): File? {
        if (themeId == DEFAULT_THEME_ID || themeId.isBlank()) return null
        val dir = getThemeDir(context, themeId)
        return findThemeFile(dir, "lpcar_pic_widget_4x2.png")
    }

    fun getWidget2x2BgFile(context: Context, themeId: String): File? {
        if (themeId == DEFAULT_THEME_ID || themeId.isBlank()) return null
        val dir = getThemeDir(context, themeId)
        return findThemeFile(dir, "lpcar_pic_widget_2x2.png")
    }

    fun load3dBgBitmap(context: Context, themeId: String): Bitmap? {
        if (themeId == DEFAULT_THEME_ID || themeId.isBlank()) return null
        val file = get3dBgFile(context, themeId)
        if (file != null && file.exists() && file.length() > 0L) {
            return try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                Log.w(TAG, "加载 3D 展台主题背景图失败: $themeId", e)
                null
            }
        }
        return null
    }

    /** 安全清理历史误存的官方模糊遮罩图，确保纯净拉取高清 4K 实景大图 */
    fun cleanInvalidBlurredCache(context: Context) {
        val base = getBaseDir(context)
        base.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val rootBg = File(dir, "lpcar_pic_3d_bg.png")
                val inSubBg = File(File(dir, "resource"), "lpcar_pic_3d_bg.png")
                if (rootBg.exists() && rootBg.length() < 300_000L) {
                    rootBg.delete()
                    Log.i(TAG, "已清理旧版模糊 3D 展台图缓存: ${rootBg.absolutePath}")
                }
                val hasValidBg = (rootBg.exists() && rootBg.length() > 300_000L) ||
                                 (inSubBg.exists() && inSubBg.length() > 300_000L)
                if (!hasValidBg) {
                    File(dir, ".ready").delete()
                }
            }
        }
    }

    /** 快速同步 3D 展台高清大图包 */
    fun ensure3dBgDownloaded(context: Context, theme: VehicleTheme): Boolean {
        return syncThemePackage(context, null, theme)
    }

    enum class ThemeTone {
        LIGHT, // 亮色背景主题（如雾川雪镜 7、晴空广场 8），强制使用高对比度深色文字
        DARK,  // 暗色背景主题（如余晖石筑 5），强制使用纯白高对比度文字
        AUTO   // 默认主题（4），跟随系统原生深浅模式自适应
    }

    fun getThemeTone(themeId: String): ThemeTone {
        return when (themeId) {
            "7" -> ThemeTone.LIGHT // 雾川雪镜：雪山冰河高亮底
            "8" -> ThemeTone.LIGHT // 晴空广场：湛蓝天空留白高光底
            "5" -> ThemeTone.DARK  // 余晖石筑：暗色暮色岩石底
            else -> ThemeTone.AUTO // 默认主题跟随系统
        }
    }

    /** 判断在给定的主题与系统外观下，首卡展台是否为暗色背景（用于决定顶部数据区文字是偏白还是偏黑） */
    fun isHeroBackgroundDark(themeId: String, isSystemDark: Boolean): Boolean {
        return when (getThemeTone(themeId)) {
            ThemeTone.DARK -> true   // 余晖石筑：背景天生为深色暮色，无论系统深浅，展台皆为暗底
            ThemeTone.LIGHT -> false // 晴空/雪镜：背景天生为高亮白天，展台皆为亮底
            ThemeTone.AUTO -> isSystemDark // 默认主题：跟随系统深浅外观
        }
    }

    /** 是否需要在深色模式下为浅色高光主题（如晴空广场/雾川雪镜）施加微弱夜巡沉浸滤镜，避免夜晚强光刺眼 */
    fun shouldApplyNightMoodFilter(themeId: String, isSystemDark: Boolean): Boolean {
        return isSystemDark && getThemeTone(themeId) == ThemeTone.LIGHT
    }

    /** 按照 20dp 标准座舱 R 角与微晶描边对位图进行平滑抗锯齿物理裁剪，杜绝尖角直角 */
    fun createRoundedWidgetBitmap(src: Bitmap, cornerRadiusPx: Float, strokeColor: Int? = null): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rectF = android.graphics.RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())

        // 1. 绘制抗锯齿 R 角遮罩
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)

        // 2. 以 SRC_IN 模式合成背景图
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)

        // 3. 绘制外层 0.8dp 微晶质感切边描边
        if (strokeColor != null) {
            paint.xfermode = null
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = strokeColor
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        }
        return output
    }

    fun loadWidgetBgBitmap(context: Context, themeId: String, is4x2: Boolean, darkTheme: Boolean = false): Bitmap? {
        val file = if (is4x2) getWidget4x2BgFile(context, themeId) else getWidget2x2BgFile(context, themeId)
        if (file == null || !file.exists() || file.length() == 0L) return null
        return try {
            val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val refWidthDp = if (is4x2) 340f else 160f
            val cornerRadius = (20f * (raw.width / refWidthDp)).coerceIn(24f, 80f)
            val strokeColor = if (darkTheme) 0x4DFFFFFF.toInt() else 0x26000000
            val rounded = createRoundedWidgetBitmap(raw, cornerRadius, strokeColor)
            if (rounded !== raw) {
                raw.recycle()
            }
            rounded
        } catch (e: Exception) {
            Log.w(TAG, "加载小组件主题背景失败: $themeId", e)
            null
        }
    }

    /**
     * 自包含的安全 ZIP 解压方法
     */
    fun downloadAndUnzipPackage(pkgUrl: String, destDir: File, expectedMd5: String? = null): Boolean {
        if (pkgUrl.isBlank()) return false
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalFile
        val request = okhttp3.Request.Builder()
            .url(pkgUrl)
            .get()
            .build()
        val client = NetworkDebugController.httpClient()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val bytes = response.body?.bytes() ?: return@use false
                if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4B.toByte()) {
                    return@use false
                }
                if (!expectedMd5.isNullOrBlank()) {
                    val md5Digest = java.security.MessageDigest.getInstance("MD5")
                    val actualMd5 = md5Digest.digest(bytes).joinToString("") { "%02x".format(it) }
                    if (!actualMd5.equals(expectedMd5.trim(), ignoreCase = true)) {
                        Log.w(TAG, "MD5 校验不匹配: 期望=$expectedMd5, 实际=$actualMd5")
                        return@use false
                    }
                }
                java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        val targetFile = File(canonicalDest, entry.name).canonicalFile
                        if (!targetFile.path.startsWith(canonicalDest.path + File.separator)) {
                            throw java.io.IOException("Zip traversal rejected: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            java.io.FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                    }
                }
                true
            }
        }.getOrDefault(false)
    }

    /**
     * 同步并安全解压主题包。
     * [theme] 包含主题信息、下载 URL 与 MD5 校验码。
     */
    fun syncThemePackage(context: Context, api: LeapmotorApi?, theme: VehicleTheme): Boolean {
        val themeId = theme.themeId
        if (theme.isDefault || themeId == DEFAULT_THEME_ID) {
            return true
        }
        val targetDir = getThemeDir(context, themeId)
        if (isThemeReady(context, themeId)) {
            Log.d(TAG, "主题资源已就绪: id=$themeId, name=${theme.name}")
            return true
        }

        val pkgUrl = theme.resourcePkg
        if (pkgUrl.isNullOrBlank()) {
            return false
        }
        Log.i(TAG, "开始下载并解压高清实景主题包: id=$themeId, name=${theme.name}, url=$pkgUrl")
        val success = if (api != null) {
            api.downloadThemePackage(pkgUrl, targetDir, theme.fileMd5)
        } else {
            downloadAndUnzipPackage(pkgUrl, targetDir, theme.fileMd5)
        }
        if (success) {
            val bgFile = findThemeFile(targetDir, "lpcar_pic_3d_bg.png")
            if (bgFile != null && bgFile.exists() && bgFile.length() > 300_000L) {
                File(targetDir, ".ready").createNewFile()
                Log.i(TAG, "高清 3D 实景主题包部署就绪: id=$themeId, name=${theme.name} (${bgFile.length()} bytes)")
                return true
            }
        }
        Log.w(TAG, "主题包下载解压失败或缺少高清 3D 背景图: id=$themeId")
        return false
    }

    /** 获取车辆当前激活的主题 ID（优先读取本地自选设置，未设置时读取官方云端下发或默认） */
    fun getActiveThemeId(context: Context, vin: String, remoteTheme: VehicleTheme? = null): String {
        val store = SessionStore(context)
        val localSelected = store.loadSelectedThemeId(vin)
        val result = if (!localSelected.isNullOrBlank()) {
            localSelected
        } else {
            remoteTheme?.themeId?.takeIf { it.isNotBlank() } ?: DEFAULT_THEME_ID
        }
        Log.i(TAG, "getActiveThemeId: vin='$vin', localSelected='$localSelected' -> result='$result'")
        return result
    }

    /** 设置车辆当前主题，若设置为默认或 null 则恢复跟随默认/云端 */
    fun setActiveThemeId(context: Context, vin: String, themeId: String?) {
        val store = SessionStore(context)
        store.saveSelectedThemeId(vin, themeId)
    }

    /** 保存主题列表缓存到磁盘，供离线时读取 */
    fun saveThemeListCache(context: Context, themes: List<VehicleTheme>) {
        if (themes.isEmpty()) return
        try {
            val jsonArray = JSONArray()
            themes.forEach { t ->
                val obj = JSONObject().apply {
                    put("themeId", t.themeId)
                    put("themeCode", t.themeCode)
                    put("name", t.name)
                    put("description", t.description)
                    put("isDefault", t.isDefault)
                    val res = JSONObject().apply {
                        put("resourceBg", t.resourceBg)
                        put("resourcePkg", t.resourcePkg)
                        put("resourcePkgName", t.resourcePkgName)
                        put("fileSize", t.fileSize)
                        put("fileMd5", t.fileMd5)
                        val aff = JSONArray().apply {
                            put(JSONObject().apply {
                                put("name", "首页")
                                put("preview", t.homePreview)
                                put("thumbnail", t.homeThumbnail)
                            })
                            put(JSONObject().apply {
                                put("name", "小组件")
                                put("preview", t.widgetPreview)
                                put("thumbnail", t.widgetThumbnail)
                            })
                        }
                        put("affiliatedResource", aff)
                    }
                    put("resource", res)
                    if (t.tags.isNotEmpty()) {
                        put("tags", JSONArray(t.tags))
                    }
                }
                jsonArray.put(obj)
            }
            val cacheFile = File(getBaseDir(context), "theme_list.json")
            cacheFile.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "保存主题列表缓存失败", e)
        }
    }

    /** 从本地缓存读取主题列表，若无缓存返回预设兜底清单 */
    fun loadCachedThemeList(context: Context): List<VehicleTheme> {
        val cacheFile = File(getBaseDir(context), "theme_list.json")
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            try {
                val text = cacheFile.readText(Charsets.UTF_8)
                val jsonArray = JSONArray(text)
                val list = ArrayList<VehicleTheme>(jsonArray.length())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    list.add(VehicleTheme.fromJson(obj))
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.w(TAG, "解析主题列表缓存失败，使用预设兜底", e)
            }
        }
        return PRESET_THEMES
    }
}
