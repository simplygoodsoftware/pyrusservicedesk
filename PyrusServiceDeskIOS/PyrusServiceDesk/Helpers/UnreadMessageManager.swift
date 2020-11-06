import Foundation
///The manager for storing last unread comment and passing info to subscriber
class UnreadMessageManager {
    private static let LAST_MESSAGE_KEY = "PSDLastUnreadMessage"
    private static func saveLastComment(_ message: PSDMessage) -> PSDLastUnreadMessage {
        let unreadMessage = PSDLastUnreadMessage(message: message)
        PSDMessagesStorage.pyrusUserDefaults()?.set(unreadMessage.toDictioanary(), forKey: LAST_MESSAGE_KEY)
        PSDMessagesStorage.pyrusUserDefaults()?.synchronize()
        return unreadMessage
    }
    private static func resaveLastComment(_ message: PSDLastUnreadMessage) {
        PSDMessagesStorage.pyrusUserDefaults()?.set(message.toDictioanary(), forKey: LAST_MESSAGE_KEY)
        PSDMessagesStorage.pyrusUserDefaults()?.synchronize()
    }
    static func removeLastComment() {
        PSDMessagesStorage.pyrusUserDefaults()?.removeObject(forKey: LAST_MESSAGE_KEY)
        PSDMessagesStorage.pyrusUserDefaults()?.synchronize()
    }
    private static func getLastComment() ->  PSDLastUnreadMessage? {
        guard let dict = PSDMessagesStorage.pyrusUserDefaults()?.object(forKey: LAST_MESSAGE_KEY) as? [String: Any] else {
            return nil
        }
        return PSDLastUnreadMessage(dictionary: dict)
    }
    ///check the las saved comment and send it to subscriber
    static func checkLastComment() {
        guard let _ = PyrusServiceDesk.subscriber, !PyrusServiceDeskController.PSDIsOpen(), let comment = getLastComment() else {
            return
        }
        checkLastComment(comment)
    }
    static func refreshNewMessagesCount(_ unread:Int, lastMessage: PSDMessage?){
        guard unread > 0 else{
            sendNoNewIfNeed()
            removeLastComment()
            return
        }
        guard !PyrusServiceDeskController.PSDIsOpen() else {
            return
        }
        let storedUnreadMessage = getLastComment()
        var needResponse = storedUnreadMessage == nil
        if !needResponse, let storedUnreadMessage = storedUnreadMessage, storedUnreadMessage.isShown{
            if let messageId = Int(storedUnreadMessage.messageId), messageId > 0{
                let lastMessageId = Int(storedUnreadMessage.messageId) ?? 0
                needResponse = messageId < lastMessageId
            }else{
                needResponse = true
            }
        }
        guard needResponse else {
            return
        }
        getLastCommentFromServer()
    }
    ///Sends to subscriber that there is no new messages in chat, if previus callback was with hasUnreadComments == true
    private static func sendNoNewIfNeed() {
        guard let subscriber = PyrusServiceDesk.subscriber, !PyrusServiceDeskController.PSDIsOpen(), let storedUnreadMessage = getLastComment(), storedUnreadMessage.isShown else {
            return
        }
        subscriber.onNewReply(hasUnreadComments: false, lastCommentText: nil, lastCommentAttachmentsCount: 0, lastCommentAttachments: nil, commetId: nil, utcTime: 0)
    }
    private static func getLastCommentFromServer() {
        PSDGetChat.get("", needShowError: false, delegate: nil, keepUnread: true, completion: {
            (chat : PSDChat?) in
            guard let lastMessage = chat?.messages.last else{
                return
            }
            DispatchQueue.main.async  {
                let lastComment = saveLastComment(lastMessage)
                checkLastComment(lastComment)
            }
        })
    }
    private static func checkLastComment(_ lastComment: PSDLastUnreadMessage) {
        guard let subscriber = PyrusServiceDesk.subscriber, !PyrusServiceDeskController.PSDIsOpen(), !lastComment.isShown else {
            return
        }
        subscriber.onNewReply(hasUnreadComments: true, lastCommentText: lastComment.text, lastCommentAttachmentsCount: lastComment.attchmentsCount, lastCommentAttachments: lastComment.attachments, commetId: lastComment.messageId, utcTime: lastComment.utcTime)
        lastComment.isShown = true
        resaveLastComment(lastComment)
    }
}
