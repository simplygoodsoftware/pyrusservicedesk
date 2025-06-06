import UIKit


///The state of message.
enum messageState: Int16 {
    ///message now is sending
    case sending
    ///message was sent to server
    case sent
    ///message was try to sent, but end with some error
    case cantSend
}
///The messages that came from sever
class PSDMessage: NSObject {
    //messages saved in PyrusServiceDeskCreator is not denited
    var text: String
    var attributedText: NSAttributedString?
    var attachments: [PSDAttachment]?
    var owner: PSDUser
    ///The server Id
    var messageId: String
    ///The local created id
    var clientId: String
    var date = Date()
    var state: messageState
    var rating: Int? {
        didSet {
            if rating ?? 0 > 0 {
                isRatingMessage = true
            }
        }
    }
    var isOutgoing: Bool = true
    var fromStrorage: Bool = false
    var isRatingMessage: Bool = false
    var ticketId: Int = 0
    var userId: String?
    var appId: String?
    var commandId: String?
    var requestNewTicket = false
    var isWelcomeMessage = false
    
    init(text: String?, attachments: [PSDAttachment]?, messageId: String?, owner: PSDUser?, date: Date?) {
        self.text = text ?? ""        
        self.attachments = attachments

        self.owner = owner ?? PSDUsers.user
        self.messageId = messageId ?? "0"
        self.date = date ?? Date()
        self.state = .sending
        self.clientId = UUID().uuidString
        
        super.init()
        
//        DispatchQueue.global().async {
//            let attributedText = AttributedStringCache.cachedString(for: text ?? "", fontColor: .lastMessageInfo, font: .lastMessageInfo, key: messageId ?? self.commandId ?? "")
//        }
        //attributedText =  AttributedStringCache.cachedString(for: text, fontColor: .lastMessageInfo, font: .lastMessageInfo)//HelpersStrings.decodeHTML(in: removeLinkAttributes(from: (text ?? "").parseXMLToAttributedString(fontColor: .lastMessageInfo, font: .lastMessageInfo).0) ?? NSAttributedString(string: ""))
        if self.hasId() || self.owner != PSDUsers.user {
            self.state = .sent
        }
        
        DispatchQueue.global().async {
            for attachment in attachments ?? [] {
                if attachment.isImage, let image = UIImage(data: attachment.data) {
                    attachment.previewImage = image
                }
            }
        }
    }
    func hasId() -> Bool {
        if(self.messageId != "0" && self.messageId != ""){
            return true
        }
        return false
    }
    
    func removeLinkAttributes(from attributedString: NSAttributedString?) -> NSAttributedString? {
        guard let attributedString else { return nil }
        let mutableAttributedString = NSMutableAttributedString(attributedString: attributedString)
        let range = NSRange(location: 0, length: mutableAttributedString.length)
        
        mutableAttributedString.enumerateAttribute(.link, in: range, options: []) { value, range, _ in
            if value != nil {
                mutableAttributedString.removeAttribute(.link, range: range)
            }
        }
        
        return mutableAttributedString
    }
}

private extension UIFont {
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}

private extension UIColor {    
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}

import UIKit

final class AttributedStringCache {
    private static let memoryCache = NSCache<NSString, NSAttributedString>()
    private static let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!

    static func cachedString(
        for text: String,
        fontColor: UIColor,
        font: UIFont,
        key: String?
    ) -> NSAttributedString {
        guard let key, key.count > 0, text.count > 0 else {
            return NSAttributedString(string: text, attributes: [.foregroundColor: fontColor, .font: font])
        }
        if let cached = memoryCache.object(forKey: key as NSString) {
            return cached
        }
        
        if let diskCached = loadFromDiskCache(key: key) {
            memoryCache.setObject(diskCached, forKey: key as NSString)
            return diskCached
        }
        
        let newString = generateAttributedString(text, fontColor: fontColor, font: font)
        
        memoryCache.setObject(newString, forKey: key as NSString)
        DispatchQueue.global().async {
            saveToDiskCache(newString, key: key)
        }
        
        return newString
    }
    
    private static func saveToDiskCache(_ string: NSAttributedString, key: String) {
        let fileURL = cacheDirectory.appendingPathComponent(key)
        if let data = try? NSKeyedArchiver.archivedData(withRootObject: string, requiringSecureCoding: false) {
            try? data.write(to: fileURL)
        }
    }

    private static func loadFromDiskCache(key: String) -> NSAttributedString? {
        let fileURL = cacheDirectory.appendingPathComponent(key)
        if let data = try? Data(contentsOf: fileURL),
           let string = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(data) as? NSAttributedString {
            return string
        }
        return nil
    }
    
    private static func generateAttributedString(_ text: String, fontColor: UIColor, font: UIFont) -> NSAttributedString {
        return HelpersStrings.decodeHTML(
            in: removeLinkAttributes(
                from: text.parseXMLToAttributedString(fontColor: fontColor, font: font).0
            ) ?? NSAttributedString(string: "")
        )
    }
    
    private static func removeLinkAttributes(from attributedString: NSAttributedString?) -> NSAttributedString? {
        guard let attributedString else { return nil }
        let mutableAttributedString = NSMutableAttributedString(attributedString: attributedString)
        let range = NSRange(location: 0, length: mutableAttributedString.length)
        
        mutableAttributedString.enumerateAttribute(.link, in: range, options: []) { value, range, _ in
            if value != nil {
                mutableAttributedString.removeAttribute(.link, range: range)
            }
        }
        
        return mutableAttributedString
    }
}
