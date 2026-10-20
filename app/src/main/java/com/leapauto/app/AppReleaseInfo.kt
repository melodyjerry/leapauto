package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 全工程深度清理 19 处历史遗留死代码与废弃函数，精炼代码体量\n2. 彻底优化小组件 14 处重复刷新调用，消除运行时二次跨进程通知与大图解码开销\n3. 清理废弃未引用的 Drawable 资源与色值，全面消除编译静态检查警告"
}
