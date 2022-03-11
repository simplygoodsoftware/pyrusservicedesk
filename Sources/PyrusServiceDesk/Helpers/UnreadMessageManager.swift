import Foundation
///The manager for storing last unread comment and passing info to subscriber
class UnreadMessageManager {
    private static let LAST_MESSAGE_KEY = "PSDLastUnreadMessage"
    private static func saveLastComment(_ message: PSDMessage) -> PSDLastUnreadMessage {
        let unreadMessage = PSDLastUnreadMessage(message: message)
        updateLastComment(unreadMessage)
        return unreadMessage
    }
    private static func updateLastComment(_ message: PSDLastUnreadMessage) {
        let encoder = JSONEncoder()
        guard let data = try? encoder.encode(message) else {
            return
        }
        PSDMessagesStorage.pyrusUserDefaults()?.set(data, forKey: LAST_MESSAGE_KEY)
    }
    static func removeLastComment() {
        PSDMessagesStorage.pyrusUserDefaults()?.removeObject(forKey: LAST_MESSAGE_KEY)
    }
    private static func getLastComment() ->  PSDLastUnreadMessage? {
        let decoder = JSONDecoder()
        guard let messageData = PSDMessagesStorage.pyrusUserDefaults()?.object(forKey: LAST_MESSAGE_KEY) as? Data,
              let message = try? decoder.decode(PSDLastUnreadMessage.self, from: messageData) else {
            return nil
        }
        return message
    }
    ///Check the last saved comment and send it to subscriber
    static func checkLastComment() {
        guard let _ = PyrusServiceDesk.subscriber,
              !PyrusServiceDeskController.PSDIsOpen(),
              let comment = getLastComment() else {
            return
        }
        checkLastComment(comment)
    }
    static func refreshNewMessagesCount(_ unread: Bool, lastMessage: PSDMessage?) {
        guard unread else{
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
            if let messageId = Int(storedUnreadMessage.messageId), messageId > 0 {
                let lastMessageId = Int(lastMessage?.messageId ?? "") ?? 0
                needResponse = messageId < lastMessageId
            } else {
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
        guard let subscriber = PyrusServiceDesk.subscriber,
              !PyrusServiceDeskController.PSDIsOpen(),
              let storedUnreadMessage = getLastComment(),
              storedUnreadMessage.isShown else {
            return
        }
        subscriber.onNewReply(hasUnreadComments: false, lastCommentText: nil, lastCommentAttachmentsCount: 0, lastCommentAttachments: nil, utcTime: 0)
    }
    private static func getLastCommentFromServer() {
        PSDGetChat.get(needShowError: false, delegate: nil, keepUnread: true, completion: {
            (chat : PSDChat?) in
            guard let lastMessage = chat?.messages.last else {
                return
            }
            DispatchQueue.main.async  {
                let lastComment = saveLastComment(lastMessage)
                checkLastComment(lastComment)
            }
        })
    }
    private static func checkLastComment(_ lastComment: PSDLastUnreadMessage) {
        guard let subscriber = PyrusServiceDesk.subscriber,
              !PyrusServiceDeskController.PSDIsOpen(),
              !lastComment.isShown else {
            return
        }
        subscriber.onNewReply(hasUnreadComments: true, lastCommentText: lastComment.text, lastCommentAttachmentsCount: lastComment.attchmentsCount, lastCommentAttachments: lastComment.attachments, utcTime: lastComment.utcTime)
        lastComment.isShown = true
        updateLastComment(lastComment)
    }
}
