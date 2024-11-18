import Foundation

class SyncManager {
    private var isRequestInProgress = false
    private var shouldSendAnotherRequest = false
    private var isFilter = false
    var commandsResult = [TicketCommandResult]()
    var sendingMessages = [MessageToPass]()

    static let commandsResultNotification = Notification.Name("COMMANDS_RESULT")
    static let updateAccessesNotification = Notification.Name("UPDATE_ACCSESSES")
    
    init() {
        sendingMessages = PSDMessagesStorage.getSendingMessagesFromStorage()
    }
    
    func syncGetTickets(isFilter: Bool = false) {
        self.isFilter = isFilter
        if isRequestInProgress {
            shouldSendAnotherRequest = true
            return
        }

        isRequestInProgress = true
        shouldSendAnotherRequest = false

        PyrusServiceDesk.repository.load { result in
            switch result {
            case .success(let commands):
                PSDGetChats.get(commands: commands.map({ $0.toDictionary() })) { [weak self] chats, commandsResult, authorAccessDenied, clientsArray in
                    guard let self = self else { return }
                    var clients = clientsArray
                    PyrusServiceDesk.accessDeniedIds = authorAccessDenied ?? []
                    if let authorAccessDenied, authorAccessDenied.count > 0 {
                        DispatchQueue.main.async {
                            PyrusServiceDesk.deniedAccessCallback?.deleteUsers(userIds: authorAccessDenied)
                        }
                        NotificationCenter.default.post(name: SyncManager.updateAccessesNotification, object: nil)
                        
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
                    if let clients, clients.count > 0 {
                        PyrusServiceDesk.clients = clients
                    }
                    
                    if self.isFilter {
                        NotificationCenter.default.post(name: PyrusServiceDesk.usersUpdateNotification, object: nil)
                        self.isFilter = false
                    }
                    
                    if let commandsResult {
                        self.commandsResult = commandsResult
                        NotificationCenter.default.post(name: SyncManager.commandsResultNotification, object: nil)
                        for commandResult in commandsResult {
                            PyrusServiceDesk.repository.deleteCommand(withId: commandResult.commandId)
                            if let message = self.sendingMessages.first(where: { $0.commandId.lowercased() == commandResult.commandId.lowercased() })?.message {
                                PSDMessagesStorage.removeFromStorage(messageId: message.clientId)
                                if commandResult.error != nil {
                                    message.state = .cantSend
                                    PSDMessagesStorage.saveInStorage(message: message)
                                }
                            }
                        }
                    }
                                
                    DispatchQueue.main.async {
                        PyrusLogger.shared.logEvent("PSDGetChats did end with chats count: \(chats?.count ?? 0).")
                        guard let chats = chats else{
                            return
                        }
                        var unreadChats = 0
                        var lasMessage: PSDMessage?
                        for chat in chats {
                            lasMessage = chat.messages.last
                            guard !chat.isRead else{
                                continue
                            }
                            unreadChats = unreadChats + 1
                        }
                        
                        UnreadMessageManager.refreshNewMessagesCount(unreadChats > 0, lastMessage: lasMessage)
                    }

                    self.isRequestInProgress = false
                    
                    if self.shouldSendAnotherRequest {
                        self.syncGetTickets()
                    }
                }
            case .failure(_):
                break
            }
        }
    }
}
