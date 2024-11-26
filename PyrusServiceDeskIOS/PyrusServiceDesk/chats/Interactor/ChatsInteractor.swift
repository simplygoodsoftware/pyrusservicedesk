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
    
    var selectedIndex = 0
    var isNewQr = false
    var pushTicketId: Int?
    var pushUserId: String?
    private var clients = [PSDClientInfo]()
    
    private var currentUserId: String? {
        didSet {
            updateChats()
        }
    }
    
    init(presenter: ChatsPresenterProtocol, pushTicketId: Int? = nil, pushUserId: String? = nil) {
        self.presenter = presenter
        self.pushTicketId = pushTicketId//256426849//pushTicketId
        self.pushUserId = pushUserId//"251380446"//pushUserId
        super.init()
    }
}

extension ChatsInteractor: ChatsInteractorProtocol {
    func doInteraction(_ action: ChatsInteractorCommand) {
        switch action {
        case .viewDidload:
            if let pushTicketId, let pushUserId,
               let clientId = getUsers().first(where: { $0.userId == pushUserId })?.clientId {
                presenter.doWork(.endRefresh)
                PyrusServiceDesk.currentUserId = pushUserId
                PyrusServiceDesk.currentClientId = clientId
                let chat = PSDChat(chatId: pushTicketId, date: Date(), messages: [])
                openChat(chat: chat, fromPush: true)
            }
            createMenuActions()
            NotificationCenter.default.addObserver(self, selector: #selector(updateChats), name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(updateClients), name: PyrusServiceDesk.clientsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(setFilter), name: PyrusServiceDesk.usersUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(forName: SyncManager.updateAccessesNotification, object: nil, queue: .main) { [weak self] notification in
                if let userInfo = notification.userInfo,
                   let isFilter = userInfo["isFilter"] as? Bool {
                    self?.denyAccesses(isFilter: isFilter)
                }
            }
        case .reloadChats:
            reloadChats()
        case .selectChat(index: let index):
            openChat(chat: chats[index], fromPush: false)
        case .newChat:
            if let clientId = PyrusServiceDesk.currentClientId {
                if let userId = PyrusServiceDesk.currentUserId {
                    openNewChat(userId: userId)
                } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.clientId == clientId }) {
                    openNewChat(userId: user.userId)
                } else {
                    openNewChat()
                }
            } else {
                openNewChat()
            }
        case .deleteFilter:
            deleteFilter()
        case .viewWillAppear:
            PyrusServiceDesk.currentUserId = nil
        case .updateSelected(index: let index):
            updateSelected(index: index)
        }
    }
}

private extension ChatsInteractor {
    
    func openChat(chat: PSDChat, fromPush: Bool) {
        PyrusServiceDesk.currentUserId = chat.userId
        let params = TicketCommandParams(ticketId: chat.chatId ?? 0, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId:  PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId, params: params)
        PyrusServiceDesk.repository.add(command: command)
        PyrusServiceDesk.syncManager.syncGetTickets()
        presenter.doWork(.openChat(chat: chat, fromPush: fromPush))
    }
    
    @objc func denyAccesses(isFilter: Bool) {
        var userNames = ""
        for userId in PyrusServiceDesk.accessDeniedIds {
          //  if PyrusServiceDesk.currentUserId == userId {
         //   }
            if PyrusServiceDesk.customUserId == userId {
                userNames += "\(PyrusServiceDesk.userName ?? ""), "
                if PyrusServiceDesk.additionalUsers.count > 0 {
                    let user = PyrusServiceDesk.additionalUsers.last
                    PyrusServiceDesk.customUserId = user?.userId
                    PyrusServiceDesk.clientId = user?.clientId
                    PyrusServiceDesk.clientName = user?.clientName
                    PyrusServiceDesk.userName = user?.userName
                    PyrusServiceDesk.additionalUsers.removeLast()
                } else {
                    userNames = String(userNames.dropLast(2))
                    DispatchQueue.main.async { [weak self] in
                        if isFilter {
                            self?.presenter.doWork(.showAccessDeniedAlert(userNames: userNames, doExit: true))
                        }
                    }
                    return
                }
            } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == userId }) {
                userNames += "\(user.userName), "
                PyrusServiceDesk.additionalUsers.removeAll(where: { $0.userId == userId })
            }
        }
        
        DispatchQueue.main.async { [weak self] in
            self?.deleteFilter()
            self?.presenter.doWork(.deleteFilter)
        }
        
        DispatchQueue.main.async { [weak self] in
            self?.createMenuActions()
            userNames = String(userNames.dropLast(2))
            if isFilter {
                self?.presenter.doWork(.showAccessDeniedAlert(userNames: userNames, doExit: false))
            }
        }
    }
    
    func loadImage(urlString: String, completion: @escaping (_ image: UIImage?) -> Void){
        let url = PyrusServiceDeskAPI.PSDURL(url: urlString)
        PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
            guard let data = data, error == nil else{
                completion(nil)
                return
            }
            if data.count != 0 {
                let image = UIImage(data: data)
                completion(image)
            }
            else{
                completion(nil)
            }
        }.resume()
    }
    
    func updateIcon(imagePath: String, index: Int) {
        if let image = clients[index].image {
            presenter.doWork(.updateIcon(image: image))
        }
        loadImage(urlString: imagePath) { [weak self] image in
            self?.clients[index].image = image ?? UIImage.PSDImage(name: "iiko")
            PyrusServiceDesk.clients[index].image = image ?? UIImage.PSDImage(name: "iiko")
            DispatchQueue.main.async { [weak self] in
                self?.presenter.doWork(.updateIcon(image: image))
            }
        }
    }
    
    func updateSelected(index: Int) {
        if index < clients.count {
            selectedIndex = index
            presenter.doWork(.deleteFilter)
            PyrusServiceDesk.currentClientId = clients[index].clientId
            createMenuActions()
            updateChats()
            
            updateIcon(imagePath: clients[index].clientIcon, index: index)
        }
    }
    
    @objc func updateClients() {
        guard clients != PyrusServiceDesk.clients else { return }
        DispatchQueue.main.async { [weak self] in
            if PyrusServiceDesk.clients.count == 1 {
                PyrusServiceDesk.currentClientId = PyrusServiceDesk.clientId
                self?.updateChats()
                self?.createMenuActions()
                self?.presenter.doWork(.deleteSegmentControl)
                self?.presenter.doWork(.updateTitle(title: PyrusServiceDesk.clients[0].clientName))
                self?.clients = PyrusServiceDesk.clients
                self?.updateIcon(imagePath: PyrusServiceDesk.clients[0].clientIcon, index: 0)
            } else if PyrusServiceDesk.clients.count > 1 {
                let selectedIndex = self?.clients.count ?? 0 > 0 ? PyrusServiceDesk.clients.count - 1 : 0
                let titles: [String] = PyrusServiceDesk.clients.map({ $0.clientName })
                self?.presenter.doWork(.updateTitles(titles: titles, selectedIndex: selectedIndex))
            }
            self?.clients = PyrusServiceDesk.clients
        }
    }
    
    func deleteFilter() {
        if !isNewQr {
            PyrusServiceDesk.currentUserId = nil
            currentUserId = nil
            updateChats()
            createMenuActions()
        }
        isNewQr = false
    }
    
    func reloadChats() {
        PyrusServiceDesk.restartTimer()
        PyrusServiceDesk.syncManager.syncGetTickets()
    }
    
    func openNewChat(userId: String? = nil) {
        let chat = PSDChat(chatId: 0, date: Date(), messages: [])
        chat.subject = "Новое обращение"
        chat.userId = userId ?? PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.userId
        PyrusServiceDesk.currentUserId = userId
        presenter.doWork(.openChat(chat: chat, fromPush: false))
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
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.endRefresh)
        }
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        var filterChats = [PSDChat]()
        for chat in PyrusServiceDesk.chats {
            if currentUserId != nil {
                if chat.userId == currentUserId {
                    filterChats.append(chat)
                }
            } else {
                if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == chat.userId }) {
                    if user.clientId == clientId {
                        filterChats.append(chat)
                    }
                } else if PyrusServiceDesk.clientId == clientId {
                    filterChats.append(chat)
                }
            }
        }

        if chats != filterChats || filterChats.count == 0 {
            DispatchQueue.main.async {
                self.chats = filterChats
            }
        }
    }
    
    @objc func setFilter() {
        guard !PyrusServiceDesk.accessDeniedIds.contains(PyrusServiceDesk.currentUserId ?? "") else {
            return
        }
        
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        if let newSelectedIndex = clients.firstIndex(where: { $0.clientId == clientId }),
           clients.count > 1,
           newSelectedIndex != selectedIndex {
            isNewQr = true
            DispatchQueue.main.async { [weak self] in
                self?.presenter.doWork(.updateSelected(index: newSelectedIndex))
            }
        }
        currentUserId = PyrusServiceDesk.currentUserId
        updateChats()
        
        guard let userId = PyrusServiceDesk.currentUserId,
              getUsers().count > 1
        else {
            createMenuActions()
            return
        }
        
        let userName = PyrusServiceDesk.additionalUsers
            .first(where: {$0.userId == userId})?.userName ?? PyrusServiceDesk.userName
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.setFilter(userName: userName ?? ""))
            self?.createMenuActions()
        }
    }
    
    func createMenuActions() {
        var actions = [MenuAction]()
        let users =  getUsers()
       
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
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.updateMenu(actions: actions, menuVisible: users.count > 1))
        }
    }
    
    func getUsers() -> [PSDUserInfo] {
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
        
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        users = users.filter({ $0.clientId == clientId })
        return users
    }
}
