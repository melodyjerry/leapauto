package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 修复「层峦叠嶂」原厂 3D 主题顶部状态栏大黑边，恢复全景天幕沉浸式铺满\n2. 优化 3D 摄像机垂直目标点，保持纯三维视角下车模平稳居中舒展姿态"
}
