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
            
            let authorName = NSAttributedString(
                string: "\(chat.authorName): ",
                attributes: [.font: UIFont.lastMessageInfo, .foregroundColor: UIColor.lastMessageInfo]
            )
            let messageText: NSAttributedString
            if chat.isMessage {
                let message = chat.messageText.count > 50 ? shortenString(around: searchString, in:  chat.messageText) : chat.messageText
                messageText = highlightText(in: message, substring: searchString, highlightColor: .secondColor ?? .white, font: .lastMessageInfo, fontColor: .lastMessageInfo)
            } else {
                if chat.messageText.count > 0 {
                    messageText = HelpersStrings.decodeHTML(in: chat.messageText.parseXMLToAttributedString(fontColor: .lastMessageInfo, font: .lastMessageInfo).0 ?? NSAttributedString(
                        string: chat.messageText,
                        attributes: [.font: UIFont.lastMessageInfo, .foregroundColor: UIColor.lastMessageInfo])
                    )
                } else {
                    messageText = NSAttributedString(string: "")
                }
            }
            
            var fullMessageText = NSMutableAttributedString()
            fullMessageText.append(authorName)
            fullMessageText.append(messageText)
            
            let subject: NSAttributedString
            if chat.isMessage {
                subject = NSAttributedString(
                    string: chat.subject.count > 0 ? chat.subject : "NewTicket".localizedPSD(),
                    attributes: [.font: UIFont.messageLabel, .foregroundColor: UIColor.label]
                )
            } else {
                subject = highlightText(in: chat.subject, substring: searchString, highlightColor: .secondColor ?? .white, font: .messageLabel, fontColor: .label)
            }
            
            if fullMessageText.string == ": " {
                fullMessageText = NSMutableAttributedString(string: "")
            }
            var model = SearchChatViewModel(
                id: chat.id,
                date: chat.date.messageTime(),
                subject: subject,
                messageText: fullMessageText,
                messageId: chat.messageId,
                isMessage: chat.isMessage
            )
            
            if messageText.string.count == 0, let attachment = chat.lastMessage?.attachments?.last {
                model.hasAttachments = true
                model.attachmentName = attachment.isImage ? "Photo".localizedPSD() : attachment.isVideo ? "Video".localizedPSD() : "File".localizedPSD()
            }
            
            searchChats.append(model)
        }
        
        return [searchChats]
    }
    
    func highlightText(in text: String, substring: String, highlightColor: UIColor, font: UIFont, fontColor: UIColor) -> NSAttributedString {
        let attributedString = NSMutableAttributedString(attributedString: HelpersStrings.decodeHTML(in: (text as NSString).parseXMLToAttributedString(fontColor: fontColor, font: font).0 ?? NSAttributedString(string: "")))
        
        let range = (attributedString.string as NSString).range(of: substring, options: .caseInsensitive)
        
        if range.location != NSNotFound {
            attributedString.addAttribute(.backgroundColor, value: highlightColor, range: range)
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
    static let messageLabel = CustomizationHelper.systemBoldFont(ofSize: 17)
}
