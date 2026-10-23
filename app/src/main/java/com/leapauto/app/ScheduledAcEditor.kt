package com.leapauto.app

fun ScheduledAcState.editorPlan(localPlan: ScheduledAcPlan): ScheduledAcPlan =
    if (hasSchedule) plan else localPlan.copy(enabled = false)
