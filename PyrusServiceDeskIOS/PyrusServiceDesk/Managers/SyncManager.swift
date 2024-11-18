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
                PSDGetChats.get(commands: commands.map({ $0.toDictionary() })) { [weak self] chatsArray, commandsResult, authorAccessDenied  in
                    guard let self = self else { return }
                    
                    PyrusServiceDesk.accessDeniedIds = authorAccessDenied ?? []
                    if let authorAccessDenied, authorAccessDenied.count > 0 {
                        shouldSendAnotherRequest = true
                        DispatchQueue.main.async {
                            PyrusServiceDesk.deniedAccessCallback?.deleteUsers(userIds: authorAccessDenied)
                        }
                        NotificationCenter.default.post(name: SyncManager.updateAccessesNotification, object: nil)
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
                    
                    self.isRequestInProgress = false
                    
                    if self.shouldSendAnotherRequest {
                        self.syncGetTickets()
                    }
                }
            case .failure(let _):
                break
            }
        }
    }
}
