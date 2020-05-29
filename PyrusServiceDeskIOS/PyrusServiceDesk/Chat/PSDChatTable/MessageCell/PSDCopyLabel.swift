
import Foundation
class PSDCopyTextView :UITextView,UIGestureRecognizerDelegate, UITextViewDelegate{
    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer:textContainer)
        self.isEditable = false
        self.delegate = self
    }
    override var selectedTextRange: UITextRange? {
        get { return nil }
        set {}
    }
    override func selectionRects(for range: UITextRange) -> [UITextSelectionRect] {
        return []
    }
    func textViewShouldBeginEditing(_ textView: UITextView) -> Bool {
        return false
    }
    override var canBecomeFirstResponder: Bool{
        return false
    }
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        guard let pos = closestPosition(to: point) else {
            return false
        }
        guard let range = tokenizer.rangeEnclosingPosition(pos, with: .character, inDirection: .layout(.left)) else {
            return false
        }
        let startIndex = offset(from: beginningOfDocument, to: range.start)
        return attributedText.attribute(.link, at: startIndex, effectiveRange: nil) != nil
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

