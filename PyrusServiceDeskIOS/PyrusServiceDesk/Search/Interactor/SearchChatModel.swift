import Foundation

struct SearchChatModel {
    let id: Int
    let date: Date
    let subject: String
    let messageText: String
    let messageId: String
    let authorName: String
    let isMessage: Bool
    let messageAttributedText: NSAttributedString?
    var lastMessage: PSDMessage?
    
    init(id: Int, date: Date, subject: String, messageText: String, messageId: String, authorName: String, isMessage: Bool, messageAttributedText: NSAttributedString?) {
        self.id = id
        self.date = date
        self.subject = subject
        self.messageText = messageText
        self.messageId = messageId
        self.authorName = authorName
        self.isMessage = isMessage
        self.messageAttributedText = messageAttributedText
    }
}
