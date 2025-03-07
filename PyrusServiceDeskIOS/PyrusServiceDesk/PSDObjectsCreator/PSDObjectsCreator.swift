import Foundation

class PSDObjectsCreator {
    static func getNextLocalId() -> Int {
        let valueKey = "LastLocalId"
        let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults()
        let lastLocalId = pyrusUserDefaults?.integer(forKey: valueKey) ?? 0
        let next = lastLocalId - 1
        pyrusUserDefaults?.set(next, forKey: valueKey)
        return next
    }
    
    /**
     Create a new message that was not sent. PSDMessage date is always now and owner is current user. TicketId is empty string.
     - parameter comment: A text sending to server.
    - parameter comment: A text sending to server.
     */
    static func createMessage(_ comment:String, attachments:[PSDAttachment]?, ticketId: Int = 0, userId: String? = nil) -> PSDMessage {
        return PSDObjectsCreator.createMessage(comment, attachments: attachments, user:PSDUsers.user, ticketId: ticketId, userId: userId)
    }
    static func createWelcomeMessage()->PSDRowMessage{
        let message = PSDObjectsCreator.createMessage(CustomizationHelper.welcomeMessage, attachments:nil, user: PSDUser(personId: "", name: "", type: .support, imagePath: ""), userId: nil)
        return PSDRowMessage(message: message, attachment: nil, text: message.text)
    }
    
    static func createRatingMessage(_ text: String, ticketId: Int = 0, userId: String? = nil) -> PSDRowMessage {
        let message = PSDObjectsCreator.createMessage(text, attachments:nil, user: PSDUser(personId: "", name: "", type: .support, imagePath: ""), ticketId: ticketId, userId: userId)
        message.isRatingMessage = true
        return PSDRowMessage(message: message, attachment: nil, text: message.text)
    }
    
    static func createAttachment(_ data: Data, _ url: URL?) -> PSDAttachment {
        return PSDAttachment(localPath: url?.absoluteString, data: data, serverIdentifer:nil)
    }
    static func parseMessageToRowMessage(_ message: PSDMessage)->[PSDRowMessage]{
        if let attachments = message.attachments, attachments.count > 0{
            var rowMessages = [PSDRowMessage]()
            for (i,attachment) in attachments.enumerated(){
                let rowMessage = PSDRowMessage(message: message, attachment: attachment, text: i > 0 ? "" : message.text)
                if i > 0{
                    rowMessage.rating = nil
                }
                rowMessages.append(rowMessage)
            }
            return rowMessages
        }else{
            return [PSDRowMessage(message: message, attachment: nil, text: message.text)]
        }
    }
    ///Returns number of PSDRowMessage after parsing PSDMessage, minimum value is 1
    static func rowMessagesCount(for message: PSDMessage) -> Int {
        if let attachments = message.attachments, attachments.count > 0{
            return attachments.count
        }
        return 1
    }
    static func createMessage(rating: Int, ticketId: Int, userId: String? = nil) -> PSDMessage {
        let message = PSDObjectsCreator.createMessage("", attachments: nil, user: PSDUsers.user, ticketId: ticketId, userId: userId)
        message.rating = rating
        return message
    }
    /**
     Create a new message that was not sent.
     - parameter data: Any object to message. If has string
     */
    private static func createMessage(_ text:String, attachments: [PSDAttachment]?, user:PSDUser, ticketId: Int = 0, userId: String?)->PSDMessage{
        let messageId: String = ""
        let date: Date = Date()
        let message = PSDMessage(text: text, attachments: attachments, messageId: messageId, owner: user, date: date)
        message.state = .sending
        message.ticketId = ticketId
        message.isOutgoing = PyrusServiceDesk.authorId == user.authorId
        message.userId = userId
        message.isWelcomeMessage = true
        return message
    }
}
