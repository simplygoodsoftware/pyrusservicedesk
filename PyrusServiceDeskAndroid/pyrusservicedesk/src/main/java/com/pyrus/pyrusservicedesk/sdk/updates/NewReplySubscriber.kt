package com.pyrus.pyrusservicedesk.sdk.updates

/**
 * Should be implemented to be subscribed on updates of new reply from support
 */
interface NewReplySubscriber {
    /**
     * Invoked on subscriber when there is a new reply from support is received
     * @param hasUnreadComments true if reply subscriber has some unread comments.
     */
    fun onNewReply(
        hasUnreadComments: Boolean,
        lastCommentText: String?,
        lastCommentAttachmentsCount: Int,
        lastCommentAttachments: List<String>?,
        id: Int,
        utcTime: Long
    )
}