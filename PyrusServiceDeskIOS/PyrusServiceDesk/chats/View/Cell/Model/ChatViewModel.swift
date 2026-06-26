import Foundation

struct ChatViewModel: Hashable {
    let id: Int
    let date: String
    let isRead: Bool
    let subject: String
    let lastMessageText: String
    let attachmentText: String
    let hasAttachment: Bool
    let state: messageState
    let isAudio: Bool
    let lastMessageId: String?
    let lastMessageCommandId: String?
    let lastMessageAuthor: String
    
    init(type: PSDChatsCellType = .chat, id: Int, date: String, isRead: Bool, subject: String, lastMessageText: String, attachmentText: String, hasAttachment: Bool, state: messageState, isAudio: Bool, lastMessageId: String?, lastMessageCommandId: String?, lastMessageAuthor: String) {
        self.id = id
        self.date = date
        self.isRead = isRead
        self.subject = subject
        self.lastMessageText = lastMessageText
        self.attachmentText = attachmentText
        self.hasAttachment = hasAttachment
        self.state = state
        self.isAudio = isAudio
        self.lastMessageId = lastMessageId
        self.lastMessageAuthor = lastMessageAuthor
        self.lastMessageCommandId = lastMessageCommandId
    }
    
    static func == (lhs: ChatViewModel, rhs: ChatViewModel) -> Bool {
        return lhs.id == rhs.id &&
                lhs.lastMessageId == rhs.lastMessageId &&
                lhs.lastMessageCommandId == rhs.lastMessageCommandId &&
                lhs.date == rhs.date &&
                lhs.isRead == rhs.isRead &&
                lhs.attachmentText == rhs.attachmentText &&
                lhs.hasAttachment == rhs.hasAttachment &&
                lhs.lastMessageAuthor == rhs.lastMessageAuthor &&
                lhs.isAudio == rhs.isAudio &&
                lhs.subject.prefix(50) == rhs.subject.prefix(50) &&
                lhs.state == rhs.state
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(lastMessageId)
        hasher.combine(lastMessageCommandId)
        hasher.combine(date)
        hasher.combine(isRead)
        hasher.combine(attachmentText)
        hasher.combine(hasAttachment)
        hasher.combine(isAudio)
        hasher.combine(lastMessageAuthor)
        hasher.combine(subject.prefix(50))
        hasher.combine(state)
    }
}
