import Foundation

struct SearchChatModel {
    let id: Int
    let date: Date
    let subject: String
    let messageText: String
    let messageId: String
    
    init(id: Int, date: Date, subject: String, messageText: String, messageId: String) {
        self.id = id
        self.date = date
        self.subject = subject
        self.messageText = messageText
        self.messageId = messageId
    }
}
