package net.papirus.pyrusservicedesk.sdk.updates

/**
 * Should be implemented to be subscribed on updates of new reply from support
 */
interface OnNewReplySubscriber {
    /**
     * Invoked on subscriber when there is a new reply from support is received
     */
    fun onNewReply()
}