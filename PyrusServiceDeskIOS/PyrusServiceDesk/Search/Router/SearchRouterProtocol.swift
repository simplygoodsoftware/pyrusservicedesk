import Foundation

protocol SearchRouterProtocol: NSObjectProtocol {
    func route(to destination: SearchRouterDestination)
}

enum SearchRouterDestination {
    case chat(chat: PSDChat, messageId: String)
}
