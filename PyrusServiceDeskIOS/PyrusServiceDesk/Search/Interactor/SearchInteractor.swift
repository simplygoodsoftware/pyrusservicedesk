import Foundation

class SearchInteractor: NSObject {
    private let presenter: SearchPresenterProtocol
    
    private var chats = [SearchChatModel]() {
        didSet {
           // presenter.doWork(.updateChats(chats: chats))
        }
    }

    var isClear = false
    var firtLoad = true
    
    private let coreDataService: CoreDataServiceProtocol
    private let chatsDataService: PSDChatsDataServiceProtocol
    private let imageRepository: ImageRepositoryProtocol?
    private var oldClientId: String?
    
    init(presenter: SearchPresenterProtocol) {
        self.presenter = presenter
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        imageRepository = ImageRepository()
        oldClientId = PyrusServiceDesk.currentClientId
        super.init()
        NotificationCenter.default.addObserver(self, selector: #selector(updateChats), name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
    }
}

extension SearchInteractor: SearchInteractorProtocol {
    func doInteraction(_ action: SearchInteractorCommand) {
        switch action {
        case .viewDidload:
            presenter.doWork(.updateChats(chats: [], searchString: ""))
        case .reloadChats:
            reloadChats()
        case .selectChat(index: let index):
            if let chat = PyrusServiceDesk.chats.first(where: { $0.chatId == chats[index].id }) {
                openChat(chat: chat, messageId: chats[index].messageId)
            }
            break
        case .search(text: let text):
//            let chats = chatsDataService.searchMessages(searchString: text).sorted(by: { $0.date > $1.date })
//            presenter.doWork(.updateChats(chats: chats, searchString: text))
//            self.chats = chats
            chatsDataService.searchMessages(searchString: text) { [weak self] result in
                switch result {
                case .success(let chats):
                    DispatchQueue.main.async { [weak self] in
                        let sortedChats = chats.sorted(by: { $0.date > $1.date })
                        self?.chats = sortedChats
                        self?.presenter.doWork(.updateChats(chats: sortedChats, searchString: text))
                    }
                case .failure(let failure):
                    break
                }
            }
        case .viewWillAppear:
            PyrusServiceDesk.currentUserId = nil
            PyrusServiceDesk.currentClientId = oldClientId
        }
    }
}

private extension SearchInteractor {
    
    func search() {
        
    }
    
    func openChat(chat: PSDChat, messageId: String) {
        oldClientId = PyrusServiceDesk.currentClientId
        PyrusServiceDesk.currentUserId = chat.userId
        PyrusServiceDesk.currentClientId = getUsers().first(where: { $0.userId == chat.userId })?.clientId
        presenter.doWork(.openChat(chat: chat, messageId: messageId))
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

        return users
    }
    
    func reloadChats() {
        PyrusServiceDesk.restartTimer()
        DispatchQueue.main.async {
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
    }
    
    @objc func updateChats() {
        
    }
}
