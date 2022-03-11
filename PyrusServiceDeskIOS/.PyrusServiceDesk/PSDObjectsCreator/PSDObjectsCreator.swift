import Foundation

struct PSDObjectsCreator {
    /*
     {\"CommentId\":182744765,\"Body\":\"Привет\",\"IsInbound\":true,\"CreatedAt\":\"2019-02-13T13:21:28Z\"}
 */
    /**
     Create a new message that was not sent. PSDMessage date is always now and owner is current user. TicketId is empty string.
     - parameter comment: A text sending to server.
    - parameter comment: A text sending to server.
     */
    static func createMessage(_ comment:String, attachments:[PSDAttachment]?)->PSDMessage{
        return PSDObjectsCreator.createMessage(comment, attachments: attachments, user:PSDUsers.user)
    }
    static func createWelcomeMessage()->PSDRowMessage{
        let message = PSDObjectsCreator.createMessage(CustomizationHelper.welcomeMessage, attachments:nil, user: PSDUser(personId: "", name: "", type: .support, imagePath: ""))
        return PSDRowMessage(message: message, attachment: nil)
    }
    static func createAttachment(_ data: Data, _ url: URL?) -> PSDAttachment {
        return PSDAttachment(localPath: url?.absoluteString, data: data, serverIdentifer:nil)
    }
    static func parseMessageToRowMessage(_ message: PSDMessage)->[PSDRowMessage]{
        if let attachments = message.attachments, attachments.count > 0{
            var rowMessages = [PSDRowMessage]()
            for (i,attachment) in attachments.enumerated(){
                let rowMessage = PSDRowMessage(message: message, attachment: attachment)
                if i > 0{
                    rowMessage.text = ""
                    rowMessage.rating = nil
                }
                rowMessages.append(rowMessage)
            }
            return rowMessages
        }else{
            return [PSDRowMessage(message: message, attachment: nil)]
        }
    }
    ///Returns number of PSDRowMessage after parsing PSDMessage, minimum value is 1
    static func rowMessagesCount(for message: PSDMessage) -> Int {
        if let attachments = message.attachments, attachments.count > 0{
            return attachments.count
        }
        return 1
    }
    static func createMessage(rating: Int) -> PSDMessage {
        let message = PSDObjectsCreator.createMessage("", attachments: nil, user: PSDUsers.user)
        message.rating = rating
        return message
    }
    /**
     Create a new message that was not sent.
     - parameter data: Any object to message. If has string
     */
    private static func createMessage(_ text:String, attachments: [PSDAttachment]?, user:PSDUser)->PSDMessage{
        let ticketId : String = ""
        let date : Date = Date()
        let message = PSDMessage(text:text , attachments:attachments, messageId: ticketId, owner:user, date:date)
        message.state = .sending
        return message
    }
}
