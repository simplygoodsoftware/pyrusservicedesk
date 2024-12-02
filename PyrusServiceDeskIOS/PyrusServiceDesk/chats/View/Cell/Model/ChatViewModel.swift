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
}
