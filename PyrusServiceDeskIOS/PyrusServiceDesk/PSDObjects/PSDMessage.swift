import UIKit


///The state of message.
enum messageState {
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
    var messageId :String
    var localId : String
    var date : Date = Date()
    var state : messageState
    
    init(text: String?, attachments: [PSDAttachment]? ,messageId:String? , owner:PSDUser?, date: Date?){
        self.text = text ?? ""        
        self.attachments = attachments

        self.owner = owner ?? PSDUsers.user
        self.messageId = messageId ?? "0"
        self.date = date ?? Date()
        self.state = .sending
        self.localId = UUID().uuidString
        super.init()
        
        if(self.hasId() || (self.owner != PSDUsers.user)){
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
