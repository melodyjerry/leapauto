package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 优化能耗里程卡片自适应算法，设置 180~205dp 黄金比例高度门禁\n2. 重构指标竖卡内部布局，改为居中凝聚排列，彻底消除长条空旷感"
}
