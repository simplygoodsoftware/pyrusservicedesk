package com.pyrus.pyrusservicedesk.sdk.updates

internal interface Preferences {

    fun saveLastActiveTime(time: Long)

    fun getLastActiveTime(): Long

}