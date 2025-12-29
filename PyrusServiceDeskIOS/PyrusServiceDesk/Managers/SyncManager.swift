import Foundation
import Network
import AudioToolbox

class SyncManager {
    private var isRequestInProgress = false
    private var shouldSendAnotherRequest = false
    private let coreDataService: CoreDataServiceProtocol
    let chatsDataService: PSDChatsDataServiceProtocol
    var firstLoad = true
    private var isFilter = false
    let monitor = NWPathMonitor()
    private var lastMonitorPathStatus: NWPath.Status = .satisfied
    private let throttle: ThrottleController
    
    var commandsResult = [TicketCommandResult]()
    var sendingMessages = [MessageToPass]()
    var networkAvailability = true {
        didSet {
            NotificationCenter.default.post(name: SyncManager.connectionErrorNotification, object: nil)
        }
    }
    
    private var timerFosSendSync: Timer?
    private var previousDelay: Double?

    static let commandsResultNotification = Notification.Name("COMMANDS_RESULT")
    static let updateAccessesNotification = Notification.Name("UPDATE_ACCSESSES")
    static let connectionErrorNotification = Notification.Name("CONNECTION_ERROR")

    init() {
        throttle = ThrottleController(interval: 5)
        coreDataService = CoreDataService()
        chatsDataService = PSDChatsDataService(coreDataService: coreDataService)
        monitor.pathUpdateHandler = { [weak self] path in
            guard let lastMonitorPathStatus = self?.lastMonitorPathStatus else { return }
            if path.status == .satisfied && (lastMonitorPathStatus == .requiresConnection || lastMonitorPathStatus == .unsatisfied) {
                if self?.timerFosSendSync != nil {
                    self?.clearTimer()
                    self?.updateRepeatSyncTimer()
                }
            }
            self?.lastMonitorPathStatus = path.status
        }
        monitor.start(queue: DispatchQueue.main)
    }
    
    func syncGetTickets(isFilter: Bool = false) {
        let ticketCommands = PyrusServiceDesk.repository.getCommands()
        if ticketCommands.contains(where: { $0.type == TicketCommandType.createComment.rawValue }) {
            throttle.setInterval(Constants.throttlingCreateCommentInterval)
        } else {
            throttle.setInterval(Constants.throttlingMinInterval)
        }
        throttle.execute { [weak self, isFilter] in
            self?.sync(isFilter: isFilter)
        }
    }
    
    func loadCache() {
        firstLoadUpdates()
    }
}

private extension SyncManager {
    
    func sync(isFilter: Bool = false) {
        guard PyrusServiceDesk.isStarted || !PyrusServiceDesk.multichats else { return }
        PSDMessagesStorage.loadAttachments()
        
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
                    if let chat = chats.first {
                        if !chat.isRead {
                            unreadChats += 1
                            lasMessage = chat.lastComment
                        }
                    }
                    UnreadMessageManager.refreshNewMessagesCount(unreadChats > 0, lastMessage: lasMessage)
                    
                    chatsDataService.saveChatModels(with: chats) { [weak self] _ in
                        DispatchQueue.main.async { [weak self] in
                            guard let self else { return }
                            
                            if let clients, clients.count > 0  {
                                updateChatsAndClients(clients: clients, userInfo: userInfo)
                            }
                            
                            if isFilter {
                                self.isFilter = false
                            }
                            clearTimer()
                            previousDelay = Constants.baseDelay
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
                } else {
                    clearTimer()
                    networkAvailability = true
                    isRequestInProgress = false
                }
            }
        }
    }
    
    func firstLoadUpdates() {
        PyrusServiceDesk.clients = chatsDataService.getAllClients()
        PyrusServiceDesk.repository.loadCommands()
        if PyrusServiceDesk.multichats {
            let cashe = chatsDataService.getAllChats()
            PyrusServiceDesk.chats = cashe
        } else {
            let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(PyrusServiceDesk.customUserId)
            let localChats = PSDGetChats.getSortedChatForMessages(createMessages)
            let chats = chatsDataService.getChatsHeaders()
            let messages = chatsDataService.getAllMessages()
            PyrusServiceDesk.chats = localChats + chats
            PyrusServiceDesk.allMessages = messages + createMessages
        }
        
        
        firstLoad = false
        if CacheVersionManager.shared.checkAndUpdateIfNeeded() {
            PyrusServiceDesk.lastNoteId = 0
            for user in PyrusServiceDesk.additionalUsers {
                user.lastNoteId = 0
            }
        }
        
        let commands = PyrusServiceDesk.repository.getCommands()
        if commands.count > 0,
           commands.contains(where: { $0.type != TicketCommandType.setPushToken.rawValue }) {
            syncGetTickets()
        }
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
                if let error = commandResult.error {
                    print("Comand error: \(error)")
                }
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
                        let localChats = PSDGetChats.getSortedChatForMessages(createMessages)
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
                previousDelay = 1
            } else {
                let currentRepeatInterval = previousDelay ?? Constants.baseDelay
                previousDelay = currentRepeatInterval * 3
                clearTimer()
            }
            
            let random = Bool.random() ? Constants.baseDelay : (previousDelay ?? Constants.baseDelay) * 3
            let delay = min(random, Constants.maxDelay)
            
            self.timerFosSendSync = Timer.scheduledTimer(timeInterval: delay, target: self, selector: #selector(self.doSync), userInfo: nil, repeats: false)
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

private extension SyncManager {
    enum Constants {
        static let throttlingMinInterval: TimeInterval = 5
        static let throttlingCreateCommentInterval: TimeInterval = 1
        static let baseDelay: TimeInterval = 1
        static let maxDelay: TimeInterval = 3 * 60
    }
}
