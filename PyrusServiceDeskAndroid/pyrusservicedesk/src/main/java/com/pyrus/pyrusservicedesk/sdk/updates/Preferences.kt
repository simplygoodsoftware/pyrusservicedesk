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

    /**
     * Save the list of token registration times
     */
    fun setTokenRegisterTimeList(timeList: List<Long>)

    /**
     * @return the list of token registration times
     */
    fun getTokenRegisterTimeList(): List<Long>

    /**
     * Save the map of token registration time to user
     */
    fun setLastTokenRegisterMap(timeMap: Map<String, Long>)

    /**
     * @return the map of token registration time to user
     */
    fun getLastTokenRegisterMap(): Map<String, Long>

}