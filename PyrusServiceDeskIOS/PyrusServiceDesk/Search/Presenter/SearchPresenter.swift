import UIKit

@available(iOS 13.0, *)
class SearchPresenter: NSObject {
    weak var view: SearchViewProtocol?
}

@available(iOS 13.0, *)
extension SearchPresenter: SearchPresenterProtocol {
    func doWork(_ action: SearchPresenterCommand) {
        switch action {
        case .updateChats(chats: let chats, searchString: let searchString):
            view?.show(.updateChats(chats: prepareChats(chats: chats, searchString: searchString)))
        case .openChat(chat: let chat, messageId: let messageId):
            view?.show(.openChat(chat: chat, messageId: messageId))
        case .endRefresh:
            view?.show(.endRefresh)
        case .startRefresh:
            view?.show(.startRefresh)
        }
    }
}

@available(iOS 13.0, *)
private extension SearchPresenter {
    func prepareChats(chats: [SearchChatModel], searchString: String) -> [[SearchChatViewModel]] {
        var searchChats = [SearchChatViewModel]()
        
        for chat in chats {
            let subject = chat.subject
            
            let message = chat.messageText.count > 50 ? shortenString(around: searchString, in:  chat.messageText) : chat.messageText
            
            let model = SearchChatViewModel(
                id: chat.id,
                date: chat.date.messageTime(),
                subject: subject.count > 0 ? subject : "NewTicket".localizedPSD(),
                messageText: highlightText(in: message, substring: searchString, with: .secondColor ?? .white),
                messageId: chat.messageId
            )
            
            searchChats.append(model)
        }
        
        return [searchChats]
    }
    
    func highlightText(in text: String, substring: String, with color: UIColor) -> NSAttributedString {
        let attributedString = NSMutableAttributedString(attributedString: (text as NSString).parseXMLToAttributedString(fontColor: .lastMessageInfo, font: .lastMessageInfo).0 ?? NSAttributedString(string: ""))
        
        let range = (attributedString.string as NSString).range(of: substring, options: .caseInsensitive)
        
        if range.location != NSNotFound {
            attributedString.addAttribute(.backgroundColor, value: color, range: range)
            attributedString.addAttribute(.foregroundColor, value: UIColor.label, range: range)
        }
        
        return attributedString
    }
    
    func shortenString(around substring: String, in string: String) -> String {
        let words = string.components(separatedBy: " ")
        
        guard let index = words.firstIndex(where: { $0.lowercased().contains(substring.lowercased()) }) else {
            return string
        }
        
        let startIndex = max(index - 1, 0)
        let endIndex = words.count
        let resultWords = Array(words[startIndex..<endIndex])
        let hasTruncatedWords = startIndex > 0
        var result = resultWords.joined(separator: " ")
        
        if hasTruncatedWords {
            result = "... " + result
        }
        
        return result
    }
}

private extension UIColor {
    static let secondColor = UIColor(hex: "#FFB049")?.withAlphaComponent(0.7)
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}

private extension UIFont {
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}
