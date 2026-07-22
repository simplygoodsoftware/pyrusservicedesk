import UIKit

// MARK: - Кастомный атрибут группы цитат

extension NSAttributedString.Key {
    /// Помечает диапазон текста как цитату.
    /// Значение — Int (id группы подряд идущих quote-блоков),
    /// чтобы черта рисовалась одной непрерывной линией на всю группу.
    static let psdQuoteGroup = NSAttributedString.Key("psdQuoteGroup")
}

// MARK: - LayoutManager, рисующий вертикальную черту цитаты

/// Рисует вертикальную скруглённую черту слева от каждого диапазона
/// с атрибутом `.psdQuoteGroup`. В отличие от NSTextAttachment,
/// черта тянется на всю высоту цитаты: и при переносах строк,
/// и при явных LineBreak внутри блока, и для нескольких quote-блоков подряд.
final class QuoteBarLayoutManager: NSLayoutManager {

    struct Appearance {
        var barColor: UIColor = .quoteBar
        var barWidth: CGFloat = 3
        var cornerRadius: CGFloat = 1.5
        /// Отступ черты от левого края текст-контейнера.
        var barLeadingInset: CGFloat = 0
        /// Вертикальный «выпуск» черты за границы текста цитаты.
        var verticalOutset: CGFloat = 1
    }

    var appearance = Appearance()

    override func drawBackground(forGlyphRange glyphsToShow: NSRange, at origin: CGPoint) {
        super.drawBackground(forGlyphRange: glyphsToShow, at: origin)

        guard
            let textStorage,
            let container = textContainers.first,
            textStorage.length > 0
        else { return }

        let fullRange = NSRange(location: 0, length: textStorage.length)

        // Тексты объявлений короткие, поэтому обходим весь storage —
        // так черта частично видимой группы не «обрезается» по глифам.
        textStorage.enumerateAttribute(.psdQuoteGroup, in: fullRange, options: []) { value, range, _ in
            guard value != nil else { return }

            let glyphRange = self.glyphRange(forCharacterRange: range, actualCharacterRange: nil)
            guard glyphRange.length > 0 else { return }

            // Union-прямоугольник всех строк группы (включая межпараграфные зазоры).
            var rect = self.boundingRect(forGlyphRange: glyphRange, in: container)
            rect.origin.x = origin.x + self.appearance.barLeadingInset
            rect.origin.y += origin.y - self.appearance.verticalOutset
            rect.size.width = self.appearance.barWidth
            rect.size.height += self.appearance.verticalOutset * 2

            // Динамический цвет корректно резолвится в текущем trait при отрисовке.
            self.appearance.barColor.setFill()
            UIBezierPath(roundedRect: rect, cornerRadius: self.appearance.cornerRadius).fill()
        }
    }
}

// MARK: - UITextView с кастомным layout manager (TextKit 1)

final class AnnouncementTextView: UITextView {

    /// Фабрика собирает TextKit-1 стек с QuoteBarLayoutManager.
    /// Использовать вместо `UITextView()` в PSDAnnouncementCell.
    static func make(appearance: QuoteBarLayoutManager.Appearance = .init()) -> AnnouncementTextView {
        let storage = NSTextStorage()
        let layoutManager = QuoteBarLayoutManager()
        layoutManager.appearance = appearance
        storage.addLayoutManager(layoutManager)

        let container = NSTextContainer(size: .zero)
        container.widthTracksTextView = true
        // Черта рисуется от самого края текст-вью, отступ текста задаёт paragraphStyle.
        container.lineFragmentPadding = 0
        layoutManager.addTextContainer(container)

        let textView = AnnouncementTextView(frame: .zero, textContainer: container)
        textView.isEditable = false
        textView.isScrollEnabled = false
        textView.backgroundColor = .clear
        textView.textContainerInset = .zero
        return textView
    }
}

// MARK: - Цвета

private extension UIColor {
    static let quoteBar = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(white: 0.55, alpha: 1)
        default:
            return UIColor(white: 0.7, alpha: 1)
        }
    }
}

// MARK: - Детекция телефонов (замена dataDetectorTypes у UITextView)

extension NSAttributedString {
    /// Добавляет tel:-ссылки на телефонные номера.
    /// Выполнять на фоне при построении строки, а не в ячейке:
    /// синхронная детекция в UITextView — источник фризов при скролле.
    func addingPhoneNumberLinks() -> NSAttributedString {
        guard length > 0,
              let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.phoneNumber.rawValue)
        else { return self }

        let mutable = NSMutableAttributedString(attributedString: self)
        let fullRange = NSRange(location: 0, length: mutable.length)

        detector.enumerateMatches(in: mutable.string, options: [], range: fullRange) { match, _, _ in
            guard
                let match,
                match.resultType == .phoneNumber,
                let phone = match.phoneNumber,
                match.range.location < mutable.length,
                mutable.attribute(.link, at: match.range.location, effectiveRange: nil) == nil
            else { return }

            let digits = phone.filter { "+0123456789".contains($0) }
            guard let url = URL(string: "tel://\(digits)") else { return }
            mutable.addAttribute(.link, value: url, range: match.range)
        }
        return mutable
    }
}
