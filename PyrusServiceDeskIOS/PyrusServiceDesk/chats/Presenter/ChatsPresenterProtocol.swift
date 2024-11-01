enum ChatsPresenterCommand {
    case updateChats(chats: [ChatPresenterModel])
    case openChat(chat: PSDChat)
    case deleteFilter
    case setFilter(userName: String)
    case updateMenu(actions: [MenuAction], menuVisible: Bool)
    case endRefresh
    case updateTitle(title: String?)
    case updateTitles(titles: [String], selectedIndex: Int)
    case updateSelected(index: Int)
}

protocol ChatsPresenterProtocol {
    func doWork(_ action: ChatsPresenterCommand)
}

