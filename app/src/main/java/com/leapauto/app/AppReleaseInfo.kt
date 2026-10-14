package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 同步更新官方四套主题，修复一些bug和优化UI显示效果；如遇到显示相关的bug建议卸载重装"
}
