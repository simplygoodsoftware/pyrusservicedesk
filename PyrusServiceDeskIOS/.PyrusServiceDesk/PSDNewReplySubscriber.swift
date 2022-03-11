import Foundation
///The protocol for sending a notification that a new message has arrived
@objc public protocol NewReplySubscriber{
    ///The new message was send
    ///- parameter hasUnreadComments: Is there a new messages in chat
    ///- parameter lastCommentText: The text of last unread message.  Returns nil if there is no new messages or text in it.
    ///- parameter lastCommentAttachmentsCount: Total number of attachments in the message.  Returns 0 if there is no new messages or attachments in it.
    ///- parameter lastCommentAttachments: The list of attachments' names. Returns nil if there is no new messages, or atttachments in it.
    ///- parameter utcTime: The date of last unread message in(utc). Returns 0 if there is no new messages.
    @objc func onNewReply(
        hasUnreadComments: Bool,
        lastCommentText: String?,
        lastCommentAttachmentsCount: Int,
        lastCommentAttachments: [String]?,
        utcTime: Double
    )
}
