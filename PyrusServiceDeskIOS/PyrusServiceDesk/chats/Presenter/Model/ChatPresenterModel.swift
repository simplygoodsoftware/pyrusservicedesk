import Foundation

struct ChatPresenterModel {
    let id: Int
    let date: String?
    let isRead: Bool
    let isActive: Bool
    let subject: String?
    let lastComment: PSDMessage?
    let messages: [PSDMessage]
    let lastMessageAttributedText: String?
}
