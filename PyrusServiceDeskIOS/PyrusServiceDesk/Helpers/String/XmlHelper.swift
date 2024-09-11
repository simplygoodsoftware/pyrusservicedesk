import UIKit

enum HTMLTag: CaseIterable {
    case a
    case button
    case lineBreak
    ///Возвращает стрингу которая должна быть внутри тега
    var tag: String {
        switch self {
        case .a:
            return "a"
        case .button:
            return "button"
        case .lineBreak:
            return "br"
        }
    }
    ///Может ли тег существовать без параметров?
    var mayBeWithoutParameters: Bool {
        switch self {
        case .a:
            return false
        default: return true
        }
    }
    ///Если необходимо заменить на что-то, кроме пустого места
    func replaced(data: Any?) -> String {
        switch self {
        case .a:
            return ""
        case .button:
            return ""
        case .lineBreak:
            return "\n"
        }
    }
    ///Если необходимо заменить на что-то строчку внутри тега
    func replaceString() -> Bool {
        switch self {
        case .button:
            return true
        default: return false
        }
    }
    ///Для хранения идентификатора типа текста в комментарии в ключе отправляемой библиотеки
    var resultDictKey: TypeOfText {
        return .text
    }
}

enum TypeOfText: Int {
    case text = 0
}

extension NSString {
    /// Преобразует текст из **HTML** разметки в массив библиотек, **ключ** в которых - тип текста, а **значение** - сам NSAttrinbutedString
    /// - Parameter needDetectLink: Хак для ускорения индексации, по дефолту true, можно не заполнять
    /// - Parameter needMention: Хак для черновика и редактирования комментария, по дефолту true, можно не заполнять
    func parseXMLToAttributedString(needDetectLink: Bool = true,
                                    needMention: Bool = true,
                                    fontColor: UIColor) -> (NSAttributedString?, [String]?) {
        return parseXMLToAttributedString(needDetectLink: needDetectLink, needMention: needMention, font: .messageTextView, fontColor: fontColor)
    }
    
    /// Преобразует текст из **HTML** разметки в массив библиотек, **ключ** в которых - тип текста, а **значение** - сам NSAttrinbutedString
    /// - Parameter needDetectLink: Хак для ускорения индексации, по дефолту true, можно не заполнять
    /// - Parameter needMention: Хак для черновика, по дефолту true, можно не заполнять
    func parseXMLToAttributedString(needDetectLink: Bool = true,
                                    needMention: Bool = true,
                                    font: UIFont,
                                    fontColor: UIColor) -> (NSAttributedString?, [String]?) {
        ///Массив кнопок из тегов
        var buttons = [String]()
        ///Массив наших тегов.
        var tags = [HTMLTag: (count: Int, data: Any?)]()
        ///Откуда начинается наш AttributedString при смене типа текста.
        let lastRemoveLocation = 0
        ///Где был последний тег в тексте.
        ///Помогает обработать случай когда теги не закрываются (но их применение продолжается).
        var lastTagLocation = 0
        ///Формирует массив атрибутов для стринги в зависимости от накопленных тегов
        func getAttr(for tags: [HTMLTag: (count: Int, data: Any?)]) -> [NSAttributedString.Key: Any] {
            ///Результирующий словарь аттрибутов
            var dict = [NSAttributedString.Key: Any]()
            dict.updateValue(font, forKey: .font)
            dict.updateValue(fontColor, forKey: .foregroundColor)
            //В зависимости от тега лежащего в нашем массиве мы добавляем аттрибуты
            for (tag, info) in tags {
                switch tag {
                case .a:
                    if let link = info.data as? URL {
                        dict.updateValue(link, forKey: .link)
                    }
                default:
                    continue
                }
            }
            return dict
        }
        
        ///Добавляем тег в наш массив (или удаляем)
        func addAndReturn(tag str: String) -> (tagType: HTMLTag, isOpen: Bool, data: Any?)? {
            if let tag = str.getHTMLTagForString() {
                if tag.isOpen {
                    if tag.tagType != .lineBreak {
                        //place for listItem
                        tags.updateValue((tags[tag.tagType]?.count ?? 0 + 1, tag.data), forKey: tag.tagType)
                    }
                } else if let value = tags[tag.tagType]?.count {
                    if value == 1 {
                        tags.removeValue(forKey: tag.tagType)
                    } else {
                        tags.updateValue((value - 1, tag.data), forKey: tag.tagType)
                    }
                } else { return nil }
                return tag
            }
            return nil
        }
        
        var str = self
        let attrStr = NSMutableAttributedString(string: String(str))
        
        var range = NSRange(location: 0, length: str.length)
        
        while range.location != NSNotFound && range.length > 0 {
            //Ищем теги в нашей строке
            let tagRange = (str as NSString).range(of: String.tagRegex, options: .regularExpression, range: range)
            var removedString = false
            if tagRange.location != NSNotFound {
                lastTagLocation = tagRange.location
                //Получаем стрингу внутри тега (без "<" и ">")
                let tagString = (str as NSString).substring(with: NSRange(location: tagRange.location + 1, length: tagRange.length - 2))
                //Получаем range строки до текущего тега от прошлого тега
                let addAttrRange = NSRange(location: range.location, length: tagRange.location - range.location)
                //Добавляем атрибуты в найденый Range
                attrStr.addAttributes(getAttr(for: tags), range: addAttrRange)
                if let tag = addAndReturn(tag: tagString) {
                    //Удаляем тег из строки
                    let replaceString = tag.isOpen ? tag.tagType.replaced(data: tag.data) : ""
                    str = str.replacingCharacters(in: tagRange, with: replaceString) as NSString
                    attrStr.replaceCharacters(in: tagRange, with: replaceString)
                    if tag.tagType == .button && !tag.isOpen {
                        let button = str.substring(with: addAttrRange)
                        if button.count > 0 {
                            if button.trimmingCharacters(in: .whitespacesAndNewlines).count > 0 {//Добавлеям кнопки только если в них есть текст, иначе удаляем
                                buttons.append(button)
                            }
                            let replaceString = ""
                            str = str.replacingCharacters(in: addAttrRange, with: replaceString) as NSString
                            attrStr.replaceCharacters(in: addAttrRange, with: replaceString)
                            removedString = true
                        }
                    }
                } else {
                    //Если мы не знаем такой тег, то с чистой совестью удаляем его
                    str = str.replacingCharacters(in: tagRange, with: "") as NSString
                    attrStr.replaceCharacters(in: tagRange, with: "")
                }
                if removedString {
                    lastTagLocation = addAttrRange.location
                    //Обновляем границы поиска до начала тега который нашли (потому что мы его удалили)
                    range = NSRange(location: addAttrRange.location, length: str.length - addAttrRange.location)
                }
            }
            if !removedString {
                //Обновляем границы поиска до начала тега который нашли (потому что мы его удалили)
                range = NSRange(location: tagRange.location, length: str.length - tagRange.location)
            }
        }
        
        //Применяем аттрибуты для последнего отрывка нашей строки
        if lastTagLocation != str.length {
            attrStr.addAttributes(getAttr(for: tags), range: NSRange(location: lastTagLocation, length: str.length - lastTagLocation))
        }
        //Добавляем последний отрывок нашей строки (если он есть) в результирующий массив
        let subStrRange = NSRange(location: lastRemoveLocation, length: str.length - lastRemoveLocation)
        let attrSubStr = NSMutableAttributedString(attributedString: attrStr.attributedSubstring(from: subStrRange))
        if subStrRange.length > 0 {
            if needDetectLink {
                return (HelpersStrings.attributedString(byDecodingHTMLEntities: attrSubStr)
                                                .detectEmail()
                                                .detectLink()
                                                .detectPhoneNumber(), buttons)
             } else {
                 return (HelpersStrings.attributedString(byDecodingHTMLEntities: attrSubStr), buttons)
             }
        }
        return (nil, buttons)
    }
    
    ///Поиск электронной почты
    static let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
}

///Перечесление для тегов с параметрами
enum HardTagType: CaseIterable {
    case href
    ///Строка для идентефикации типа тега с параметрами
    var pattern: String {
        switch self {
        case .href:
            return "href=\""
        }
    }
    var parametr: String {
        return ""
    }
}

extension String {
    ///Определяем что же за тег нам пришел
    func getHTMLTagForString() -> (tagType: HTMLTag, isOpen: Bool, data: Any?)? {
        //Если тег без параметров, он должен определяться по первой проверке
        //Если тег без параметров и закрывающий, он должен определяться по второй проверке
        var check = HTMLTag.allCases.filter { $0.tag == self && $0.mayBeWithoutParameters }
        if check.first != nil { return (check.first!, true, nil) }
        check = HTMLTag.allCases.filter { "/\($0.tag)" == self }
        if check.first != nil { return (check.first!, false, nil) }
        //Если тег с параметрами тогда первые проверки не идентифицируют его
        //Используем поиск по параметрам которые уникальные для определенного тега
        for type in HardTagType.allCases {
            if self.contains(type.pattern) {
                switch type {
                case .href:
                    return (HTMLTag.a, true, searchURLParam(type.parametr))
                }
            }
        }
        return nil
    }
    
    ///Ищем в строке тега параметр типа String
    func searchStringParam(name: String) -> String? {
        let str = self as NSString
        let rangeOfStartId = str.range(of: "\(name)=\"", options: .literal)
        if rangeOfStartId.location == NSNotFound { return nil }
        let rangeOfCloseStartId = str.range(of: "\"", options: .literal,
                                            range: NSRange(location: rangeOfStartId.location + rangeOfStartId.length, length: str.length - (rangeOfStartId.location + rangeOfStartId.length)))
        if rangeOfCloseStartId.location == NSNotFound { return nil }
        return str.substring(with: NSRange(location: rangeOfStartId.location + rangeOfStartId.length, length: rangeOfCloseStartId.location - (rangeOfStartId.location + rangeOfStartId.length)))
    }
    
    func searchURLParam(_ name: String) -> URL? {
        guard let param = searchStringParam(name: name) else {
            return nil
        }
        return URL(string: param)
    }
    
    ///Возвращает фактические размеры строки
    func size(OfFont font: UIFont) -> CGSize {
        return (self as NSString).size(withAttributes: [.font: font])
    }
    
    ///Поиск любого тега ограниченного угловыми скобками <tag>
    static let tagRegex = "<[^>]*>"
    
    ///Набор символов для фильтрации номера
    static let telephoneCharacters = "+0(123)456-789"
}

extension NSMutableAttributedString {
    ///Обнаружить и добавить атрибуты для электронной почты
    func detectEmail() -> NSMutableAttributedString {
        let attrStr = self
        let str = NSString(string: self.string)
        var range = str.range(of: String(NSString.emailRegex), options: .regularExpression)
        while range.location != NSNotFound {
            attrStr.addAttributes([.link: "mailto:\(str.substring(with: range))"], range: range)
            range = str.range(of: String(NSString.emailRegex), options: .regularExpression,
                              range: NSRange(location: range.location + range.length, length: str.length - (range.location + range.length)))
        }
        return attrStr
    }
    
    ///Обнаружить и добавить атрибуты для обычных ссылок
    func detectLink() -> NSMutableAttributedString {
        let attrStr = self
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else { return attrStr }
        let matches = detector.matches(in: attrStr.string, options: [], range: NSRange(location: 0, length: attrStr.length))
        for match in matches {
            guard let range = Range(match.range, in: attrStr.string) else { continue }
            let nsRange = NSRange(range, in: attrStr.string)
            
            // Хак для якорных объектов в ссылке
            var charSet = CharacterSet.urlQueryAllowed
            charSet.insert(charactersIn: "#%")
            
            guard let linkStr = attrStr.string[range].addingPercentEncoding(withAllowedCharacters: charSet), let url = URL(string: linkStr) else { continue }
            if attrStr.attributes(at: nsRange.location, effectiveRange: nil)[.link] != nil { continue }
            if url.scheme == nil {
                attrStr.addAttributes([.link: "https://\(linkStr)"], range: nsRange)
            } else {
                attrStr.addAttributes([.link: linkStr], range: nsRange)
            }
            
        }
        return attrStr
    }
    
    ///Обнаружить и добавить атрибуты для номера телефона
    func detectPhoneNumber() -> NSMutableAttributedString {
        let attrStr = self
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.phoneNumber.rawValue) else { return attrStr }
        let matches = detector.matches(in: attrStr.string, options: [], range: NSRange(location: 0, length: attrStr.length))
        for match in matches {
            guard let range = Range(match.range, in: attrStr.string) else { continue }
            let nsRange = NSRange(range, in: attrStr.string)
            if attrStr.attributes(at: nsRange.location, effectiveRange: nil)[.link] != nil { continue }
            let number = attrStr.string[range].filter { String.telephoneCharacters.contains($0) }
            attrStr.addAttributes([.link: "tel:\(number)"], range: nsRange)
        }
        return attrStr
    }
}
