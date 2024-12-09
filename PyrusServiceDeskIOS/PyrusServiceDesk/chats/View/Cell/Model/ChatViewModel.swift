import Foundation

class ChatViewModel: PSDChatsViewModelProtocol {
    let id: Int
    let date: String
    let isRead: Bool
    let subject: String
    let lastMessageText: String
    let attachmentText: String
    let hasAttachment: Bool
    let state: messageState
    
    init(type: PSDChatsCellType = .chat, id: Int, date: String, isRead: Bool, subject: String, lastMessageText: String, attachmentText: String, hasAttachment: Bool, state: messageState) {
        self.id = id
        self.date = date
        self.isRead = isRead
        self.subject = subject
        self.lastMessageText = lastMessageText
        self.attachmentText = attachmentText
        self.hasAttachment = hasAttachment
        self.state = state
        super.init(type: type)
    }
    
    static func == (lhs: ChatViewModel, rhs: ChatViewModel) -> Bool {
        return lhs.id == rhs.id && lhs.date == rhs.date && lhs.isRead == rhs.isRead && lhs.subject == rhs.subject && lhs.lastMessageText == rhs.lastMessageText && lhs.attachmentText == rhs.attachmentText && lhs.hasAttachment == rhs.hasAttachment && lhs.state == rhs.state
    }
    
    override func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(date)
        hasher.combine(isRead)
        hasher.combine(subject)
        hasher.combine(lastMessageText)
        hasher.combine(attachmentText)
        hasher.combine(hasAttachment)
        hasher.combine(state)
    }
}
