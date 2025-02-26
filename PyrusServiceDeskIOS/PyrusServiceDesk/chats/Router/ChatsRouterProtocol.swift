import Foundation

protocol ChatsRouterProtocol: NSObjectProtocol {
    func route(to destination: ChatsRouterDestination)
}

enum ChatsRouterDestination {
    case chat(chat: PSDChat, fromPush: Bool)
}
