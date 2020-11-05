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
    static func refreshNewMessagesCount(_ unread:Int){
        guard unread > 0 else{
            removeLastComment()
            return
        }
        guard let _ = PyrusServiceDesk.subscriber, !PyrusServiceDeskController.PSDIsOpen() else {
            return
        }
        let storedUnreadMessage = getLastComment()
        var needResponse = storedUnreadMessage == nil
        if !needResponse, let storedUnreadMessage = storedUnreadMessage, !storedUnreadMessage.isShown{
            needResponse = true
        }
        guard needResponse else {
            return
        }
        getLastCommentFromServer()
    }
    private static func getLastCommentFromServer() {
        PSDGetChat.get("", needShowError: false, delegate: nil, completion: {
            (chat : PSDChat?) in
            guard let lastMessage = chat?.messages.last else{
                return
            }
            ///test нужно проверять на isRead?
            let lastComment = saveLastComment(lastMessage)
            guard let subscriber = PyrusServiceDesk.subscriber, !PyrusServiceDeskController.PSDIsOpen() else {
                return
            }
            subscriber.onNewReply(hasUnreadComments: true, lastCommentText: lastComment.text, lastCommentAttachmentsCount: lastComment.attchmentsCount, lastCommentAttachments: lastComment.attachments, commetId: lastComment.messageId, utcTime: lastComment.utcTime)
        })
        //subscriber.onNewReply(hasUnreadComments: unread>0)
    }
}
