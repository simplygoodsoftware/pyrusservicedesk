package com.pyrus.pyrusservicedesk.sdk.updates

internal interface Preferences {

    /**
     * Save the last user's comment in shared preferences.
     */
    fun saveLastComment(comment: LastComment)

    /**
     * @return The LastComment instance from shared preferences.
     */
    fun getLastComment(): LastComment?

    /**
     * Remove the last user's comment from preferences.
     */
    fun removeLastComment()

    /**
     * Save the last user's active time.
     */
    fun saveLastActiveTime(time: Long)

    /**
     * @return the last user's active time.
     */
    fun getLastActiveTime(): Long

}