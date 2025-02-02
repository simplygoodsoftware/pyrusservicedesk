import UIKit


///The state of message.
enum messageState: Int16 {
    ///message now is sending
    case sending
    ///message was sent to server
    case sent
    ///message was try to sent, but end with some error
    case cantSend
}
///The messages that came from sever
class PSDMessage: NSObject {
    //messages saved in PyrusServiceDeskCreator is not denited
    var text: String
    var attachments: [PSDAttachment]?
    var owner: PSDUser
    ///The server Id
    var messageId: String
    ///The local created id
    var clientId: String
    var date = Date()
    var state: messageState
    var rating: Int?
    var isOutgoing: Bool = true
    var fromStrorage: Bool = false
    var isRatingMessage: Bool = false
    var ticketId: Int = 0
    var userId: String?
    var appId: String?
    var commandId: String?
    var requestNewTicket = false
    var isWelcomeMessage = false
    
    init(text: String?, attachments: [PSDAttachment]?, messageId: String?, owner: PSDUser?, date: Date?) {
        self.text = text ?? ""        
        self.attachments = attachments

        self.owner = owner ?? PSDUsers.user
        self.messageId = messageId ?? "0"
        self.date = date ?? Date()
        self.state = .sending
        self.clientId = UUID().uuidString
        super.init()
        
        if self.hasId() || self.owner != PSDUsers.user {
            self.state = .sent
        }
        
       
    }
    func hasId() -> Bool {
        if(self.messageId != "0" && self.messageId != ""){
            return true
        }
        return false
    }
}
