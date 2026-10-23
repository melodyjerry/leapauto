package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 打开定时空调时读取并回显服务器预约设置\n2. 增加预约加载、未预约及读取失败重试状态\n3. 加载期间暂停编辑，避免服务器回读覆盖正在修改的设置\n4. 快速除味采用通风图标，与定时空调统一大小和线条粗细\n5. 优化空调场景提醒，避免舒适温度误报高温，按场景配色并弱化提醒字号和字重"
}
