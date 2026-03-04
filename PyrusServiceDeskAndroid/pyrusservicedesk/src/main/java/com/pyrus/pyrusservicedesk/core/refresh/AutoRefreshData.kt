package com.pyrus.pyrusservicedesk.core.refresh

data class AutoRefreshData(
    val interval: Long,
    val lastActiveTime: Long,
    val sdIsOpen: Boolean,
    val updatesIsStarted: Boolean
)
