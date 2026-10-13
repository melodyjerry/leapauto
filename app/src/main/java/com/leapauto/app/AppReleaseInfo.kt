package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 扩展首卡展台高度至 225dp，优化视口纵向开阔度并消除多余留白\n2. 优化车身展台视觉层次，使得快捷操作栏与功能卡片自然舒展"
}
