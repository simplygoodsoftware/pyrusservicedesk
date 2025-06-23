import Foundation
import StoreKit

struct MenuAction {
    let title: String
    let isSelect: Bool
    let filterAction: () -> Void
    let newChatAction: () -> Void?
}

class ChatsInteractor: NSObject {
    private let presenter: ChatsPresenterProtocol
    
    private var chats = [PSDChat]() {
        didSet {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                let chats = prepareChats()
                presenter.doWork(.updateChats(chats: chats))
            }
        }
    }
    
    var selectedIndex: Int? = nil
    var isNewQr = false
    var isClear = false
    var isNewUser = false
    var firtLoad = true
    var isFiltered = false
    
    private let coreDataService: CoreDataServiceProtocol
    private let chatsDataService: PSDChatsDataServiceProtocol
    private let imageRepository: ImageRepositoryProtocol?

    private var clients = [PSDClientInfo]() {
        didSet {
            updateIfNeedClient()
        }
    }
    
    private func updateIfNeedClient() {
        if let clientId = PyrusServiceDesk.currentClientId {
            for (i,client) in clients.enumerated() {
                if
                    client.clientId == clientId,
                    i != selectedIndex
                {
                    DispatchQueue.main.async {
                        self.updateSelected(index: i)
                        if self.clients.count > 1 {
                            self.presenter.doWork(.updateSelected(index: i))
                        }
                    }
                    break
                }
            }
        }
    }
    
    private var currentUserId: String? {
        didSet {
            updateChats()
        }
    }
    
    init(presenter: ChatsPresenterProtocol) {
        self.presenter = presenter
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        imageRepository = ImageRepository()
        super.init()
        NotificationCenter.default.addObserver(forName: PyrusServiceDesk.chatsUpdateNotification, object: nil, queue: .main) { [weak self] notification in
            if let userInfo = notification.userInfo,
               let isFilter = userInfo["isFilter"] as? Bool {
                self?.updateChats(isFilter: isFilter)
            }
        }
        NotificationCenter.default.addObserver(forName: PSDMessageSend.createNewCommand, object: nil, queue: .main) { _ in
            //self.updateData()
        }
        NotificationCenter.default.addObserver(self, selector: #selector(changedClientId), name: PyrusServiceDesk.clientIdChangedNotification, object: nil)

    }
}

extension ChatsInteractor: ChatsInteractorProtocol {
    func doInteraction(_ action: ChatsInteractorCommand) {
        switch action {
        case .viewDidload:
            if PyrusServiceDesk.clients.count > 0 {
                updateClients()
                chats = PyrusServiceDesk.chats
                presenter.doWork(.endRefresh)
                isClear = false
            }
            createMenuActions()
            NotificationCenter.default.addObserver(self, selector: #selector(showConnectionError), name: SyncManager.connectionErrorNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(createMenuActions), name: .createMenuNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(updateClients), name: PyrusServiceDesk.clientsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(setFilter), name: PyrusServiceDesk.usersUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(newUserFilter), name: PyrusServiceDesk.newUserNotification, object: nil)
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
                if let userId = currentUserId {
                    openNewChat(userId: userId)
                } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.clientId == clientId }) {
                    openNewChat(userId: user.userId)
                } else {
                    openNewChat(userId: PyrusServiceDesk.customUserId)
                }
            } else {
                openNewChat(userId: PyrusServiceDesk.customUserId)
            }
        case .deleteFilter:
            deleteFilter()
        case .viewWillAppear:
   //         guard PyrusServiceDesk.chats.count > 0 else { break }
            PyrusServiceDesk.syncManager.syncGetTickets()
//            if !isFiltered {
//                PyrusServiceDesk.currentUserId = nil
//            }
//            if chats.count == 0 {
//                updateData(firstStart: true)
//            }
            if !isFiltered {
                PyrusServiceDesk.currentUserId = nil
            }
            let filterChats = createChats()
            if filterChats != chats {
                chats = filterChats
            }
            if chats.count > 0 {
                presenter.doWork(.updateChats(chats: prepareChats()))
                firtLoad = false
                presenter.doWork(.endRefresh)
            }
            
            if RateManager.isActionPerformed(times: 3) {
                RateManager.setIfNilDateForNextRate()
                RateManager.incrementActionCount()
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                    SKStoreReviewController.requestReview(in: windowScene)
                }
            } else if RateManager.isNeedRateCurrentVersion() {
                RateManager.increaseDateForNextRate()
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                    SKStoreReviewController.requestReview(in: windowScene)
                }
            }
                      
        case .updateSelected(index: let index):
            updateSelected(index: index)
        }
    }
    
    private func updateData(firstStart: Bool = false) {
        let filterChats = createChats()
        if filterChats != chats {
            chats = filterChats
        }
        if chats.count > 0 {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                let chats = prepareChats()
                presenter.doWork(.updateChats(chats: chats))
            }
//            if firstStart {
//                presenter.doWork(.createChatsOnStart(chats: prepareChats()))
//            } else {
//                presenter.doWork(.updateChats(chats: prepareChats()))
//            }
            
            firtLoad = false
            presenter.doWork(.endRefresh)
        }
    }
}

private extension ChatsInteractor {
    
    @objc func showConnectionError() {
        DispatchQueue.main.async { [weak self] in
            self?.updateTitle()
        }
    }
    
    func updateTitle() {
        if PyrusServiceDesk.syncManager.networkAvailability {
            if self.clients.count == 0 {
               // presenter.doWork(.updateTitle(title: "All_Conversations".localizedPSD()))
            } else {
                presenter.doWork(.updateTitle(title: self.clients.count > 1 ? "All_Conversations".localizedPSD() : clients[0].clientName))
            }
        } else {
            presenter.doWork(.connectionError)
        }
    }
    
    @objc func newUserFilter() {
//        isNewQr = true
        isNewUser = true
        isClear = true
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.startRefresh)
        }
        setFilter()
    }
    
    func openChat(chat: PSDChat, fromPush: Bool) {
        PyrusServiceDesk.currentUserId = chat.userId
        let lastReadedLocalId = max(chat.lastReadedCommentId ?? 0, PyrusServiceDesk.repository.lastLocalReadCommentId(ticketId: chat.chatId) ?? 0)
        if lastReadedLocalId < Int(chat.lastComment?.messageId ?? "") ?? 0 {
            let params = TicketCommandParams(ticketId: chat.chatId ?? 0, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId, messageId: Int(chat.lastComment?.messageId ?? ""))
            let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId:  PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId, params: params)
            PyrusServiceDesk.repository.add(command: command)
            DispatchQueue.main.async {
                PyrusServiceDesk.syncManager.syncGetTickets()
            }
        }

        presenter.doWork(.openChat(chat: chat, fromPush: fromPush))
    }
    
    @objc func denyAccesses(isFilter: Bool) {
        DispatchQueue.main.async { [weak self] in
            var userNames = ""
            for userId in PyrusServiceDesk.accessDeniedIds {
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
                            self?.presenter.doWork(.showAccessDeniedAlert(userNames: userNames, doExit: true))
                        }
                        return
                    }
                } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == userId }) {
                    userNames += "\(user.userName), "
                    PyrusServiceDesk.additionalUsers.removeAll(where: { $0.userId == userId })
                }
            }
            
            self?.deleteFilter()
            self?.presenter.doWork(.deleteFilter)
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
        } else if let image = imageRepository?.loadImage(name: clients[index].clientId, id: nil, type: .clientIcon) {
            presenter.doWork(.updateIcon(image: image))
            clients[index].image = image
            PyrusServiceDesk.clients[index].image = image
        }
        loadImage(urlString: imagePath) { [weak self] image in
            DispatchQueue.main.async { [weak self] in
                if image != nil || self?.clients[index].image == nil {
                    self?.clients[index].image = image ?? UIImage.PSDImage(name: "iiko")
                    PyrusServiceDesk.clients[index].image = image ?? UIImage.PSDImage(name: "iiko")
                    self?.presenter.doWork(.updateIcon(image: image))
                    if let image, let name = self?.clients[index].clientId {
                        self?.imageRepository?.saveImage(image, name: name, id: nil, type: .clientIcon)
                    }
                }
            }
        }
    }
    
    func updateSelected(index: Int) {
        if index < clients.count {
            if selectedIndex != nil && index != selectedIndex {
                presenter.doWork(.deleteFilter)
            }
            selectedIndex = index
            PyrusServiceDesk.currentClientId = clients[index].clientId
            createMenuActions()
            updateChats()
            
            updateIcon(imagePath: clients[index].clientIcon, index: index)
        }
        if let newUser = PyrusServiceDesk.newUser {
            if PyrusServiceDesk.chats.first(where: { $0.userId == newUser.userId }) == nil {
                newUserFilter()
            }
            let _ = PyrusServiceDesk.addUser(appId: newUser.clientId, clientName: "", userId: newUser.userId, userName: newUser.userName)
            PyrusServiceDesk.newUser = nil
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
                self?.clients = PyrusServiceDesk.clients
            } else if PyrusServiceDesk.clients.count > 1 {
                let selectedIndex = self?.clients.count ?? 0 > 0 ? PyrusServiceDesk.clients.count - 1 : 0
                let titles: [String] = PyrusServiceDesk.clients.map({ $0.clientName })
                self?.clients = PyrusServiceDesk.clients
                self?.presenter.doWork(.updateTitles(titles: titles, selectedIndex: selectedIndex))
            }
        }
    }
    
    
    
    @objc func changedClientId() {
        updateIfNeedClient()
    }
    
    func deleteFilter() {
        isFiltered = false
        if !isNewQr {
            isNewUser = false
            PyrusServiceDesk.currentUserId = nil
            currentUserId = nil
            updateChats()
            createMenuActions()
        }
        isNewQr = false
    }
    
    func reloadChats() {
        PyrusServiceDesk.restartTimer()
        DispatchQueue.main.async {
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
    }
    
    func openNewChat(userId: String? = nil) {
        let chat = PSDChat(chatId: 0, date: Date(), messages: [])
        chat.subject = "NewTicket".localizedPSD()
        chat.userId = userId ?? PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.userId
        PyrusServiceDesk.currentUserId = userId
        presenter.doWork(.openChat(chat: chat, fromPush: false))
    }
    
    func prepareChats() -> [ChatPresenterModel] {
        return chats.map({
            var isRead = $0.isRead
            if
                !isRead,
                let lastLocalReadedId = PyrusServiceDesk.repository.lastLocalReadCommentId(ticketId: $0.chatId),
                lastLocalReadedId >= (Int($0.lastComment?.messageId ?? "") ?? 0)
            {
                isRead = true
            }
            return ChatPresenterModel(
            id: $0.chatId ?? 0,
            date: $0.lastMessageDate ?? $0.date?.messageTime(),
            isRead: isRead || $0.messages.count == 0,
            isActive: $0.isActive,
            subject: $0.subject,
            lastComment: $0.lastComment,
            messages: $0.messages,
            lastMessageAttributedText: $0.lastMessageText
        )})
    }
    
    @objc func updateChats(isFilter: Bool = false) {
        if let newUser = PyrusServiceDesk.newUser {
            if PyrusServiceDesk.chats.first(where: { $0.userId == newUser.userId }) == nil {
                newUserFilter()
            }
            let _ = PyrusServiceDesk.addUser(appId: newUser.clientId, clientName: "", userId: newUser.userId, userName: newUser.userName)
            PyrusServiceDesk.newUser = nil
        }
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                return
            }
            
            if !isFilter && self.isNewUser {
                return
            }
            isNewUser = false
            presenter.doWork(.endRefresh)
            
//            updateLocalChats()
            let filterChats = createChats()
            if chats != filterChats || filterChats.count == 0 || isClear {
                chats = filterChats
                isClear = false
            }

        }
    }
    
    private func updateLocalChats() {
        let filterChats = createChats()
        if chats != filterChats || filterChats.count == 0 || isClear {
            chats = filterChats
            isClear = false
        }
    }
    
    private func createChats() -> [PSDChat] {
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        var filterChats = [PSDChat]()
        
        let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(currentUserId)
        var localChats = PSDGetChats.getSortedChatForMessages(createMessages)
        var allChats = PyrusServiceDesk.chats
        allChats = localChats + allChats
        
        for chat in allChats {
            if currentUserId != nil {
                if chat.userId == currentUserId {
                    filterChats.append(chat)
                }
            } else {
                if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == chat.userId }) {
                    if user.clientId == clientId {
                        filterChats.append(chat)
                    }
                } else if PyrusServiceDesk.clientId == clientId && chat.userId == (PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId) {
                    filterChats.append(chat)
                }
            }
        }
        for chat in filterChats {
            guard let lastMessage = PSDMessagesStorage.getMessages(for: chat.chatId).last else {
                continue
            }
            if let date = chat.date {
                chat.date = lastMessage.date > date ? lastMessage.date : date
            } else {
                chat.date = lastMessage.date
            }
            chat.isActive = true
        }
        filterChats = PSDGetChats.sortByLastMessage(filterChats)
        if filterChats.count == 0 {
            print("JNrelf")
        }
        return filterChats
    }
    
    @objc func setFilter() {
        isFiltered = true
        DispatchQueue.main.async { [weak self] in
            let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
            if let newSelectedIndex = self?.clients.firstIndex(where: { $0.clientId == clientId }),
               self?.clients.count ?? 0 > 1,
               newSelectedIndex != self?.selectedIndex {
                self?.isNewQr = true
                self?.presenter.doWork(.updateSelected(index: newSelectedIndex))
            }
            self?.currentUserId = PyrusServiceDesk.currentUserId
            if !(self?.isNewUser ?? false) {
                self?.updateChats()
            }
            
            guard let userId = PyrusServiceDesk.currentUserId,
                  self?.getUsers().count ?? 0 > 1
            else {
                self?.createMenuActions()
                return
            }
            
            let userName = PyrusServiceDesk.additionalUsers
                .first(where: {$0.userId == userId})?.userName ?? PyrusServiceDesk.userName
            
            self?.presenter.doWork(.setFilter(userName: userName ?? ""))
            self?.createMenuActions()
        }
    }
    
    @objc func createMenuActions() {
        DispatchQueue.main.async { [weak self] in
            var actions = [MenuAction]()
            let users =  self?.getUsers() ?? []
            
            for user in users {
                let userId = user.userId
                let filterAction = {
                    PyrusServiceDesk.currentUserId = userId
                    self?.currentUserId = userId
                    self?.presenter.doWork(.setFilter(userName: user.userName))
                    self?.createMenuActions()
                }
                let openNewAction = {
                    self?.openNewChat(userId: userId)
                }
                let menuAction = MenuAction(
                    title: user.userName,
                    isSelect: userId == PyrusServiceDesk.currentUserId,
                    filterAction: filterAction,
                    newChatAction: openNewAction
                )
                actions.append(menuAction)
            }
            
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
            secretKey: PyrusServiceDesk.securityKey
        )
        if !users.contains(user) {
            users.append(user)
        }
        
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        users = users.filter({ $0.clientId == clientId })
        return users
    }
}

