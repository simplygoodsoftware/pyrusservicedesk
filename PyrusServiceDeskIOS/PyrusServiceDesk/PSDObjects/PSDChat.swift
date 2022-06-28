import UIKit
class PSDChat: NSObject {
    var date: Date?
    var messages: [PSDMessage]
    var isRead = true
    var showRating = false
    init(date: Date, messages: [PSDMessage]) {
        self.date = date
        self.messages = messages
    }
    
    func draftAnswers() -> [String]? {
        guard
            let message = messages.last
        else {
            return nil
        }
        let (_, links) = (message.text as NSString) .parseXMLToAttributedString(fontColor: .appTextColor)
        guard
            let links = links,
            links.count > 0
        else {
            return nil
        }
        return links
    }
}
