import UIKit

@available(iOS 13.0, *)
final class ChatsRouter: NSObject {
    weak var controller: PSDChatsViewController?
}

@available(iOS 13.0, *)
extension ChatsRouter: ChatsRouterProtocol {
    func route(to destination: ChatsRouterDestination) {
        switch destination {
        case .chat(chat: let chat):
            openChat(chat: chat)
        case .goBack:
            controller?.navigationController?.popViewController(animated: true)
        }
    }
}

@available(iOS 13.0, *)
private extension ChatsRouter {
    func openChat(chat: PSDChat) {
        let pyrusChat = PSDChatViewController()
        pyrusChat.chat = chat
        pyrusChat.ticketId = chat.chatId ?? 0
        controller?.navigationController?.pushViewController(pyrusChat, animated: true)
    }
}

