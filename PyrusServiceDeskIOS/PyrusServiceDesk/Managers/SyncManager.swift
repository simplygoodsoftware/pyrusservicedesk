import Foundation
import Network

class SyncManager {
    private var isRequestInProgress = false
    private var shouldSendAnotherRequest = false
    private let coreDataService: CoreDataServiceProtocol
    let chatsDataService: PSDChatsDataServiceProtocol
    var firstLoad = true
    private var isFilter = false
    let monitor = NWPathMonitor()

    var commandsResult = [TicketCommandResult]()
    var sendingMessages = [MessageToPass]()
    var networkAvailability = true {
        didSet {
            NotificationCenter.default.post(name: SyncManager.connectionErrorNotification, object: nil)
        }
    }
    
    private var timerFosSendSync: Timer?
    private var repeatTimeInterval: Double? {
        didSet {
        }
    }

    static let commandsResultNotification = Notification.Name("COMMANDS_RESULT")
    static let updateAccessesNotification = Notification.Name("UPDATE_ACCSESSES")
    static let connectionErrorNotification = Notification.Name("CONNECTION_ERROR")

    init() {
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        monitor.pathUpdateHandler = { path in
            if path.status == .satisfied {
                self.clearTimer()
                self.repeatTimeInterval = 1.0
                self.doSync()
            }
        }
        monitor.start(queue: DispatchQueue.main)
    }
    
    func syncGetTickets(isFilter: Bool = false) {
        guard PyrusServiceDesk.isStarted || !PyrusServiceDesk.multichats else { return }
        PSDMessagesStorage.loadAttachments()
        
//        chatsDataService.deleteAllObjects()
//        PyrusServiceDesk.lastNoteId = 0
//        let repository = AudioRepository()
//        repository?.clearRepository()
//        
//        return;

        if firstLoad {
            firstLoadUpdates()
        }
        
        if !self.isFilter {
            self.isFilter = isFilter
        }
        
        if isRequestInProgress {
            shouldSendAnotherRequest = true
            return
        }

        isRequestInProgress = true
        shouldSendAnotherRequest = false
        sendingMessages = PSDMessagesStorage.getSendingMessages()
        
        let ticketCommands = PyrusServiceDesk.repository.getCommands()
        PSDGetChats.get(commands: ticketCommands.map({ $0.toDictionary() })) { [weak self] chats, commandsResult, authorAccessDenied, clientsArray, complete in
            guard PyrusServiceDesk.isStarted || !PyrusServiceDesk.multichats else { return }
            guard let self = self else { return }
            
            let userInfo = ["isFilter": isFilter]
            var clients = clientsArray
            
            // Проверяем доступы и удаляем вендоров (clients) при необходимости
            checkAccesses(authorAccessDenied: authorAccessDenied, clientsArray: clientsArray, clients: &clients, userInfo: userInfo)
            
            // Обрабатываем результаты команд
            updateCommandsAndMessageStorage(commandsResult: commandsResult)
            
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                
                if let chats, complete {
                    
                    var unreadChats = 0
                    var lasMessage: PSDMessage?
                    for chat in chats {
                        guard !chat.isRead else {
                            continue
                        }
                        lasMessage = chat.messages.last                        
                        unreadChats = unreadChats + 1
                    }
                    UnreadMessageManager.refreshNewMessagesCount(unreadChats > 0, lastMessage: lasMessage)
                    
                    chatsDataService.saveChatModels(with: chats) { [weak self] _ in
                        DispatchQueue.main.async { [weak self] in
                            guard let self else { return }
                            
                            if let clients, clients.count > 0, PyrusServiceDesk.isStarted {
                                updateChatsAndClients(clients: clients, userInfo: userInfo)
                            }
                            
                            if isFilter {
                                self.isFilter = false
                            }
                            clearTimer()
                            networkAvailability = true
                            isRequestInProgress = false
                            
                            if shouldSendAnotherRequest {
                                syncGetTickets(isFilter: self.isFilter)
                            }
                        }
                    }
                    
                } else if !complete {
                    updateRepeatSyncTimer()
                    networkAvailability = false
                    isRequestInProgress = false
                    if shouldSendAnotherRequest {
                        syncGetTickets(isFilter: self.isFilter)
                    }
                }
            }
        }
    }
}

private extension SyncManager {
    
    func firstLoadUpdates() {
        if PyrusServiceDesk.multichats {
            let cashe = chatsDataService.getAllChats()
            PyrusServiceDesk.chats = cashe
        } else {
            let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
            var localChats = PSDGetChats.getSortedChatForMessages(createMessages)
            let chats = chatsDataService.getChatsHeaders()
            let messages = chatsDataService.getAllMessages()
            PyrusServiceDesk.chats = localChats + chats
            PyrusServiceDesk.allMessages = messages + createMessages
//            PyrusServiceDesk.chats = chatsDataService.getChatsHeaders()
//            PyrusServiceDesk.allMessages = chatsDataService.getAllMessages()
        }
        
        PyrusServiceDesk.clients = chatsDataService.getAllClients()
        PyrusServiceDesk.repository.loadCommands()
        firstLoad = false
        if CacheVersionManager.shared.checkAndUpdateIfNeeded() {
            PyrusServiceDesk.lastNoteId = 0
            for user in PyrusServiceDesk.additionalUsers {
                user.lastNoteId = 0
            }
        }
//            chatsDataService.getAllChats() { [weak self] chats in
//                DispatchQueue.main.async { [weak self] in
//                    guard let self else { return }
//                    PyrusServiceDesk.clients = chatsDataService.getAllClients()
//                    PyrusServiceDesk.chats = chats
//                    PyrusServiceDesk.cacheLoadedCallback?.cacheLoaded()
//                }
//            }
    }
    
    func checkAccesses(authorAccessDenied: [String]?, clientsArray: [PSDClientInfo]?, clients: inout [PSDClientInfo]?, userInfo: [AnyHashable : Any]?) {
        DispatchQueue.main.async {
            PyrusServiceDesk.accessDeniedIds = authorAccessDenied ?? []
        }

        if let authorAccessDenied, authorAccessDenied.count > 0 {
            DispatchQueue.main.async {
                PyrusServiceDesk.deniedAccessCallback?.deleteUsers(userIds: authorAccessDenied)
            }
            
            NotificationCenter.default.post(name: SyncManager.updateAccessesNotification, object: nil, userInfo: userInfo)
            
            if let clientsArray {
                for client in clientsArray {
                    var removeClient = true
                    for user in PyrusServiceDesk.additionalUsers {
                        if user.clientId == client.clientId && !authorAccessDenied.contains(user.userId) {
                            removeClient = false
                            break
                        }
                    }
                    if client.clientId == PyrusServiceDesk.clientId && !authorAccessDenied.contains(PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId) {
                        continue
                    }
                    
                    if !removeClient { continue }
                    if PyrusServiceDesk.currentClientId == client.clientId {
                        PyrusServiceDesk.currentClientId = nil
                    }
                    clients?.removeAll(where: { $0.clientId == client.clientId })
                }
            }
        }
    }
    
    func updateCommandsAndMessageStorage(commandsResult: [TicketCommandResult]?) {
        if let commandsResult {
            self.commandsResult = commandsResult.sorted(by: { $0.commentId ?? 0 < $1.commentId ?? 0 })
            do {
                let jsonData = try JSONEncoder().encode(commandsResult)
                NotificationCenter.default.post(name: SyncManager.commandsResultNotification, object: nil, userInfo: ["tickets": jsonData])
            } catch { }
            
            for commandResult in commandsResult {
                if let message = self.sendingMessages.first(where: { $0.commandId.lowercased() == commandResult.commandId.lowercased() })?.message {
                    PyrusServiceDesk.repository.deleteCommand(withId: commandResult.commandId, serverTicketId: commandResult.ticketId)
                    PSDMessagesStorage.remove(messageId: message.clientId, needSafe: false, serverTicketId: commandResult.ticketId)
                    if commandResult.error != nil {
                        message.state = .cantSend
                        PSDMessagesStorage.save(message: message)
                    }
                } else {
                    PyrusServiceDesk.repository.deleteCommand(withId: commandResult.commandId)
                }
            }
            
            PSDMessagesStorage.saveMessagesToFile()
        }
    }
    
    func updateChatsAndClients(clients: [PSDClientInfo], userInfo: [AnyHashable: Any]) {
        if PyrusServiceDesk.multichats {
            chatsDataService.getAllChats() { [weak self, userInfo] chats in
                DispatchQueue.main.async { [weak self] in
                    PyrusServiceDesk.clients = clients
                    self?.chatsDataService.saveClientModels(with: clients)
                    PyrusServiceDesk.chats = chats
                    NotificationCenter.default.post(name: PyrusServiceDesk.chatsUpdateNotification, object: nil, userInfo: userInfo)
                }
            }
        } else {
            chatsDataService.getChatsHeaders() { [weak self, userInfo] chats in
                DispatchQueue.main.async { [weak self, userInfo] in
                    self?.chatsDataService.getAllMessages() { [weak self, userInfo] messages in
                        PyrusServiceDesk.clients = clients
                        self?.chatsDataService.saveClientModels(with: clients)
                        let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
                        var localChats = PSDGetChats.getSortedChatForMessages(createMessages)
                        PyrusServiceDesk.chats = localChats + chats
                        PyrusServiceDesk.allMessages = messages + createMessages
                        NotificationCenter.default.post(name: PyrusServiceDesk.chatsUpdateNotification, object: nil, userInfo: userInfo)
                    }
                }
            }
        }
    }
    
    func updateRepeatSyncTimer() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if timerFosSendSync == nil {
                repeatTimeInterval = 1
            } else {
                let currentRepeatInterval = repeatTimeInterval ?? 0
                repeatTimeInterval = (currentRepeatInterval >= 120 || (currentRepeatInterval * 2) > 120) ? 120 : currentRepeatInterval * 2
                clearTimer()
            }
            self.timerFosSendSync = Timer.scheduledTimer(timeInterval: repeatTimeInterval ?? 1, target: self, selector: #selector(self.doSync), userInfo: nil, repeats: false)
        }
    }
    
    func clearTimer() {
        timerFosSendSync?.invalidate()
        timerFosSendSync = nil
    }
    
    @objc func doSync() {
        DispatchQueue.main.async { [weak self] in
            self?.syncGetTickets(isFilter: self?.isFilter ?? false)
        }
    }
}
