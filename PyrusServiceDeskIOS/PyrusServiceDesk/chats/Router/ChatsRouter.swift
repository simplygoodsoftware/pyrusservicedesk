import UIKit

@available(iOS 13.0, *)
final class ChatsRouter: NSObject {
    weak var controller: PSDChatsViewController?
}

@available(iOS 13.0, *)
extension ChatsRouter: ChatsRouterProtocol {
    func route(to destination: ChatsRouterDestination) {
        switch destination {
        case .goBack:
            controller?.navigationController?.popViewController(animated: true)
        case .chat(chat: let chat, fromPush: let fromPush):
            openChat(chat: chat, fromPush: fromPush)
        }
    }
}

@available(iOS 13.0, *)
private extension ChatsRouter {
    func openChat(chat: PSDChat, fromPush: Bool) {
        let presenter = PSDChatPresenter()
        let interactor = PSDChatInteractor(presenter: presenter, chat: chat, fromPush: fromPush)
        let router = PSDChatRouter()
        let pyrusChat = PSDChatViewController(interactor: interactor, router: router)
        presenter.view = pyrusChat
        router.controller = pyrusChat
        controller?.navigationController?.pushViewController(pyrusChat, animated: !fromPush)
    }
}

