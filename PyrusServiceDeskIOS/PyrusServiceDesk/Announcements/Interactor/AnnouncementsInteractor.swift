import Foundation

final class AnnouncementsInteractor: NSObject {

    // MARK: - Dependencies

    private let presenter: AnnouncementsPresenterProtocol

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
            if PyrusServiceDesk.clients.contains(where: { $0.announcementsUnreadCount > 0 }) {
                presenter.doWork(.scrollToTop)
            }
            // Порядок важен: сначала обновили announcements (и announcementsDict),
            // потом шлём readAnnouncements — иначе на сервер уйдёт устаревший lastReadId.
            readAnnouncements()
            PyrusServiceDesk.syncManager.syncGetTickets()

        case .updateSelected:
            // TODO: вернётся вместе с сегмент-контролом мультиклиентов.
            break

        case .viewWillDisappear:
            isOpen = false
            markVisibleAnnouncementsAsRead()
        }
    }
}

// MARK: - Private

private extension AnnouncementsInteractor {

    func refreshLastReadIds() {
        for client in PyrusServiceDesk.clients {
            lastReadIds[client.clientId] = client.lasAnnoncementReadId
        }
    }

    /// Локально фиксирует прочитанность при закрытии экрана:
    /// пользователь видел всю ленту, обнуляем счётчики и двигаем lastReadId.
    func markVisibleAnnouncementsAsRead() {
        for client in PyrusServiceDesk.clients {
            if let clientAnnouncements = announcementsDict[client.clientId],
               let newestId = clientAnnouncements.first?.id {
                client.lasAnnoncementReadId = newestId
            }
            client.announcementsUnreadCount = 0
        }
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
            // (уже с обновлённым announcementsDict).
            if hasNewAnnouncements && isOpen {
                readAnnouncements()
            }
        }
    }

    func createAnnouncements() -> [PSDAnnouncement] {
        // TODO: при возврате мультиклиентов — фильтр по PyrusServiceDesk.currentClientId.
        PyrusServiceDesk.announcements
    }
}
