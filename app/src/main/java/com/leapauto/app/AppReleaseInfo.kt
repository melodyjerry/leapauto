package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 优化充电中心数据回显不及时问题\n2. 车辆系统OTA检测、预约安装等（不一定有用，可测试）"
}
