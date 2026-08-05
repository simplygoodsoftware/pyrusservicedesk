import Foundation
import StoreKit

struct MenuAction {
    let title: String
    let isSelect: Bool
    let filterAction: () -> Void
    let newChatAction: () -> Void?
}

final class ChatsInteractor: NSObject {

    // MARK: - Constants

    private enum Constants {
        /// Задержка перед скрытием плашки о новых объявлениях:
        /// даём завершиться переходу между экранами, чтобы удаление
        /// ячейки не анимировалось на глазах у пользователя.
        static let hideAnnouncementsBannerDelay: TimeInterval = 0.7
    }

    // MARK: - Dependencies

    private let presenter: ChatsPresenterProtocol
    private let coreDataService: CoreDataServiceProtocol
    private let chatsDataService: PSDChatsDataServiceProtocol
    private let imageRepository: ImageRepositoryProtocol?
    private let readStorage = AnnouncementsReadStorage.shared

    // MARK: - State

    private var chats = [PSDChat]() {
        didSet {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                let chats = prepareChats()
                presenter.doWork(.updateChats(chats: chats, newAnnouncementsInfo: getNewAnnouncementsInfo()))
            }
        }
    }

    var selectedIndex: Int? = nil
    var newAnnouncementsCount: Int = 0
    var isNewQr = false
    var isClear = false
    var isNewUser = false
    var firtLoad = true
    var isFiltered = false

    private var clients = [PSDClientInfo]() {
        didSet {
            updateIfNeedClient()
        }
    }

    private var currentUserId: String? {
        didSet {
            updateChats()
        }
    }

    private var chatsUpdateObserver: NSObjectProtocol?
    private var announcementsReadObserver: NSObjectProtocol?
    private var announcementsUpdateObserver: NSObjectProtocol?
    private var accessesUpdateObserver: NSObjectProtocol?
    private var hideBannerWorkItem: DispatchWorkItem?

    // MARK: - Init

    init(presenter: ChatsPresenterProtocol) {
        self.presenter = presenter
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        imageRepository = ImageRepository()
        super.init()

        chatsUpdateObserver = NotificationCenter.default.addObserver(
            forName: PyrusServiceDesk.chatsUpdateNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            if let userInfo = notification.userInfo,
               let isFilter = userInfo["isFilter"] as? Bool {
                self?.updateChats(isFilter: isFilter)
            }
        }

        announcementsReadObserver = NotificationCenter.default.addObserver(
            forName: PyrusServiceDesk.announcementsReadNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleAnnouncementsRead()
        }

        announcementsUpdateObserver = NotificationCenter.default.addObserver(
            forName: PyrusServiceDesk.announcementsUpdateNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleAnnouncementsUpdate()
        }

        NotificationCenter.default.addObserver(self, selector: #selector(changedClientId), name: PyrusServiceDesk.clientIdChangedNotification, object: nil)
    }

    deinit {
        hideBannerWorkItem?.cancel()
        [chatsUpdateObserver, announcementsReadObserver, announcementsUpdateObserver, accessesUpdateObserver]
            .compactMap { $0 }
            .forEach { NotificationCenter.default.removeObserver($0) }
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - ChatsInteractorProtocol

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
            accessesUpdateObserver = NotificationCenter.default.addObserver(
                forName: SyncManager.updateAccessesNotification,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                if let userInfo = notification.userInfo,
                   let isFilter = userInfo["isFilter"] as? Bool {
                    self?.denyAccesses(isFilter: isFilter)
                }
            }

        case .reloadChats:
            reloadChats()

        case .selectChat(index: let index):
            if newAnnouncementsCount > 0 && index == 0 {
                // Тап по плашке объявлений — переход обрабатывает вью-контроллер.
            } else {
                let index = newAnnouncementsCount > 0 ? index - 1 : index
                openChat(chat: chats[index], fromPush: false)
            }

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
            PyrusServiceDesk.syncManager.syncGetTickets()

            if !isFiltered {
                PyrusServiceDesk.currentUserId = nil
            }
            let filterChats = createChats()
            if filterChats != chats {
                chats = filterChats
            }
            // Плашка объявлений пересчитывается независимо от наличия
            // тикетов: гейт по chats.count оставлял счётчик и модель
            // устаревшими на аккаунтах без чатов.
            presenter.doWork(.updateChats(chats: prepareChats(), newAnnouncementsInfo: getNewAnnouncementsInfo()))
            if chats.count > 0 {
                firtLoad = false
                presenter.doWork(.endRefresh)
            }

            requestReviewIfNeeded()

            if PyrusServiceDesk.clients.count > 1 {
                let selectedIndex = PyrusServiceDesk.clients.firstIndex(where: { $0.clientId == PyrusServiceDesk.currentClientId }) ?? 0
                updateSelected(index: selectedIndex)
                presenter.doWork(.updateSelected(index: selectedIndex))
            }

        case .updateSelected(index: let index):
            updateSelected(index: index)
        }
    }
}

// MARK: - Announcements banner

private extension ChatsInteractor {

    /// Собирает модель плашки о новых объявлениях.
    /// Счётчик считается через AnnouncementsReadStorage: флаги isRead
    /// с наложенным локальным указателем, поэтому эхо синка со старым
    /// состоянием не воскрешает плашку.
    func getNewAnnouncementsInfo() -> NewAnnouncementsInfo? {
        var unreadClients: [PSDClientInfo] = []
        var totalUnread = 0

        for client in PyrusServiceDesk.clients {
            let unread = readStorage.unreadCount(for: client.clientId)
            if unread > 0 {
                totalUnread += unread
                unreadClients.append(client)
            }
        }

        newAnnouncementsCount = totalUnread

        guard totalUnread > 0 else { return nil }
        return NewAnnouncementsInfo(
            clients: unreadClients,
            newAnnouncementsCount: totalUnread,
            lastAnnouncement: totalUnread == 1 ? PyrusServiceDesk.announcements.last : nil
        )
    }

    /// Объявления обновились (пришла дельта). Чаты при этом могли
    /// не измениться — их didSet не сработает, поэтому плашку
    /// пересчитываем и рисуем отсюда.
    ///
    /// Рендер безусловный: newAnnouncementsCount обновляется в том числе
    /// рендерами, выброшенными на невидимом экране (guard isVisible),
    /// поэтому решение «рисовать или нет» по нему принимать нельзя —
    /// кэш может застрять и молча погасить единственный рендер плашки.
    /// Diffable сам не перерисует то, что не изменилось.
    func handleAnnouncementsUpdate() {
        presenter.doWork(.updateChats(
            chats: prepareChats(),
            newAnnouncementsInfo: getNewAnnouncementsInfo()
        ))
    }

    /// Убирает плашку после прочтения объявлений — с задержкой,
    /// чтобы удаление ячейки не анимировалось во время перехода между
    /// экранами. Если обновление придёт, когда экран чатов уже скрыт,
    /// его применит без анимации needsReloadOnAppear во вью-контроллере.
    /// Без гейта по кэшированному счётчику: рендер идемпотентен.
    func handleAnnouncementsRead() {
        hideBannerWorkItem?.cancel()

        let workItem = DispatchWorkItem { [weak self] in
            guard let self else { return }
            presenter.doWork(.updateChats(
                chats: prepareChats(),
                newAnnouncementsInfo: getNewAnnouncementsInfo()
            ))
        }
        hideBannerWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + Constants.hideAnnouncementsBannerDelay,
            execute: workItem
        )
    }
}

// MARK: - Private

private extension ChatsInteractor {

    func updateIfNeedClient() {
        if let clientId = PyrusServiceDesk.currentClientId {
            for (i, client) in clients.enumerated() {
                if client.clientId == clientId,
                   i != selectedIndex {
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

    func requestReviewIfNeeded() {
        let needRequest = RateManager.isActionPerformed(times: 3) || RateManager.isNeedRateCurrentVersion()
        if RateManager.isActionPerformed(times: 3) {
            RateManager.setIfNilDateForNextRate()
            RateManager.incrementActionCount()
        } else if RateManager.isNeedRateCurrentVersion() {
            RateManager.increaseDateForNextRate()
        }
        guard needRequest,
              let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene
        else { return }
        if #available(iOS 14.0, *) {
            SKStoreReviewController.requestReview(in: windowScene)
        }
    }

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
            let userId = PyrusServiceDesk.multichats ? PyrusServiceDesk.currentUserId : PyrusServiceDesk.customUserId
            let params = TicketCommandParams(ticketId: chat.chatId ?? 0, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: userId, messageId: Int(chat.lastComment?.messageId ?? ""))
            let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: userId, params: params)
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
                    userNames += "\(user.userName ?? ""), "
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

    func loadImage(urlString: String, completion: @escaping (_ image: UIImage?) -> Void) {
        let url = PyrusServiceDeskAPI.PSDURL(url: urlString)
        PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
            guard let data = data, error == nil else {
                completion(nil)
                return
            }
            if data.count != 0 {
                let image = UIImage(data: data)
                completion(image)
            } else {
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
            )
        })
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

            let filterChats = createChats()
            if chats != filterChats || filterChats.count == 0 || isClear {
                chats = filterChats
                isClear = false
            }
        }
    }

    func createChats() -> [PSDChat] {
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        var filterChats = [PSDChat]()

        let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(currentUserId)
        let localChats = PSDGetChats.getSortedChatForMessages(createMessages)
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
                } else if chat.userId?.count ?? 0 == 0 && PyrusServiceDesk.anonimClients.contains(PyrusServiceDesk.currentClientId ?? "") {
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
                  userId.count > 0,
                  self?.getUsers().count ?? 0 > 1 || PyrusServiceDesk.anonimClients.contains(PyrusServiceDesk.currentClientId ?? "")
            else {
                self?.createMenuActions()
                return
            }

            let userName = PyrusServiceDesk.additionalUsers
                .first(where: { $0.userId == userId })?.userName ?? PyrusServiceDesk.userName

            self?.presenter.doWork(.setFilter(userName: userName ?? ""))
            self?.createMenuActions()
        }
    }

    @objc func createMenuActions() {
        DispatchQueue.main.async { [weak self] in
            var actions = [MenuAction]()
            let users = self?.getUsers() ?? []

            for user in users {
                guard let userId = user.userId, userId.count > 0 else { continue }
                let filterAction = {
                    PyrusServiceDesk.currentUserId = userId
                    self?.currentUserId = userId
                    self?.presenter.doWork(.setFilter(userName: user.userName ?? ""))
                    self?.createMenuActions()
                }
                let openNewAction = {
                    self?.openNewChat(userId: userId)
                }
                let menuAction = MenuAction(
                    title: user.userName ?? "",
                    isSelect: userId == PyrusServiceDesk.currentUserId,
                    filterAction: filterAction,
                    newChatAction: openNewAction
                )
                actions.append(menuAction)
            }

            self?.presenter.doWork(.updateMenu(actions: actions, menuVisible: actions.count > 1))
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
