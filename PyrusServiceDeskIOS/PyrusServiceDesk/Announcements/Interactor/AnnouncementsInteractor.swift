import Foundation

class AnnouncementsInteractor: NSObject {
    private let presenter: AnnouncementsPresenterProtocol
    
    private var announcements = [PSDAnnouncement]() {
        didSet {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                let announcements = self.announcements.reversed()
                presenter.doWork(.updateAnnouncements(announcements: Array(announcements), lastReadId: lasReadAnnouncementId))
            }
        }
    }
    
    var selectedIndex: Int? = nil
    var isClear = false
    var firtLoad = true
    var isOpen = false
    
    var lasReadAnnouncementId: String?
    
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
            updateAnnouncements()
        }
    }
    
    init(presenter: AnnouncementsPresenterProtocol) {
        self.presenter = presenter
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        imageRepository = ImageRepository()
        super.init()
        NotificationCenter.default.addObserver(forName: PyrusServiceDesk.chatsUpdateNotification, object: nil, queue: .main) { [weak self] notification in
            if let userInfo = notification.userInfo,
               let isFilter = userInfo["isFilter"] as? Bool {
                self?.updateAnnouncements(isFilter: isFilter)
            }
        }

        NotificationCenter.default.addObserver(self, selector: #selector(changedClientId), name: PyrusServiceDesk.clientIdChangedNotification, object: nil)

    }
}

extension AnnouncementsInteractor: AnnouncementsInteractorProtocol {
    func doInteraction(_ action: AnnouncementsInteractorCommand) {
        switch action {
        case .viewDidload:
            if PyrusServiceDesk.clients.count > 0 {
                updateClients()
                announcements = PyrusServiceDesk.announcements
                presenter.doWork(.endRefresh)
                isClear = false
            }
            NotificationCenter.default.addObserver(self, selector: #selector(showConnectionError), name: SyncManager.connectionErrorNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(updateClients), name: PyrusServiceDesk.clientsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(updateAnnouncements), name: PyrusServiceDesk.announcementsUpdateNotification, object: nil)
            
        case .reloadAnnouncements:
            reloadAnnouncements()
        case .viewWillAppear:
            if PyrusServiceDesk.clients.count > 1 {
                let selectedIndex = PyrusServiceDesk.clients.firstIndex(where: { $0.clientId == PyrusServiceDesk.currentClientId }) ?? 0
                updateSelected(index: selectedIndex)
                presenter.doWork(.updateSelected(index: selectedIndex))
            }
            isOpen = true
            if let client = PyrusServiceDesk.clients.first(where: { $0.clientId == PyrusServiceDesk.currentClientId }) {
                lasReadAnnouncementId = client.lasAnnoncementReadId
            }
            readAnnouncements()
            PyrusServiceDesk.syncManager.syncGetTickets()
            let filterChats = createAnnouncements()
            if filterChats != announcements {
                announcements = filterChats
            }
            if announcements.count > 0 {
                presenter.doWork(.updateAnnouncements(announcements: announcements.reversed(), lastReadId: lasReadAnnouncementId))
                firtLoad = false
                presenter.doWork(.endRefresh)
            }
                      
        case .updateSelected(index: let index):
            updateSelected(index: index)
        case .viewWillDisappear:
            isOpen = false
            let client = PyrusServiceDesk.clients.first(where: { $0.clientId == PyrusServiceDesk.currentClientId })
            client?.lasAnnoncementReadId = announcements.last?.id
            client?.announcementsUnreadCount = 0
        }
    }
    
    private func updateData(firstStart: Bool = false) {
        let filterChats = createAnnouncements()
        if filterChats != announcements {
            announcements = filterChats
        }
        if announcements.count > 0 {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                presenter.doWork(.updateAnnouncements(announcements: announcements.reversed(), lastReadId: lasReadAnnouncementId))
            }
            
            firtLoad = false
            presenter.doWork(.endRefresh)
        }
    }
}

private extension AnnouncementsInteractor {
    
    func readAnnouncements() {
        guard announcements.count > 0 else { return }
        let command = TicketCommand(commandId: UUID().uuidString, type: .readAnnouncemnts, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, params: TicketCommandParams(appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, authorId: PyrusServiceDesk.authorId, lastReadAnnouncementId: announcements.last?.id))
        PyrusServiceDesk.repository.add(command: command, needSync: false)
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
                presenter.doWork(.updateTitle(title: self.clients.count > 1 ? "Announcements".localizedPSD() : clients[0].clientName))
            }
        } else {
            presenter.doWork(.connectionError)
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
        let client = PyrusServiceDesk.clients.first(where: { $0.clientId == PyrusServiceDesk.currentClientId })
        client?.lasAnnoncementReadId = announcements.last?.id
        client?.announcementsUnreadCount = 0
        if index < clients.count {
            selectedIndex = index
            PyrusServiceDesk.currentClientId = clients[index].clientId
            lasReadAnnouncementId = clients[index].lasAnnoncementReadId
            updateAnnouncements()
            
            updateIcon(imagePath: clients[index].clientIcon, index: index)
        }
    }
    
    @objc func updateClients() {
        guard clients != PyrusServiceDesk.clients else { return }
        DispatchQueue.main.async { [weak self] in
            if PyrusServiceDesk.clients.count == 1 {
                PyrusServiceDesk.currentClientId = PyrusServiceDesk.clientId
                self?.updateAnnouncements()
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
    
    func reloadAnnouncements() {
        DispatchQueue.main.async {
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
    }
    
    @objc func updateAnnouncements(isFilter: Bool = false) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                return
            }
            
            presenter.doWork(.endRefresh)
            
            let filterAnnouncements = createAnnouncements()
            
            if filterAnnouncements.count > 0 && filterAnnouncements.last?.id != announcements.last?.id && isOpen {
                readAnnouncements()
            }
            
            if announcements != filterAnnouncements || filterAnnouncements.count == 0 || isClear {
                announcements = filterAnnouncements
                isClear = false
            }

        }
    }
    
    private func createAnnouncements() -> [PSDAnnouncement] {
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        let allAnnouncements = PyrusServiceDesk.announcements
        return allAnnouncements.filter({ $0.appId == clientId })
    }
}

