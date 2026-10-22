package com.leapauto.app

object AppReleaseInfo {
    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    // Update this text together with apkVersionName before each delivery package.
    const val currentReleaseNotes = "1. 快捷操作新增「电池预热」控制开关，支持双向状态感应与一键开闭\n2. 增加电池预热温控与低电量智能安全门禁提示，避免常温或低电量下无效操作\n3. 彻底消除 2D 车图车底半透明脏阴影，实景展台纯净呈现\n4. 2D 车图校准官方 2:1 黄金比例全屏自适应，主题切换新增一键回首页弹窗"
}
