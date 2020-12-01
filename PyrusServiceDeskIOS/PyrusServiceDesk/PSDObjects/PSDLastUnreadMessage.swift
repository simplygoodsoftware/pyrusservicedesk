import Foundation
///MARK: PSDLastUnreadMessage
///The object to store las unread message
final class PSDLastUnreadMessage: NSObject, Codable {
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
    private enum CodingKeys: String, CodingKey {
        case text, attachments, attchmentsCount, utcTime, messageId, isRead, isShown
    }
    
    required public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        text = try values.decode(String.self, forKey: .text)
        attachments = try values.decodeIfPresent([String].self, forKey: .attachments)
        attchmentsCount = try values.decode(Int.self, forKey: .attchmentsCount)
        utcTime = try values.decode(Double.self, forKey: .utcTime)
        messageId = try values.decode(String.self, forKey: .messageId)
        isRead = try values.decode(Bool.self, forKey: .isRead)
        isShown = try values.decode(Bool.self, forKey: .isShown)
    }
    func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(text, forKey: .text)
        try values.encode(attachments, forKey: .attachments)
        try values.encode(attchmentsCount, forKey: .attchmentsCount)
        try values.encode(utcTime, forKey: .utcTime)
        try values.encode(messageId, forKey: .messageId)
        try values.encode(isRead, forKey: .isRead)
        try values.encode(isShown, forKey: .isShown)
    }
}
///MARK: constants
private let MAX_ATTACHMENTS_COUNT = 10
private let MAX_ATTACHMENT_NAME_COUNT = 30
private let MAX_TEXT_COUNT = 500
