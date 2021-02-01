package com.pyrus.pyrusservicedesk.sdk.updates

/**
 * Should be implemented to be subscribed on updates of new reply from support
 */
interface NewReplySubscriber {
    /**
     * Invoked on subscriber when there is a new reply from support is received.
     * @param hasUnreadComments True if reply subscriber has some unread comments.
     * @param lastCommentText Text of last unread comment. Max 500 chars.
     * @param lastCommentAttachmentsCount The number of attachments for the last unread comment.
     * @param lastCommentAttachments List of attachment names. Max 10 names.
     * @param utcTime The time the last unread comment was created in UTC milliseconds.
     */
    fun onNewReply(
        hasUnreadComments: Boolean,
        lastCommentText: String?,
        lastCommentAttachmentsCount: Int,
        lastCommentAttachments: List<String>?,
        utcTime: Long
    )
}