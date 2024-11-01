import Foundation

protocol PSDChatRouterProtocol: NSObjectProtocol {
    func route(to destination: PSDChatRouterDestination)
}

enum PSDChatRouterDestination {
    case showLinkOpenAlert(linkString: String)
    case close
}
