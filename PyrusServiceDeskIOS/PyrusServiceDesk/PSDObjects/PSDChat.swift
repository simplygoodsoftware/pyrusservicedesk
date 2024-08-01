import UIKit
class PSDChat: NSObject {
    var date: Date?
    var messages: [PSDMessage]
    var isRead = true
    var showRating = false
    var showRatingText: String?
    init(date: Date, messages: [PSDMessage]) {
        self.date = date
        self.messages = messages
    }
    
    static func draftAnswers(_ tableMatrix: [[PSDRowMessage]]) -> [String]? {
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
        return links
    }
}
private let MAX_LINKS_COUNT: Int = 6
