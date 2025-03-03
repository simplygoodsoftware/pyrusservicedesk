
struct SearchChatViewModel: Hashable {
    let id: Int
    let date: String
    let subject: String
    let messageText: NSAttributedString
    let messageId: String
    
    init(id: Int, date: String, subject: String, messageText: NSAttributedString, messageId: String) {
        self.id = id
        self.date = date
        self.subject = subject
        self.messageText = messageText
        self.messageId = messageId
    }
    
    static func == (lhs: SearchChatViewModel, rhs: SearchChatViewModel) -> Bool {
        return lhs.id == rhs.id && lhs.subject == rhs.subject && lhs.messageText == rhs.messageText && lhs.messageId == rhs.messageId
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(date)
        hasher.combine(subject)
        hasher.combine(messageText)
        hasher.combine(messageId)
    }
}
