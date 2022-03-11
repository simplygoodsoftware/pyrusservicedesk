import Foundation
///the placeholder for message, using to show that here is exoecting the  message from server
class PSDPlaceholderMessage: PSDMessage {
    init(owner: PSDUser, messageId: String) {
        super.init(text: "", attachments: nil, messageId: messageId, owner: owner, date: nil)
    }
}
