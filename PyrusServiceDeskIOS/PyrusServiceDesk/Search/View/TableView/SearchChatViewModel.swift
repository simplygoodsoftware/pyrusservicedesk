
struct SearchChatViewModel: Hashable {
    let id: Int
    let date: String
    let subject: NSAttributedString
    let messageText: NSAttributedString
    let messageId: String
    let isMessage: Bool
    var isAudio: Bool = false
    var hasAttachments: Bool = false
    var attachmentName: String?
    
    init(id: Int, date: String, subject: NSAttributedString, messageText: NSAttributedString, messageId: String, isMessage: Bool) {
        self.id = id
        self.date = date
        self.subject = subject
        self.messageText = messageText
        self.messageId = messageId
        self.isMessage = isMessage
    }
    
    static func == (lhs: SearchChatViewModel, rhs: SearchChatViewModel) -> Bool {
        return lhs.id == rhs.id && lhs.subject == rhs.subject && lhs.messageText == rhs.messageText && lhs.messageId == rhs.messageId && lhs.isMessage == rhs.isMessage
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(date)
        hasher.combine(messageId)
        hasher.combine(isMessage)
        hasher.combine(subject)
        hasher.combine(messageText)
    }
}
