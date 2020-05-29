import UIKit

class PSDChat: NSObject {
    var chatId: String?
    var date: Date?
    var messages: [PSDMessage]
    var isRead : Bool = true
    init(chatId: String, date:Date, messages:[PSDMessage])  {
        self.chatId = chatId
        self.date = date
        self.messages = messages
        
    }
    
}
