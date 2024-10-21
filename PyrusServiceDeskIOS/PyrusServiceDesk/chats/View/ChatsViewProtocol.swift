import UIKit

@available(iOS 13.0, *)
enum ChatsSearchViewCommand {
    case updateChats(chats: [ChatViewModel])
    case openChat(chat: PSDChat)
    case deleteFilter
    case setFilter(userName: String)
    case updateMenus(filterActions: [UIAction], openNewActions: [UIAction], menuVisible: Bool)
    case endRefresh
}

@available(iOS 13.0, *)
protocol ChatsViewProtocol: NSObjectProtocol {
    func show(_ action: ChatsSearchViewCommand)
}
