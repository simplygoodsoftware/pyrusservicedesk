import Foundation

final class AnnouncementsInteractor: NSObject {

    // MARK: - Dependencies

    private let presenter: AnnouncementsPresenterProtocol
    private let readStorage = AnnouncementsReadStorage.shared

    // MARK: - State (только main thread)

    /// lastReadId по каждому клиенту, зафиксированный на момент открытия экрана.
    /// По нему презентер делит ленту на «новые» и «прочитанные».
    private var lastReadIds: [String: String] = [:]

    /// Последние отправленные на сервер lastReadId — защита от спама командами.
    private var sentReadIds: [String: String] = [:]

    private var announcementsDict = [String: [PSDAnnouncement]]()

    /// Все объявления. Присваивать только на main thread:
    /// didSet синхронно перегруппировывает и уведомляет презентер,
    /// чтобы readAnnouncements() всегда работал с актуальными данными.
    private var announcements = [PSDAnnouncement]() {
        didSet {
            dispatchPrecondition(condition: .onQueue(.main))
            let grouped: [String: [PSDAnnouncement]] = Dictionary(grouping: announcements, by: { $0.appId })
                .mapValues { group in
                    group.sorted { $0.orderIndex > $1.orderIndex } // новые сначала
                }
            announcementsDict = grouped
            presenter.doWork(.updateAnnouncements(announcements: grouped, lastReadIds: lastReadIds))
        }
    }

    private var isOpen = false
    private var chatsUpdateObserver: NSObjectProtocol?

    // MARK: - Init

    init(presenter: AnnouncementsPresenterProtocol) {
        self.presenter = presenter
        super.init()

        chatsUpdateObserver = NotificationCenter.default.addObserver(
            forName: PyrusServiceDesk.chatsUpdateNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let isFilter = notification.userInfo?["isFilter"] as? Bool ?? false
            self?.updateAnnouncements(isFilter: isFilter)
        }
    }

    deinit {
        if let chatsUpdateObserver {
            NotificationCenter.default.removeObserver(chatsUpdateObserver)
        }
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - AnnouncementsInteractorProtocol

extension AnnouncementsInteractor: AnnouncementsInteractorProtocol {
    func doInteraction(_ action: AnnouncementsInteractorCommand) {
        switch action {
        case .viewDidload:
            NotificationCenter.default.addObserver(self, selector: #selector(showConnectionError), name: SyncManager.connectionErrorNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(updateAnnouncementsFromNotification), name: PyrusServiceDesk.announcementsUpdateNotification, object: nil)

            updateTitle()
            if !PyrusServiceDesk.clients.isEmpty {
                // lastReadIds заполняем ДО первого присвоения announcements,
                // иначе первый рендер пройдёт без разделителя «новые/прочитанные»
                // и таблица мигнёт при повторном обновлении.
                refreshLastReadIds()
                announcements = createAnnouncements()
                presenter.doWork(.endRefresh)
            }

        case .reloadAnnouncements:
            reloadAnnouncements()

        case .viewWillAppear:
            isOpen = true
            refreshLastReadIds()

            let filtered = createAnnouncements()
            if !filtered.isEmpty {
                presenter.doWork(.endRefresh)
            }
            announcements = filtered

            // Скролл к новым проверяем ДО локальной пометки —
            // после неё непрочитанных уже не будет.
            if readStorage.hasUnread() {
                presenter.doWork(.scrollToTop)
            }

            // Порядок важен: сначала обновили announcements (и announcementsDict),
            // потом пометка и отправка — иначе на сервер уйдёт устаревший lastReadId.
            markLocalAsRead()
            readAnnouncements()
            PyrusServiceDesk.syncManager.syncGetTickets()

        case .updateSelected:
            // TODO: вернётся вместе с сегмент-контролом мультиклиентов.
            break

        case .viewWillDisappear:
            isOpen = false
            // Подчищает объявления, пришедшие пока экран был открыт.
            markLocalAsRead()
        }
    }
}

// MARK: - Private

private extension AnnouncementsInteractor {

    /// Снапшот прочитанности на момент открытия экрана:
    /// серверное состояние с наложенной локальной пометкой.
    func refreshLastReadIds() {
        for client in PyrusServiceDesk.clients {
            lastReadIds[client.clientId] = readStorage.effectiveLastReadId(for: client.clientId)
        }
    }

    /// Локально помечает видимую ленту прочитанной и уведомляет экран чатов,
    /// чтобы тот убрал плашку о новых объявлениях.
    ///
    /// Объекты клиентов не мутируем: синк заменяет их целиком,
    /// и любая пометка в них живёт до первого эха с сервера.
    func markLocalAsRead() {
        var didChange = false

        for client in PyrusServiceDesk.clients {
            guard readStorage.unreadCount(for: client.clientId) > 0,
                  let newestId = announcementsDict[client.clientId]?.first?.id
            else { continue }
            readStorage.markRead(clientId: client.clientId, lastReadId: newestId)
            didChange = true
        }

        guard didChange else { return }
        NotificationCenter.default.post(
            name: PyrusServiceDesk.announcementsReadNotification,
            object: nil
        )
    }

    /// Отправляет на сервер lastReadId по каждому клиенту.
    /// Команда добавляется только если id изменился с прошлой отправки.
    func readAnnouncements() {
        var didAddCommand = false

        for (appId, clientAnnouncements) in announcementsDict {
            guard let newestId = clientAnnouncements.first?.id,
                  sentReadIds[appId] != newestId
            else { continue }

            var userId = PyrusServiceDesk.customUserId
            if appId != PyrusServiceDesk.clientId {
                userId = PyrusServiceDesk.additionalUsers.first(where: { $0.clientId == appId })?.userId
            }

            let command = TicketCommand(
                commandId: UUID().uuidString,
                type: .readAnnouncemnts,
                appId: appId,
                userId: userId,
                params: TicketCommandParams(
                    appId: appId,
                    userId: userId,
                    authorId: PyrusServiceDesk.authorId,
                    lastReadAnnouncementId: newestId
                )
            )
            PyrusServiceDesk.repository.add(command: command, needSync: false)
            sentReadIds[appId] = newestId
            didAddCommand = true
        }

        if didAddCommand {
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
    }

    @objc func showConnectionError() {
        DispatchQueue.main.async { [weak self] in
            self?.updateTitle()
        }
    }

    func updateTitle() {
        if PyrusServiceDesk.syncManager.networkAvailability {
            presenter.doWork(.updateTitle(title: "Announcements".localizedPSD()))
        } else {
            presenter.doWork(.connectionError)
        }
    }

    func reloadAnnouncements() {
        DispatchQueue.main.async {
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
    }

    @objc func updateAnnouncementsFromNotification() {
        updateAnnouncements()
    }

    func updateAnnouncements(isFilter: Bool = false) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }

            presenter.doWork(.endRefresh)

            let filtered = createAnnouncements()
            let previousNewestId = announcements.last?.id
            let hasNewAnnouncements = !filtered.isEmpty && filtered.last?.id != previousNewestId

            if announcements != filtered {
                announcements = filtered
            }

            // Экран открыт и пришло новое — сразу помечаем прочитанным
            // локально и на сервере (уже с обновлённым announcementsDict).
            if hasNewAnnouncements && isOpen {
                markLocalAsRead()
                readAnnouncements()
            }
        }
    }

    func createAnnouncements() -> [PSDAnnouncement] {
        // TODO: при возврате мультиклиентов — фильтр по PyrusServiceDesk.currentClientId.
        PyrusServiceDesk.announcements
    }
}
