import Foundation

struct MenuAction {
    let title: String
    let isSelect: Bool
    let filterAction: () -> Void
    let newChatAction: () -> Void
}

class ChatsInteractor: NSObject {
    private let presenter: ChatsPresenterProtocol
    
    private var chats = [PSDChat]() {
        didSet {
            presenter.doWork(.updateChats(chats: prepareChats()))
        }
    }
    
    private var currentUserId: String? {
        didSet {
            updateChats()
        }
    }
    
    init(presenter: ChatsPresenterProtocol) {
        self.presenter = presenter
        super.init()
    }
}

extension ChatsInteractor: ChatsInteractorProtocol {
    func doInteraction(_ action: ChatsInteractorCommand) {
        switch action {
        case .viewDidload:
            createMenuActions()
            NotificationCenter.default.addObserver(self, selector: #selector(updateChats), name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(setFilter), name: PyrusServiceDesk.usersUpdateNotification, object: nil)
        case .reloadChats:
            reloadChats()
        case .selectChat(index: let index):
            PyrusServiceDesk.currentUserId = chats[index].userId
            presenter.doWork(.openChat(chat: chats[index]))
        case .newChat:
            openNewChat()
        case .deleteFilter:
            deleteFilter()
        case .viewWillAppear:
            PyrusServiceDesk.currentUserId = nil
        }
    }
}

private extension ChatsInteractor {
    
    func deleteFilter() {
        PyrusServiceDesk.currentUserId = nil
        currentUserId = nil
        updateChats()
        createMenuActions()
    }
    
    func reloadChats() {
        PyrusServiceDesk.restartTimer()
        
        DispatchQueue.global().async { [weak self] in
            PSDGetChats.get() { [weak self] chats in
                DispatchQueue.main.async {
                    self?.presenter.doWork(.endRefresh)
                }
            }
        }
    }
    
    func openNewChat(userId: String? = nil) {
        let chat = PSDChat(chatId: 0, date: Date(), messages: [])
        chat.subject = "Новое обращение"
        chat.userId = userId
        PyrusServiceDesk.currentUserId = userId
        presenter.doWork(.openChat(chat: chat))
    }
    
    func prepareChats() -> [ChatPresenterModel] {
        return chats.map({ ChatPresenterModel(
            id: $0.chatId ?? 0,
            date: $0.date,
            isRead: $0.isRead,
            subject: $0.subject,
            lastComment: $0.lastComment,
            messages: $0.messages
        )})
    }
    
    @objc func updateChats() {
        let filterChats = currentUserId == nil
        ? PyrusServiceDesk.chats
        : PyrusServiceDesk.chats.filter({ $0.userId == currentUserId })
        if chats != filterChats {
            DispatchQueue.main.async {
                self.chats = filterChats
            }
        }
    }
    
    @objc func setFilter() {
        guard let userId = PyrusServiceDesk.currentUserId,
              PyrusServiceDesk.additionalUsers.count > 0
        else {
            createMenuActions()
            return
        }
        
        currentUserId = PyrusServiceDesk.currentUserId
        updateChats()
        let userName = PyrusServiceDesk.additionalUsers
            .first(where: {$0.userId == userId})?.userName ?? PyrusServiceDesk.userName
        DispatchQueue.main.async {
            self.presenter.doWork(.setFilter(userName: userName ?? ""))
            self.createMenuActions()
        }
    }
    
    func createMenuActions() {
        var actions = [MenuAction]()
        var users = PyrusServiceDesk.additionalUsers
        let user = PSDUserInfo(
            appId: PyrusServiceDesk.clientId ?? "",
            clientName: PyrusServiceDesk.clientName ?? "",
            userId: PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId,
            userName: PyrusServiceDesk.userName ?? "",
            secretKey: PyrusServiceDesk.securityKey ?? ""
        )
        if !users.contains(user) {
            users.append(user)
        }
       
        for user in users {
            let userId = user.userId
            let filterAction = {
                PyrusServiceDesk.currentUserId = userId
                self.currentUserId = userId
                self.presenter.doWork(.setFilter(userName: user.userName))
                self.createMenuActions()
            }
            let openNewAction = {
                self.openNewChat(userId: userId)
            }
            let menuAction = MenuAction(
                title: user.userName,
                isSelect: userId == PyrusServiceDesk.currentUserId,
                filterAction: filterAction,
                newChatAction: openNewAction
            )
            actions.append(menuAction)
        }
        presenter.doWork(.updateMenu(actions: actions, menuVisible: PyrusServiceDesk.additionalUsers.count > 0))
    }
}
