import Foundation

struct ChatPresenterModel {
    let id: Int
    let date: Date?
    let isRead: Bool
    let subject: String?
    let lastComment: PSDMessage?
    let messages: [PSDMessage]
}
