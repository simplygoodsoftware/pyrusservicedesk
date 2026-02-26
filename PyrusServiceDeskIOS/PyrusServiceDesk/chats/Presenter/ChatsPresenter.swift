import UIKit

@available(iOS 13.0, *)
class ChatsPresenter: NSObject {
    private let chatsPrepareQueue = DispatchQueue.init(label: "prepareChats")

    weak var view: ChatsViewProtocol?
    private var isClosedTicketsOpened: Bool = false
    private var chats = [ChatPresenterModel]()
    private var newAnnouncementsInfo: NewAnnouncementsInfo?
}

@available(iOS 13.0, *)
extension ChatsPresenter: ChatsPresenterProtocol {
    func doWork(_ action: ChatsPresenterCommand) {
        switch action {
        case .updateChats(chats: let chats, newAnnouncementsInfo: let newAnnouncementsInfo):
            let startTime = DispatchTime.now()
            self.chats = chats
            self.newAnnouncementsInfo = newAnnouncementsInfo
            updateChats(newAnnouncementsInfo: newAnnouncementsInfo)
            let endTime = DispatchTime.now()
            let nanoTime = endTime.uptimeNanoseconds - startTime.uptimeNanoseconds
            let timeInterval = Double(nanoTime) / 1_000_000
//            print("⏱ updateChats выполнена за \(timeInterval) мс (обработано \(chats.count) чатов)")
        case .openChat(chat: let chat, fromPush: let fromPush):
            view?.show(.openChat(chat: chat, fromPush: fromPush))
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
        case .updateTitle(title: let title):
            view?.show(.updateTitle(title: title ?? "All_Conversations".localizedPSD()))
        case .updateTitles(titles: let titles, selectedIndex: let selectedIndex):
            let titles = titles.map({ TitleWithBadge(title: $0) })
            view?.show(.updateTitle(title: "All_Conversations".localizedPSD()))
            view?.show(.updateTitles(titles: titles, selectedIndex: selectedIndex))
        case .updateSelected(index: let index):
            view?.show(.updateSelected(index: index))
        case .updateIcon(image: let image):
            view?.show(.updateIcon(image: image ?? UIImage.PSDImage(name: "iiko")))
        case .showAccessDeniedAlert(userNames: let userNames, doExit: let doExit):
            let okAction = UIAlertAction(title: "Ок", style: .default, handler: {_ in 
                if doExit {
                    PyrusServiceDesk.mainController?.closeServiceDesk()
                }
            })
            view?.show(.showAccessDeniedAlert(userNames: userNames, okAction: okAction))
        case .deleteSegmentControl:
            view?.show(.deleteSegmentControl)
        case .startRefresh:
            view?.show(.startRefresh)
        case .connectionError:
            view?.show(.connectionError)
        case .createChatsOnStart(chats: let chats):
            self.chats = chats
            loadChatsState()
        case .openAnnouncements:
            view?.show(.openAnnouncements)
        }
    }
}

private extension ChatsPresenter {
    func updateChats(open: Bool = false, newAnnouncementsInfo: NewAnnouncementsInfo?) {
        var chatsModel = self.prepareChats(chats: self.chats)

        if let newAnnouncementsInfo {
            let annsModel = getNewAnnouncementsModel(newAnnouncementsInfo: newAnnouncementsInfo)
            chatsModel[0] = annsModel + chatsModel[0]
        }
        self.view?.show(.updateChats(chats: chatsModel))
        if open {
            self.view?.show(.scrollToClosedTickets)
        }
    }
    
    private func loadChatsState() {
        let chatsModel = self.prepareChats(chats: self.chats)
        self.view?.show(.updateChats(chats: chatsModel))
    }
    
    func getNewAnnouncementsModel(newAnnouncementsInfo: NewAnnouncementsInfo) -> [PSDChatsViewModel] {
        var title: String = ""
        var subtitle: String = ""
        
        let count = newAnnouncementsInfo.newAnnouncementsCount
        if count > 1 {
            subtitle = "\(count) \(getNewAnnsString(count: count))"
        } else {
            let text = newAnnouncementsInfo.lastAnnouncement?.text ?? ""
            subtitle = text.count > 0 ? text : "\(count) \(getNewAnnsString(count: count))"
        }
        
        if newAnnouncementsInfo.clients.count == 2 {
            title = "\(newAnnouncementsInfo.clients[0].clientName) \("And".localizedPSD()) \(newAnnouncementsInfo.clients[1].clientName)"
        } else {
            title = newAnnouncementsInfo.clients.map(\.clientName).joined(separator: ", ")
        }
        
        let anns = NewAnnouncementsModel(
            logos: newAnnouncementsInfo.clients.map({ ($0.image ?? UIImage(named: "iiko")) ?? UIImage() }),
            title: title,
            subtitle: subtitle
        )
        
        return [PSDChatsViewModel(data: anns, type: .announcements)]
    }
    
    func getNewAnnsString(count: Int) -> String {
        if (count % 10 == 1 && count % 100 != 11) {
            return "NewAnnouncements1".localizedPSD()
        } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
            return "NewAnnouncements2".localizedPSD()
        }
        return "NewAnnouncements5".localizedPSD()
    }
    
    func prepareChats(chats: [ChatPresenterModel]) -> [[PSDChatsViewModel]] {
        let startTime = DispatchTime.now()
        
        var activeChats = [PSDChatsViewModel]()
        var closeChats = [PSDChatsViewModel]()
        
        for chat in chats {
            let subject = chat.subject == ""
            ? "NewTicket".localizedPSD()
            : chat.subject ?? "NewTicket".localizedPSD()
                
            var lastMessage = chat.lastComment
            
            var text = chat.lastMessageAttributedText ?? AttributedStringCache.cachedString(for: lastMessage?.text ?? "", fontColor: .lastMessageInfo, font: .lastMessageInfo, key: lastMessage?.messageId).string
            if let lastStoreMessage = PSDMessagesStorage.getMessages(for: chat.id).last {
                if lastStoreMessage.date >= lastMessage?.date ?? Date() {
                    lastMessage = lastStoreMessage
                    text = lastMessage?.text ?? ""
                }
            }
           
            let author = lastMessage?.isOutgoing ?? false ? "You".localizedPSD() : lastMessage?.owner?.name ?? ""
            if lastMessage?.attachments?.count ?? 0 > 0 {
                text = ""
            }
            
            let model = ChatViewModel(
                id: chat.id,
                date: chat.date ?? "",
                isRead: chat.isRead,
                subject: subject,
                lastMessageText: text,
                attachmentText: getAttachmentString(attachment: lastMessage?.attachments?.last) ?? "",
                hasAttachment: lastMessage?.attachments?.count ?? 0 > 0,
                state: lastMessage?.state ?? .sent,
                isAudio: lastMessage?.attachments?.first?.isAudio ?? false,
                lastMessageId: lastMessage?.messageId,
                lastMessageCommandId: lastMessage?.commandId,
                lastMessageAuthor: author//lastMessage?.messageId ?? lastMessage?.commandId ?? "",
            )
            
            if chat.isActive {
                activeChats.append(PSDChatsViewModel(data: model, type: .chat))
            } else {
                closeChats.append(PSDChatsViewModel(data: model, type: .chat))
            }
        }
        
        if closeChats.count > 0 {
            activeChats.append(PSDChatsViewModel(data: ClosedTicketsCellModel(count: closeChats.count, isOpen: isClosedTicketsOpened, delegate: self), type: .header) )
        }
        
        let endTime = DispatchTime.now()
        let nanoTime = endTime.uptimeNanoseconds - startTime.uptimeNanoseconds
        let timeInterval = Double(nanoTime) / 1_000_000
//        print("⏱ prepareChats выполнена за \(timeInterval) мс (обработано \(chats.count) чатов)")
        
        if isClosedTicketsOpened {
            return [activeChats, closeChats]
        } else {
            return [activeChats, []]
        }
    }
    
    func removeLinkAttributes(from attributedString: NSAttributedString?) -> NSAttributedString? {
        guard let attributedString else { return nil }
        let mutableAttributedString = NSMutableAttributedString(attributedString: attributedString)
        let range = NSRange(location: 0, length: mutableAttributedString.length)
        
        mutableAttributedString.enumerateAttribute(.link, in: range, options: []) { value, range, _ in
            if value != nil {
                mutableAttributedString.removeAttribute(.link, range: range)
            }
        }
        
        return mutableAttributedString
    }
    
    func getAttachmentString(attachment: PSDAttachment?) -> String? {
        guard let attachment else { return nil }
        if attachment.isImage {
            return "Photo".localizedPSD()
        } else if attachment.isVideo {
            return "Video".localizedPSD()
        } else if attachment.isAudio {
            return "Audio".localizedPSD()
        } else {
            return "File".localizedPSD()
        }
    }
    
    @available(iOS 13.0, *)
    func prepareActions(menuActions: [MenuAction], isFilter: Bool) -> [UIAction] {
        var actions = [UIAction]()
        if isFilter {
            let image = PyrusServiceDesk.currentUserId == nil ? UIImage(systemName: "checkmark") : nil
            let action = UIAction(title: "All".localizedPSD(), image: image, handler: { [weak self] _ in
                self?.view?.show(.deleteFilter)
            })
            actions.append(action)
        }
        
        for menuAction in menuActions {
            let image = menuAction.isSelect && isFilter ? UIImage(systemName: "checkmark") : nil
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

@available(iOS 13.0, *)
extension ChatsPresenter: ClosedTicketsCellDelegate {
    func redrawChats(open: Bool) {
        isClosedTicketsOpened = open
        updateChats(open: open, newAnnouncementsInfo: newAnnouncementsInfo)
    }
}

private extension UIFont {
    static let timeLabel = CustomizationHelper.systemFont(ofSize: 13.0)
    static let messageLabel = CustomizationHelper.systemBoldFont(ofSize: 17)
    static let notificationButton = CustomizationHelper.systemFont(ofSize: 12.0)
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}

private extension UIColor {
    static let timeLabel = UIColor(hex: "#9199A1") ?? .systemGray
    static let secondColor = UIColor(hex: "#FFB049")
    
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}
