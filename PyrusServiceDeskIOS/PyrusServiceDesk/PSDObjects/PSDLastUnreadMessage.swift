import Foundation
///MARK: PSDLastUnreadMessage
///The object to store las unread message
class PSDLastUnreadMessage: NSObject {
    ///MARK: constants
    private let MAX_ATTACHMENTS_COUNT = 10
    private let MAX_ATTACHMENT_NAME_COUNT = 30
    private let MAX_TEXT_COUNT = 500
    
    ///MARK: properties
    private(set) var text: String?
    private(set) var attachments: [String]?
    private(set) var attchmentsCount: Int = 0
    private(set) var utcTime: Double
    private(set) var messageId: String
    ///Detect if this message was shown to user in chat(chat was opened)
    private var isRead = false
    ///Detect if this message was sent to  NewReplySubscriber
    var isShown = false
    
    init(message: PSDMessage) {
        text = message.text.count > 0 ? String(message.text.suffix(MAX_TEXT_COUNT)) : nil
        if let att = message.attachments, att.count > 0 {
            attchmentsCount = att.count
            attachments = [String]()
            for (i, attachment) in att.enumerated() {
                if i > MAX_ATTACHMENTS_COUNT{
                    break
                }
                attachments?.append(String(attachment.name.suffix(MAX_ATTACHMENT_NAME_COUNT)))
            }
        }
        messageId = message.messageId
        utcTime = message.date.timeIntervalSince1970
    }
    init(dictionary: [String: Any]) {
        text = dictionary[keys.text.rawValue] as? String
        attachments = dictionary[keys.attachments.rawValue] as? [String]
        attchmentsCount = dictionary[keys.attchmentsCount.rawValue] as? Int ?? 0
        utcTime = dictionary[keys.utcTime.rawValue] as? Double ?? 0
        messageId = dictionary[keys.messageId.rawValue] as? String ?? "0"
        isRead = dictionary[keys.isRead.rawValue] as? Bool ?? false
        isShown = dictionary[keys.isShown.rawValue]  as? Bool ?? false
    }
    func toDictioanary() -> [String: Any] {
        var dict = [String: Any]()
        dict[keys.text.rawValue] = text
        dict[keys.attachments.rawValue] = attachments
        dict[keys.attchmentsCount.rawValue] = attchmentsCount
        dict[keys.utcTime.rawValue] = utcTime
        dict[keys.messageId.rawValue] = messageId
        dict[keys.isRead.rawValue] = isRead
        dict[keys.isShown.rawValue] = isShown
        return dict
    }
}

///MARK: Keys for  Dictionary
private enum keys: String {
    case text = "text"
    case attachments = "attachments"
    case attchmentsCount = "attchmentsCount"
    case utcTime = "utcTime"
    case messageId = "messageId"
    case isRead = "isRead"
    case isShown = "isShown"
}
