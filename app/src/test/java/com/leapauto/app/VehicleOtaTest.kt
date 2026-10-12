package com.leapauto.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleOtaTest {

    @Test
    fun `parse up to date ota response`() {
        val json = JSONObject("""
            {
                "currentVersion": "Leapmotor OS 1.3.20",
                "hasNewVersion": false,
                "status": 0
            }
        """.trimIndent())
        val info = VehicleOtaInfo.fromJson(json)
        assertEquals("Leapmotor OS 1.3.20", info.currentVersion)
        assertFalse(info.hasNewVersion)
        assertEquals(OtaStatus.UP_TO_DATE, info.status)
        assertTrue(info.isUpToDate)
    }

    @Test
    fun `parse real official fota getCurrentVersion response`() {
        val json = JSONObject("""
            {
                "vin": "TESTVIN0000000001",
                "versionNo": "3.06.40",
                "logContent": "本次OTA新增功能：\n1.城市领航辅助\n2.一键泊车",
                "updateTime": "2026.05.24"
            }
        """.trimIndent())
        val info = VehicleOtaInfo.fromJson(json)
        assertEquals("3.06.40", info.currentVersion)
        assertEquals("本次OTA新增功能：\n1.城市领航辅助\n2.一键泊车", info.releaseNotes)
        assertEquals("2026.05.24", info.updateTime)
        assertFalse(info.hasNewVersion)
        assertEquals(OtaStatus.UP_TO_DATE, info.status)
        assertTrue(info.isUpToDate)
    }

    @Test
    fun `parse update available ota response with release notes`() {
        val json = JSONObject("""
            {
                "curVersion": "Leapmotor OS 1.3.20",
                "newVersion": "Leapmotor OS 1.4.00",
                "hasUpdate": true,
                "releaseNotes": "1. 优化能耗算法\n2. 新增哨兵模式低功耗",
                "taskId": "802914",
                "packageSize": 2000000000,
                "status": 1
            }
        """.trimIndent())
        val info = VehicleOtaInfo.fromJson(json)
        assertEquals("Leapmotor OS 1.3.20", info.currentVersion)
        assertEquals("Leapmotor OS 1.4.00", info.newVersion)
        assertTrue(info.hasNewVersion)
        assertEquals("802914", info.taskId)
        assertEquals("1.86 GB", info.packageSize)
        assertEquals(OtaStatus.UPDATE_AVAILABLE, info.status)
        assertFalse(info.isUpToDate)
    }

    @Test
    fun `parse downloading and downloaded ota states`() {
        val downloadingJson = JSONObject("""
            {
                "version": "1.3.20",
                "targetVersion": "1.4.00",
                "status": 2,
                "progress": 45
            }
        """.trimIndent())
        val downloading = VehicleOtaInfo.fromJson(downloadingJson)
        assertEquals(OtaStatus.DOWNLOADING, downloading.status)
        assertTrue(downloading.isDownloading)
        assertEquals(45, downloading.progressPercent)

        val downloadedJson = JSONObject("""
            {
                "version": "1.3.20",
                "targetVersion": "1.4.00",
                "status": 3,
                "progress": 100
            }
        """.trimIndent())
        val downloaded = VehicleOtaInfo.fromJson(downloadedJson)
        assertEquals(OtaStatus.DOWNLOADED, downloaded.status)
        assertTrue(downloaded.isDownloaded)
    }

    @Test
    fun `commands build generates valid FOTA control payloads`() {
        val download = Commands.build("fotaDownload:802914")
        assertEquals("390", download.cmdid)
        assertEquals("""{"taskId":"802914"}""", download.stateJson)
        assertEquals("下载固件", download.label)

        val install = Commands.build("fotaInstall:802914")
        assertEquals("391", install.cmdid)
        assertEquals("""{"taskId":"802914"}""", install.stateJson)
        assertEquals("安装固件", install.label)

        val schedule = Commands.build("fotaSchedule:802914:02:00:00")
        assertEquals("392", schedule.cmdid)
        assertEquals("""{"taskId":"802914","scheduleTime":"02:00:00"}""", schedule.stateJson)
        assertEquals("预约安装固件", schedule.label)
    }

    @Test
    fun `control feedback policy provides clear messages for fota commands`() {
        assertEquals("正在启动固件下载...", ControlFeedbackFormatter.inProgress("fotaDownload:802914", null))
        assertEquals("正在发送固件升级指令...", ControlFeedbackFormatter.inProgress("fotaInstall:802914", null))
        assertEquals("正在提交定时升级预约...", ControlFeedbackFormatter.inProgress("fotaSchedule:802914:02:00:00", null))

        assertEquals("固件下载已启动", ControlFeedbackFormatter.success("fotaDownload:802914", null))
        assertEquals("整车升级指令已发送", ControlFeedbackFormatter.success("fotaInstall:802914", null))
        assertEquals("定时升级已预约成功", ControlFeedbackFormatter.success("fotaSchedule:802914:02:00:00", null))
    }

    @Test
    fun `sub account ota policy correctly rejects sub account and allows owner account`() {
        assertFalse(VehicleOtaPolicy.canOperateOta(isSharedAccount = true))
        assertTrue(VehicleOtaPolicy.canOperateOta(isSharedAccount = false))

        assertEquals("当前账号为授权子账号，车机 OTA 更新需车主主账号操作", VehicleOtaPolicy.SUB_ACCOUNT_OTA_UNSUPPORTED_MESSAGE)
        assertEquals("子账号无车机 OTA 更新权限", VehicleOtaPolicy.SUB_ACCOUNT_OTA_HINT_TITLE)
        assertTrue(VehicleOtaPolicy.SUB_ACCOUNT_OTA_HINT_DESC.contains("车主主账号可用"))
        assertTrue(VehicleOtaPolicy.SUB_ACCOUNT_OTA_HINT_DESC.contains("请使用车主手机号登录"))
    }

    @Test
    fun `simulate full ota lifecycle from update discovery to download scheduling and successful installation`() {
        val taskId = "904128"
        val oldVer = "Leapmotor OS 3.06.40"
        val newVer = "Leapmotor OS 3.08.00"

        // === 阶段 1: 发现新固件推送 (UPDATE_AVAILABLE) ===
        val discoveryResponse = JSONObject("""
            {
                "curVersion": "$oldVer",
                "newVersion": "$newVer",
                "hasUpdate": true,
                "releaseNotes": "1. 优化高快领航辅助驾驶\n2. 优化空调制冷能耗表现\n3. 新增手机钥匙离车自动闭锁",
                "taskId": "$taskId",
                "packageSize": 2500000000,
                "status": 1
            }
        """.trimIndent())
        val discoveryInfo = VehicleOtaInfo.fromJson(discoveryResponse)
        assertEquals(OtaStatus.UPDATE_AVAILABLE, discoveryInfo.status)
        assertEquals(oldVer, discoveryInfo.currentVersion)
        assertEquals(newVer, discoveryInfo.newVersion)
        assertTrue(discoveryInfo.hasNewVersion)
        assertFalse(discoveryInfo.isUpToDate)
        assertEquals(taskId, discoveryInfo.taskId)
        assertEquals("2.33 GB", discoveryInfo.packageSize)

        // === 阶段 2: 车主触发远程下载固件 (fotaDownload) ===
        val downloadCmd = Commands.build("fotaDownload:$taskId")
        assertEquals("390", downloadCmd.cmdid)
        assertEquals("""{"taskId":"$taskId"}""", downloadCmd.stateJson)
        assertEquals("正在启动固件下载...", ControlFeedbackFormatter.inProgress("fotaDownload:$taskId", downloadCmd.label))
        assertEquals("固件下载已启动", ControlFeedbackFormatter.success("fotaDownload:$taskId", downloadCmd.label))

        // === 阶段 3: 固件后台静默下载中 (DOWNLOADING 进度推进) ===
        val progressList = listOf(20, 50, 85)
        progressList.forEach { p ->
            val downloadingResponse = JSONObject("""
                {
                    "version": "$oldVer",
                    "targetVersion": "$newVer",
                    "taskId": "$taskId",
                    "status": 2,
                    "progress": $p
                }
            """.trimIndent())
            val progressInfo = VehicleOtaInfo.fromJson(downloadingResponse)
            assertEquals(OtaStatus.DOWNLOADING, progressInfo.status)
            assertTrue(progressInfo.isDownloading)
            assertFalse(progressInfo.isDownloaded)
            assertEquals(p, progressInfo.progressPercent)
        }

        // === 阶段 4: 固件下载完成且校验就绪 (DOWNLOADED) ===
        val downloadedResponse = JSONObject("""
            {
                "version": "$oldVer",
                "targetVersion": "$newVer",
                "taskId": "$taskId",
                "status": 3,
                "progress": 100
            }
        """.trimIndent())
        val readyInfo = VehicleOtaInfo.fromJson(downloadedResponse)
        assertEquals(OtaStatus.DOWNLOADED, readyInfo.status)
        assertTrue(readyInfo.isDownloaded)
        assertFalse(readyInfo.isDownloading)

        // === 阶段 5: 车主下发夜间凌晨定时预约安装 (fotaSchedule) ===
        val scheduleTime = "02:00:00"
        val scheduleCmd = Commands.build("fotaSchedule:$taskId:$scheduleTime")
        assertEquals("392", scheduleCmd.cmdid)
        assertEquals("""{"taskId":"$taskId","scheduleTime":"$scheduleTime"}""", scheduleCmd.stateJson)
        assertEquals("正在提交定时升级预约...", ControlFeedbackFormatter.inProgress("fotaSchedule:$taskId:$scheduleTime", scheduleCmd.label))
        assertEquals("定时升级已预约成功", ControlFeedbackFormatter.success("fotaSchedule:$taskId:$scheduleTime", scheduleCmd.label))

        val scheduledResponse = JSONObject("""
            {
                "version": "$oldVer",
                "targetVersion": "$newVer",
                "taskId": "$taskId",
                "status": 3,
                "scheduledTime": "凌晨 02:00"
            }
        """.trimIndent())
        val scheduledInfo = VehicleOtaInfo.fromJson(scheduledResponse)
        assertTrue(scheduledInfo.isScheduled)
        assertEquals("凌晨 02:00", scheduledInfo.scheduledTime)

        // === 阶段 6: 到达升级时间或车主即刻触发安装升级 (fotaInstall) ===
        val installCmd = Commands.build("fotaInstall:$taskId")
        assertEquals("391", installCmd.cmdid)
        assertEquals("""{"taskId":"$taskId"}""", installCmd.stateJson)
        assertEquals("正在发送固件升级指令...", ControlFeedbackFormatter.inProgress("fotaInstall:$taskId", installCmd.label))
        assertEquals("整车升级指令已发送", ControlFeedbackFormatter.success("fotaInstall:$taskId", installCmd.label))

        // 车机进入刷写升级中
        val installingResponse = JSONObject("""
            {
                "version": "$oldVer",
                "targetVersion": "$newVer",
                "taskId": "$taskId",
                "status": 4
            }
        """.trimIndent())
        val installingInfo = VehicleOtaInfo.fromJson(installingResponse)
        assertEquals(OtaStatus.INSTALLING, installingInfo.status)
        assertTrue(installingInfo.isInstalling)

        // === 阶段 7: 车机升级完成重启，上报最新版本 (UP_TO_DATE) ===
        val completeResponse = JSONObject("""
            {
                "currentVersion": "$newVer",
                "hasNewVersion": false,
                "status": 0,
                "updateTime": "2026.10.12"
            }
        """.trimIndent())
        val finalInfo = VehicleOtaInfo.fromJson(completeResponse)
        assertEquals(OtaStatus.UP_TO_DATE, finalInfo.status)
        assertEquals(newVer, finalInfo.currentVersion)
        assertFalse(finalInfo.hasNewVersion)
        assertTrue(finalInfo.isUpToDate)
        assertEquals("2026.10.12", finalInfo.updateTime)
    }

    @Test
    fun `simulate ota installation failure and reset status workflow`() {
        val taskId = "904128"
        val failedResponse = JSONObject("""
            {
                "version": "Leapmotor OS 3.06.40",
                "taskId": "$taskId",
                "status": 6,
                "msg": "整车升级条件不满足：小电瓶电量不足"
            }
        """.trimIndent())
        val failedInfo = VehicleOtaInfo.fromJson(failedResponse)
        assertEquals(OtaStatus.FAILED, failedInfo.status)
        assertFalse(failedInfo.isUpToDate)
        assertFalse(failedInfo.isInstalling)
    }

    @Test
    fun `package size formatting correctly handles bytes megabytes and gigabytes`() {
        val mbJson = JSONObject("""{"packageSize": 450000000}""")
        assertEquals("429.2 MB", VehicleOtaInfo.fromJson(mbJson).packageSize)

        val gbJson = JSONObject("""{"packageSize": 3500000000}""")
        assertEquals("3.26 GB", VehicleOtaInfo.fromJson(gbJson).packageSize)

        val emptyJson = JSONObject()
        assertEquals("", VehicleOtaInfo.fromJson(emptyJson).packageSize)
    }

    @Test
    fun `empty and null json safely fallbacks to up to date default state`() {
        val nullInfo = VehicleOtaInfo.fromJson(null, msg = "网络正常")
        assertEquals("最新系统", nullInfo.currentVersion)
        assertEquals(OtaStatus.UP_TO_DATE, nullInfo.status)
        assertTrue(nullInfo.isUpToDate)
        assertFalse(nullInfo.hasNewVersion)
        assertEquals("网络正常", nullInfo.releaseNotes)
    }
}
