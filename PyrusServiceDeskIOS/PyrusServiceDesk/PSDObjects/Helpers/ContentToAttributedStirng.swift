import Foundation
import UIKit

// MARK: - Опции рендера (добавлен флаг заголовка)

public struct RichTextRenderOptions {
    public var baseFont: UIFont = .systemFont(ofSize: 16)
    public var textColor: UIColor = .label
    public var linkColor: UIColor = .systemBlue
    public var underlineLinks: Bool = true
    
    public var paragraphSpacing: CGFloat = 4
    public var lineSpacing: CGFloat = 0
    
    // Списки
    public var listIndent: CGFloat = 20
    public var bulletSymbol: String = "•"
    public var numberSuffix: String = "."
    
    // Цитаты
    public var quoteIndent: CGFloat = 20
    public var quotePrefix: String = "▎"
    public var quotePrefixColor: UIColor = .gray
    public var quotePrefixAsAttachment: Bool = false      // если true — рисуем серую черту как attachment
    public var quotePrefixWidth: CGFloat = 3
    public var quotePrefixCornerRadius: CGFloat = 1
    
    // Inline code
    public var codeFont: UIFont = .monospacedSystemFont(ofSize: 15, weight: .regular)
    public var codeBackground: UIColor = {
        if #available(iOS 13.0, *) { return .secondarySystemBackground }
        else { return UIColor(white: 0.9, alpha: 1) }
    }()
    public var codeForeground: UIColor? = nil
    
    // Code block
    public var codeBlockFont: UIFont = .monospacedSystemFont(ofSize: 15, weight: .regular)
    public var codeBlockBackground: UIColor = {
        if #available(iOS 13.0, *) { return .secondarySystemBackground }
        else { return UIColor(white: 0.9, alpha: 1) }
    }()
    public var codeBlockForeground: UIColor? = nil
    public var codeBlockIndent: CGFloat = 16
    public var codeBlockLineSpacing: CGFloat = 2
    public var codeBlockParagraphSpacing: CGFloat = 8
    
    // Заголовок для первой строки
    public var makeFirstLineHeader: Bool = true
    public var headerFont: UIFont = .systemFont(ofSize: 17, weight: .semibold)
    public var headerColor: UIColor? = nil                // nil -> оставить базовый цвет
    public var headerSkipCodeBlocks: Bool = true          // пропускать Code как первый блок
    
    public init() {}
}

// MARK: - Content → NSAttributedString

extension RichTextDocument {
    func toAttributedString(options: RichTextRenderOptions = .init()) -> NSAttributedString {
        let result = NSMutableAttributedString()
        let blocks = richTextBlocks ?? []
        
        var orderedListCounter = 0
        var previousType: BlockType? = nil
        var didApplyHeader = false
        
        for (i, block) in blocks.enumerated() {
            // --- Блочный код отдельно ---
            if block.type == .code {
                appendCodeBlock(block, to: result, options: options)
                if i < blocks.count - 1 {
                    let baseAttrs: [NSAttributedString.Key: Any] = [
                        .font: options.baseFont,
                        .foregroundColor: options.textColor
                    ]
                    result.append(NSAttributedString(string: "\n", attributes: baseAttrs))
                }
                previousType = .code
                continue
            }
            
            // Буфер блока
            let blockBuffer = NSMutableAttributedString()
            let baseAttrs: [NSAttributedString.Key: Any] = [
                .font: options.baseFont,
                .foregroundColor: options.textColor
            ]
            
            // Счётчик нумерованного списка
            if block.type == .numberListItem {
                orderedListCounter = (previousType == .numberListItem) ? (orderedListCounter + 1) : 1
            } else {
                orderedListCounter = 0
            }
            previousType = block.type
            
            // Префикс и отступы
            var prefixRangeInBlock: NSRange?
            var indent: CGFloat = 0
            var needsIndent = false
            let isQuote = (block.type == .quote)
            
            switch block.type {
            case .paragraph:
                break
            case .bulletListItem:
                indent = options.listIndent
                needsIndent = true
                let prefix = "\(options.bulletSymbol)\t"
                blockBuffer.append(NSAttributedString(string: prefix, attributes: baseAttrs))
                prefixRangeInBlock = NSRange(location: 0, length: (prefix as NSString).length)
                
            case .numberListItem:
                indent = options.listIndent
                needsIndent = true
                let prefix = "\(orderedListCounter)\(options.numberSuffix)\t"
                blockBuffer.append(NSAttributedString(string: prefix, attributes: baseAttrs))
                prefixRangeInBlock = NSRange(location: 0, length: (prefix as NSString).length)
                
            case .quote:
                indent = options.quoteIndent
                needsIndent = true
                
                if options.quotePrefixAsAttachment {
                    // Attachment-черта (100% сохраняет серый цвет)
                    let attach = makeQuoteBarAttachment(font: options.baseFont,
                                                        color: options.quotePrefixColor,
                                                        width: options.quotePrefixWidth,
                                                        radius: options.quotePrefixCornerRadius)
                    let start = blockBuffer.length
                    blockBuffer.append(NSAttributedString(attachment: attach))
                    blockBuffer.append(NSAttributedString(string: "\t", attributes: baseAttrs))
                    prefixRangeInBlock = NSRange(location: start, length: 2) // аттач + таб
                } else {
                    // Символ «▎» + таб
                    let prefix = "\(options.quotePrefix)\t"
                    let prefixAttrs: [NSAttributedString.Key: Any] = [
                        .font: options.baseFont,
                        .foregroundColor: options.quotePrefixColor
                    ]
                    let start = blockBuffer.length
                    blockBuffer.append(NSAttributedString(string: prefix, attributes: prefixAttrs))
                    prefixRangeInBlock = NSRange(location: start, length: (prefix as NSString).length)
                }
                
            case .code:
                break
            case .header:
                break
            }
            
            // Инлайны
            for inline in block.richTextInlines {
                switch inline.type {
                case .lineBreak:
                    blockBuffer.append(NSAttributedString(string: "\n", attributes: baseAttrs))
                case .text, .link:
                    let attrs = attributes(for: inline, base: options)
                    if let s = inline.string, !s.isEmpty {
                        blockBuffer.append(NSAttributedString(string: s, attributes: attrs))
                    }
                }
            }
            
            // Параграфные стили
            if !needsIndent {
                let p = NSMutableParagraphStyle()
                p.paragraphSpacing = options.paragraphSpacing
                p.lineSpacing = options.lineSpacing
                if blockBuffer.length > 0 {
                    blockBuffer.addAttribute(.paragraphStyle, value: p, range: NSRange(location: 0, length: blockBuffer.length))
                }
            } else {
                // pAll: все абзацы стартуют с отступа
                let pAll = NSMutableParagraphStyle()
                pAll.paragraphSpacing = options.paragraphSpacing
                pAll.lineSpacing = options.lineSpacing
                pAll.firstLineHeadIndent = indent
                pAll.headIndent = indent
                pAll.tabStops = [NSTextTab(textAlignment: .left, location: indent, options: [:])]
                pAll.defaultTabInterval = indent
                
                if blockBuffer.length > 0 {
                    blockBuffer.addAttribute(.paragraphStyle, value: pAll, range: NSRange(location: 0, length: blockBuffer.length))
                }
                
                // pFirst: первая строка первого абзаца без отступа (идёт префикс)
                let pFirst = pAll.mutableCopy() as! NSMutableParagraphStyle
                pFirst.firstLineHeadIndent = 0
                let ns = blockBuffer.string as NSString
                var firstParaRange = NSRange(location: 0, length: ns.length)
                let nl = ns.range(of: "\n")
                if nl.location != NSNotFound {
                    firstParaRange.length = nl.location + nl.length // включая \n
                }
                if firstParaRange.length > 0 {
                    blockBuffer.addAttribute(.paragraphStyle, value: pFirst, range: firstParaRange)
                }
            }
            
            // Гарантируем серый цвет префикса цитаты (если не attachment)
            if isQuote, !options.quotePrefixAsAttachment, let r = prefixRangeInBlock, r.length > 0 {
                blockBuffer.addAttribute(.foregroundColor, value: options.quotePrefixColor, range: r)
                blockBuffer.addAttribute(.font, value: options.baseFont, range: r)
            }
            
            // -------------------------------
            // Заголовок: первая строка всего контента
            // -------------------------------
            if options.makeFirstLineHeader && !didApplyHeader {
                // Пропускаем кодовые блоки (по опции)
                let blockIsSkippable = (block.type == .code) && options.headerSkipCodeBlocks
                if !blockIsSkippable {
                    // Начало первой строки — после префикса (если он есть), иначе с начала блока
                    let headerStartInBlock: Int = {
                        if let r = prefixRangeInBlock { return r.location + r.length }
                        return 0
                    }()
                    
                    // Конец первой строки — до первого '\n' или до конца блока
                    let ns = blockBuffer.string as NSString
                    let searchRange = NSRange(location: max(0, headerStartInBlock), length: ns.length - max(0, headerStartInBlock))
                    var headerEndInBlock = ns.length
                    let nl = ns.range(of: "\n", options: [], range: searchRange)
                    if nl.location != NSNotFound {
                        headerEndInBlock = nl.location
                    }
                    
                    if headerEndInBlock > headerStartInBlock {
                        let headerRange = NSRange(location: headerStartInBlock, length: headerEndInBlock - headerStartInBlock)
                        // Накладываем атрибуты заголовка поверх (font/цвет)
                        blockBuffer.addAttribute(.font, value: options.headerFont, range: headerRange)
                        if let headerColor = options.headerColor {
                            blockBuffer.addAttribute(.foregroundColor, value: headerColor, range: headerRange)
                        }
                        didApplyHeader = true
                    }
                }
            }
            
            // Вставляем блок и разделитель
            result.append(blockBuffer)
            if i < blocks.count - 1 {
                result.append(NSAttributedString(string: "\n", attributes: baseAttrs))
            }
        }
        
        return result
    }
}

// MARK: - Code block

private func appendCodeBlock(_ block: RichTextBlock,
                             to result: NSMutableAttributedString,
                             options: RichTextRenderOptions) {
    let codeText: String = {
        if let c = block.code { return c }
        var buffer = ""
        for inline in block.richTextInlines {
            switch inline.type {
            case .lineBreak: buffer.append("\n")
            case .text, .link: buffer.append(inline.string ?? "")
            }
        }
        return buffer
    }()
    
    let start = result.length
    let attrs: [NSAttributedString.Key: Any] = [
        .font: options.codeBlockFont,
        .backgroundColor: options.codeBlockBackground,
        .foregroundColor: options.codeBlockForeground ?? options.textColor
    ]
    result.append(NSAttributedString(string: codeText, attributes: attrs))
    
    let p = NSMutableParagraphStyle()
    p.firstLineHeadIndent = options.codeBlockIndent
    p.headIndent = options.codeBlockIndent
    p.lineSpacing = options.codeBlockLineSpacing
    p.paragraphSpacing = options.codeBlockParagraphSpacing
    
    let end = result.length
    if end > start {
        result.addAttribute(.paragraphStyle, value: p, range: NSRange(location: start, length: end - start))
    }
}

// MARK: - Inline атрибуты и утилиты

private func attributes(for inline: RichTextInline,
                        base options: RichTextRenderOptions) -> [NSAttributedString.Key: Any] {
    var attrs: [NSAttributedString.Key: Any] = [
        .font: options.baseFont,
        .foregroundColor: options.textColor
    ]
    
    let marks = parseMarksCSV(inline.marks)
    
    if marks.contains(.code) {
        attrs[.font] = options.codeFont
        attrs[.backgroundColor] = options.codeBackground
        if let codeFG = options.codeForeground {
            attrs[.foregroundColor] = codeFG
        }
    } else {
        let isBold = marks.contains(.bold)
        let isItalic = marks.contains(.italic)
        if isBold || isItalic {
            attrs[.font] = deriveFont(from: options.baseFont, bold: isBold, italic: isItalic)
        }
    }
    
    if marks.contains(.underline) {
        attrs[.underlineStyle] = NSUnderlineStyle.single.rawValue
    }
    if marks.contains(.strikethrough) {
        attrs[.strikethroughStyle] = NSUnderlineStyle.single.rawValue
    }
    
    if inline.type == .link, let s = inline.url, let url = URL(string: s) {
        attrs[.link] = url
        attrs[.foregroundColor] = options.linkColor
        if attrs[.underlineStyle] == nil, options.underlineLinks {
            attrs[.underlineStyle] = NSUnderlineStyle.single.rawValue
        }
    }
    
    return attrs
}

private func parseMarksCSV(_ csv: String?) -> Set<MarkType> {
    guard let csv, !csv.isEmpty else { return [] }
    let tokens = csv
        .split(separator: ",")
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
    
    var set = Set<MarkType>()
    for t in tokens {
        switch t {
        case "none":            continue
        case "bold":            set.insert(.bold)
        case "italic":          set.insert(.italic)
        case "underline":       set.insert(.underline)
        case "strikethrough":   set.insert(.strikethrough)
        case "code":            set.insert(.code)
        default:                continue
        }
    }
    return set
}

private func deriveFont(from base: UIFont, bold: Bool, italic: Bool) -> UIFont {
    var traits = base.fontDescriptor.symbolicTraits
    if bold { traits.insert(.traitBold) }
    if italic { traits.insert(.traitItalic) }
    
    if let desc = base.fontDescriptor.withSymbolicTraits(traits) {
        return UIFont(descriptor: desc, size: base.pointSize)
    }
    if bold && italic {
        let boldFont = UIFont.systemFont(ofSize: base.pointSize, weight: .semibold)
        if let desc = boldFont.fontDescriptor.withSymbolicTraits(.traitItalic) {
            return UIFont(descriptor: desc, size: base.pointSize)
        }
        return boldFont
    } else if bold {
        return UIFont.systemFont(ofSize: base.pointSize, weight: .semibold)
    } else if italic {
        return UIFont.italicSystemFont(ofSize: base.pointSize)
    } else {
        return base
    }
}

// Attachment для серой черты (если нужно «зацементировать» цвет)
private func makeQuoteBarAttachment(font: UIFont, color: UIColor,
                                    width: CGFloat,
                                    radius: CGFloat) -> NSTextAttachment {
    let height = font.lineHeight
    let size = CGSize(width: width, height: height * 0.9)
    let renderer = UIGraphicsImageRenderer(size: CGSize(width: width, height: height))
    let image = renderer.image { ctx in
        color.setFill()
        let rect = CGRect(x: 0,
                          y: (height - size.height) / 2,
                          width: size.width,
                          height: size.height)
        UIBezierPath(roundedRect: rect, cornerRadius: radius).fill()
    }
    let attach = NSTextAttachment()
    attach.image = image
    // Центрируем относительно базовой линии
    attach.bounds = CGRect(x: 0, y: font.descender, width: width, height: height)
    return attach
}
