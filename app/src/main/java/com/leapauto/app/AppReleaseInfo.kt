package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 升级 4x2 与 2x2 桌面小组件车模高清渲染，大幅提升分辨率与细节精致度\n2. 注入双线性滤波抗锯齿与色彩抖动平滑渲染，车身比例舒展饱满"
}
