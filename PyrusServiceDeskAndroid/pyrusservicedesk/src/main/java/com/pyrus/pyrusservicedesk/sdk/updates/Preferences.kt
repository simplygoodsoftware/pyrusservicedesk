package com.pyrus.pyrusservicedesk.sdk.updates

internal interface Preferences {

    fun saveLastComment(comment: LastComment)

    fun getLastComment(): LastComment?

    fun removeLastComment()

    fun saveLastActiveTime(time: Long)

    fun getLastActiveTime(): Long

}