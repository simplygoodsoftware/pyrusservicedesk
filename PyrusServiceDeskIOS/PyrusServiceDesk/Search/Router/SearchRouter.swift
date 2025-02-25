import UIKit

@available(iOS 13.0, *)
final class SearchRouter: NSObject {
    weak var controller: SearchViewController?
}

@available(iOS 13.0, *)
extension SearchRouter: SearchRouterProtocol {
    func route(to destination: SearchRouterDestination) {
        switch destination {
        case .chat(chat: let chat, messageId: let messageId):
            openChat(chat: chat, messageId: messageId)
        }
    }
}

@available(iOS 13.0, *)
private extension SearchRouter {
    func openChat(chat: PSDChat, messageId: String) {
        let presenter = PSDChatPresenter()
        let interactor = PSDChatInteractor(presenter: presenter, chat: chat, messageId: messageId)
        let router = PSDChatRouter()
        let pyrusChat = PSDChatViewController(interactor: interactor, router: router)
        presenter.view = pyrusChat
        router.controller = pyrusChat
        controller?.navigationController?.pushViewController(pyrusChat, animated: true)
    }
}
