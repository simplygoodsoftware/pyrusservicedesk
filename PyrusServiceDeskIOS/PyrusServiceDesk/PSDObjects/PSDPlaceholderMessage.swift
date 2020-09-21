import Foundation
///the placeholder for message, using to show that here is exoecting the  message from server
class PSDPlaceholderMessage: PSDMessage {
    init(owner: PSDUser) {
        super.init(text: "", attachments: nil, messageId: nil, owner: owner, date: nil)
    }
}
