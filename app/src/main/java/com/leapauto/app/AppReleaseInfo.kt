package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 恢复车机系统 OTA 更新卡片与全套远控调度逻辑\n2. 支持查询车机固件版本、新固件日志、远程下载与夜间定时预约升级"
}
