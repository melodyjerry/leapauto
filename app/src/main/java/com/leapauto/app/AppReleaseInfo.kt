package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 修复能耗大卡内部布局高度约束，消除浅色模式下卡片底部大片死白\n2. 优化卡片黄金高度为 160dp 并实现指示器底边优雅贴合，视觉紧凑饱满"
}
