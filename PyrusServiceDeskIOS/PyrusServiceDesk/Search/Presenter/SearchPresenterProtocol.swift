import Foundation

enum SearchPresenterCommand {
    case updateChats(chats: [SearchChatModel], searchString: String)
    case openChat(chat: PSDChat, messageId: String)
    case endRefresh
    case startRefresh
}

protocol SearchPresenterProtocol {
    func doWork(_ action: SearchPresenterCommand)
}

