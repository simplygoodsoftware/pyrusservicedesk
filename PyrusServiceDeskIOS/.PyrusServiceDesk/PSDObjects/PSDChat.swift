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
}
