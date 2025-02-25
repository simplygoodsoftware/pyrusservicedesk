
import Foundation

enum SearchViewCommand {
    case updateChats(chats: [[SearchChatViewModel]])
    case openChat(chat: PSDChat, messageId: String)
    case endRefresh
    case startRefresh
}

protocol SearchViewProtocol: NSObjectProtocol {
    func show(_ action: SearchViewCommand)
}


