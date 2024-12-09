import Foundation
import Network

class SyncManager {
    private var isRequestInProgress = false
    private var shouldSendAnotherRequest = false
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
//        PyrusServiceDesk.repository.clear()
//        PSDMessagesStorage.cleanStorage()
//        return
        if !self.isFilter {
            self.isFilter = isFilter
        }
        if isRequestInProgress {
            shouldSendAnotherRequest = true
            return
        }

        isRequestInProgress = true
        shouldSendAnotherRequest = false
        PyrusServiceDesk.repository.load { [weak self] result in
            self?.sendingMessages = PSDMessagesStorage.getSendingMessages()
            var ticketCommands: [TicketCommand]
            switch result {
            case .success(let commands):
                ticketCommands = commands
            case .failure(_):
                ticketCommands = []
            }
            
            PSDGetChats.get(commands: ticketCommands.map({ $0.toDictionary() })) { [weak self] chats, commandsResult, authorAccessDenied, clientsArray, complete in
                guard let self = self else { return }
                var clients = clientsArray
                DispatchQueue.main.async {
                    PyrusServiceDesk.accessDeniedIds = authorAccessDenied ?? []
                }

                let userInfo = ["isFilter": isFilter]
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
                DispatchQueue.main.async {
                    if let clients, clients.count > 0 {
                        PyrusServiceDesk.clients = clients
                    }
                }
                
                if let commandsResult {
                    self.commandsResult = commandsResult.sorted(by: { $0.commentId ?? 0 < $1.commentId ?? 0 })
                    do {
                        let jsonData = try JSONEncoder().encode(commandsResult)
                        NotificationCenter.default.post(name: SyncManager.commandsResultNotification, object: nil, userInfo: ["tickets": jsonData])
                    } catch { }
                    
                    for commandResult in commandsResult {
                        PyrusServiceDesk.repository.deleteCommand(withId: commandResult.commandId)
                        if let message = self.sendingMessages.first(where: { $0.commandId.lowercased() == commandResult.commandId.lowercased() })?.message {
                            PSDMessagesStorage.remove(messageId: message.clientId, needSafe: false, serverTicketId: commandResult.ticketId)
                            if commandResult.error != nil {
                                message.state = .cantSend
                                PSDMessagesStorage.save(message: message)
                            }
                        }
                    }
                    PSDMessagesStorage.saveMessagesToFile()
                }
                
                DispatchQueue.main.async {
                    if let chats {
                        let createMessages = PSDMessagesStorage.getNewCreateTicketMessages()
                        let localChats = PSDGetChats.getSortedChatForMessages(createMessages)
                        PyrusServiceDesk.chats = localChats + chats
                        NotificationCenter.default.post(name: PyrusServiceDesk.chatsUpdateNotification, object: nil, userInfo: userInfo)
                    }
                    
                    PyrusLogger.shared.logEvent("PSDGetChats did end with chats count: \(chats?.count ?? 0).")
                    guard let chats = chats else {
                        return
                    }
                    var unreadChats = 0
                    var lasMessage: PSDMessage?
                    for chat in chats {
                        lasMessage = chat.messages.last
                        guard !chat.isRead else {
                            continue
                        }
                        unreadChats = unreadChats + 1
                    }
                    
                    UnreadMessageManager.refreshNewMessagesCount(unreadChats > 0, lastMessage: lasMessage)
                }

                self.isRequestInProgress = false
                
                if !complete {
                    self.updateRepeatSyncTimer()
                    networkAvailability = false
                } else {
                    if isFilter {
                        self.isFilter = false
                    }
                    clearTimer()
                    networkAvailability = true
                }
                
                if self.shouldSendAnotherRequest {
                    DispatchQueue.main.async {
                        self.syncGetTickets(isFilter: self.isFilter)
                    }
                }
                
            }
        }
    }
}

private extension SyncManager {
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
        syncGetTickets(isFilter: self.isFilter)
    }
}