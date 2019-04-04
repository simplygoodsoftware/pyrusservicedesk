package net.papirus.pyrusservicedesk

/**
 * Callback that is invoked when attempt of applying push token is completed.
 * Returning without an exception means that token was successfully applied.
 */
interface SetPushTokenCallback {
    /**
     * Callback that is invoked when result of applying push token is received.
     */
    fun onResult(exception: Exception?)
}
