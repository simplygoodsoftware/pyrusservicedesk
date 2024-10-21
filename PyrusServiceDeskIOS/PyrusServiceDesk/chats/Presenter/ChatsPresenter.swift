import UIKit

@available(iOS 13.0, *)
class ChatsPresenter {
    weak var view: ChatsViewProtocol?
}

@available(iOS 13.0, *)
extension ChatsPresenter: ChatsPresenterProtocol {
    func doWork(_ action: ChatsPresenterCommand) {
        switch action {
        case .updateChats(chats: let chats):
            view?.show(.updateChats(chats: prepareChats(chats: chats)))
        case .openChat(chat: let chat):
            view?.show(.openChat(chat: chat))
        case .deleteFilter:
            view?.show(.deleteFilter)
        case .setFilter(userName: let userName):
            view?.show(.setFilter(userName: userName))
        case .updateMenu(actions: let actions, menuVisible: let menuVisible):
            view?.show(.updateMenus(
                filterActions: prepareActions(menuActions: actions, isFilter: true),
                openNewActions: prepareActions(menuActions: actions, isFilter: false), menuVisible: menuVisible
            ))
        case .endRefresh:
            view?.show(.endRefresh)
        }
    }
}

@available(iOS 13.0, *)
private extension ChatsPresenter {
    func prepareChats(chats: [ChatPresenterModel]) -> [ChatViewModel] {
        var models = [ChatViewModel]()
        for chat in chats {
            let subject = chat.subject?.count ?? 0 > 0
                ? chat.subject ?? ""
                : "Новое обращение"
            
            let lastMessage = chat.messages.last
            let author = lastMessage?.isInbound ?? false ? "Вы" : lastMessage?.owner.name ?? ""
            let text = lastMessage?.attachments != nil
                ? ""
                : chat.lastComment?.text ?? "Last_Message".localizedPSD()
            
            let model = ChatViewModel(
                id: chat.id,
                date: chat.date?.messageTime() ?? "",
                isRead: chat.isRead,
                subject: subject,
                lastMessageText: "\(author): \(text)",
                attachmentText: getAttachmentString(attachment: lastMessage?.attachments?.last) ?? "",
                hasAttachment: lastMessage?.attachments?.count ?? 0 > 0
            )
            
            models.append(model)
        }
        
        return models
    }
    
    func getAttachmentString(attachment: PSDAttachment?) -> String? {
        guard let attachment else { return nil }
        return attachment.isImage ? "Фото" : attachment.isVideo ? "Видео" : "Документ"
    }
    
    @available(iOS 13.0, *)
    func prepareActions(menuActions: [MenuAction], isFilter: Bool) -> [UIAction] {
        var actions = [UIAction]()
        for menuAction in menuActions {
            let image = menuAction.isSelect && isFilter ? UIImage(named: "checkmark") : nil
            let action = UIAction(title: menuAction.title, image: image, handler: { _ in
                if isFilter {
                    menuAction.filterAction()
                } else {
                    menuAction.newChatAction()
                }
            })
            actions.append(action)
        }
        return actions
    }
}
