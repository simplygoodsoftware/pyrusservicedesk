import UIKit
class PSDChat: Hashable {
    var chatId: Int?
    var date: Date?
    var messages: [PSDMessage]
    var isRead = true
    var showRating = false
    var showRatingText: String?
    var subject: String?
    var lastComment: PSDMessage?
    var userId: String?
    var lastReadedCommentId: Int?
    var isActive = true
    var createdAt: Date?
    
    init(chatId: Int?, date: Date, messages: [PSDMessage]) {
        self.chatId = chatId
        self.date = date
        self.messages = messages
    }
    
    static func == (lhs: PSDChat, rhs: PSDChat) -> Bool {
        return lhs.chatId == rhs.chatId && lhs.date == rhs.date && lhs.isRead == rhs.isRead && lhs.showRating == rhs.showRating && lhs.showRatingText == rhs.showRatingText && lhs.lastReadedCommentId == rhs.lastReadedCommentId && lhs.isActive == rhs.isActive && lhs.lastComment == rhs.lastComment
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(chatId)
        hasher.combine(date)
        hasher.combine(isRead)
        hasher.combine(showRating)
        hasher.combine(showRatingText)
        hasher.combine(subject)
        hasher.combine(lastReadedCommentId)
        hasher.combine(isActive)
    }
    
    static func draftAnswers(_ tableMatrix: [[PSDRowMessage]]) -> [ButtonData]? {
        guard
            let messages = tableMatrix.last,
            let message = messages.last?.message
        else {
            return nil
        }
        let (_, links) = (message.text as NSString).parseXMLToAttributedString(fontColor: .appTextColor)
        guard
            let links = links,
            links.count > 0
        else {
            return nil
        }
        if links.count > MAX_LINKS_COUNT {
            return Array(links[..<MAX_LINKS_COUNT]) as [ButtonData]
        }
        return links
    }
}
private let MAX_LINKS_COUNT: Int = 10
