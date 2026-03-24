import Foundation
import UIKit

@objc(AttributedStringTransformer)
final class AttributedStringTransformer: NSSecureUnarchiveFromDataTransformer {
    static let name = NSValueTransformerName("AttributedStringTransformer")

    // Разрешённые классы для безопасного разархивирования
    override class var allowedTopLevelClasses: [AnyClass] {
        return [
            NSAttributedString.self,
            NSMutableAttributedString.self,
            UIFont.self,
            UIColor.self,
            NSParagraphStyle.self,
            NSMutableParagraphStyle.self,
            NSTextAttachment.self,
            NSTextTab.self,
            NSShadow.self,
            UIImage.self,
            NSURL.self,
            NSData.self,
            NSNumber.self
        ]
    }

    static func register() {
        let transformer = AttributedStringTransformer()
        ValueTransformer.setValueTransformer(transformer, forName: name)
    }
}
