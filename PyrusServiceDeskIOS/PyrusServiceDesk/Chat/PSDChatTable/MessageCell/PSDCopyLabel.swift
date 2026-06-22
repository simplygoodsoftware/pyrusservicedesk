import Foundation
import UIKit

protocol LinkDelegate: NSObject {
    func showLinkOpenAlert(_ title: String)
}

class PSDCopyTextView: UITextView, UIGestureRecognizerDelegate, UITextViewDelegate {

    weak var linkDelegate: LinkDelegate?

    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        self.isEditable = false
        self.isSelectable = true
        self.delegate = self
        self.keyboardAppearance = CustomizationHelper.keyboardStyle
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: - Отключаем выделение текста, но оставляем тап по ссылкам

    override var selectedTextRange: UITextRange? {
        get { return nil }
        set {}
    }

    override func selectionRects(for range: UITextRange) -> [UITextSelectionRect] {
        return []
    }

    override var canBecomeFirstResponder: Bool {
        return false
    }

    // MARK: - Касания мимо ссылок уходят «сквозь» вью, на ссылке — перехватываем

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        guard
            let pos = closestPosition(to: point),
            let range = tokenizer.rangeEnclosingPosition(pos, with: .character, inDirection: .layout(.left))
        else {
            return false
        }
        let startIndex = offset(from: beginningOfDocument, to: range.start)
        return attributedText.attribute(.link, at: startIndex, effectiveRange: nil) != nil
    }

    // MARK: - UITextViewDelegate

    func textViewShouldBeginEditing(_ textView: UITextView) -> Bool {
        return false
    }

    func textView(_ textView: UITextView,
                  shouldInteractWith URL: URL,
                  in characterRange: NSRange,
                  interaction: UITextItemInteraction) -> Bool {
        if interaction == .invokeDefaultAction {
            handleLink(URL)
        }
        return false
    }

    // MARK: - Private

    private func handleLink(_ link: URL) {
        let decodedString = HelpersStrings.decodingHTMLEntitiesInLink(link.absoluteString)
        guard let url = URL(string: decodedString) else { return }

        if HelpersStrings.insideDomain(url: url) {
            UIApplication.shared.open(url)
        } else {
            linkDelegate?.showLinkOpenAlert(url.absoluteString)
        }
    }
}
