import Foundation

class TicketCommandRepository {
    private let fileURL: URL
    private var commandsCache: [TicketCommand]?
    private let chatsDataService: PSDChatsDataServiceProtocol
    
    init(filename: String = "ticketCommands.json") {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        self.fileURL = paths[0].appendingPathComponent(filename)
        chatsDataService = PSDChatsDataService()
    }
    
    func save(commands: [TicketCommand], completion: ((Error?) -> Void)? = nil) {
        self.commandsCache = commands
        
        DispatchQueue.global(qos: .background).async {
            do {
                let encoder = JSONEncoder()
                encoder.outputFormatting = .prettyPrinted
                let data = try encoder.encode(commands)
                try data.write(to: self.fileURL)
                DispatchQueue.main.async {
                    completion?(nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion?(error)
                }
            }
        }
    }
    
    func getCreateCommentCommands(completion: @escaping ([TicketCommand]) -> Void) {
        PSDMessagesStorage.loadMessages() { storeMessages in
            PyrusServiceDesk.storeMessages = storeMessages
            let messages = PSDMessagesStorage.getSendingMessages()
            var commands = [TicketCommand]()
            
            for message in messages {
                var hasUnsendAttachments = false
                if let attachments = message.message.attachments, attachments.count > 0 {
                    for (i,attachment) in attachments.enumerated(){
                        if attachment.emptyId() {
                            PSDMessageSend.passFile(message.message, attachmentIdex: i, delegate: nil)
                            hasUnsendAttachments = true
                        }
                        
                    }
                }
                let messageToPass = message.message
                let requestNewTicket = PyrusServiceDesk.multichats && (messageToPass.ticketId == 0 || messageToPass.requestNewTicket)
                var attachmentsData: [AttachmentData]?
                if let attachments = messageToPass.attachments {
                    attachmentsData = []
                    for attachment in attachments {
                        let attach = AttachmentData(type: 0, name: attachment.name, guid: attachment.serverIdentifer)
                        attachmentsData?.append(attach)
                    }
                }
                let params = TicketCommandParams(ticketId: messageToPass.ticketId, appId: messageToPass.appId, requestNewTicket: requestNewTicket, userId: messageToPass.userId, message: messageToPass.text, attachments: attachmentsData)
                let command = TicketCommand(commandId: message.commandId, type: .createComment, appId: messageToPass.appId, userId:  messageToPass.userId, params: params)
                
                if !hasUnsendAttachments {
                    commands.append(command)
                }
            }
            completion(commands)
        }
    }
    
    func getCommands() -> [TicketCommand] {
        if commandsCache == nil {
            loadCommands()
        }
        var commands = [TicketCommand]()
        if let commandsCache {
            for command in commandsCache {
                var hasUnsendAttachments = false
                if let attachments = command.params.attachments {
                    for attach in attachments {
                        if attach.guid?.count ?? 0 == 0 || attach.guid == "0" {
                            hasUnsendAttachments = true
                        }
                    }
                }
                
                if !hasUnsendAttachments, !(command.params.ticketId ?? 0 < 0 && command.params.requestNewTicket == false) {
                    commands.append(command)
                }
            }
        }
        
        var commandsForSync = commands//[TicketCommand]()
//        for command in commands {
//            var hasUnsendNewTicketCommand = true
//            if let ticketId = command.params.ticketId,
//               ticketId < 0,
//               !(command.params.requestNewTicket ?? false) {
//                for newTicketCommand in commands {
//                    if newTicketCommand.params.ticketId == ticketId && newTicketCommand.params.requestNewTicket ?? false {
//                        hasUnsendNewTicketCommand = false
//                    }
//                }
//            } else {
//                hasUnsendNewTicketCommand = false
//            }
//            
//            if !hasUnsendNewTicketCommand {
//                commandsForSync.append(command)
//            }
//        }
//        
        if commandsForSync.count > 0 {
            print(commandsForSync)
        }
        return commandsForSync
    }
    
    func loadCommands() {
        commandsCache = chatsDataService.getAllCommands()
        PSDMessagesStorage.createMessages(from: commandsCache?.filter({ $0.type == 0 }) ?? [])
    }
    
    func load(completion: @escaping (Result<[TicketCommand], Error>) -> Void) {
        getCreateCommentCommands() { [weak self] createTicketCommands in
            if let cachedCommands = self?.commandsCache {
                completion(.success(cachedCommands + createTicketCommands))
                return
            }
            
            DispatchQueue.global(qos: .background).async { [weak self] in
                guard let self else { return }
                do {
                    let data = try Data(contentsOf: self.fileURL)
                    let decoder = JSONDecoder()
                    let commands = try decoder.decode([TicketCommand].self, from: data)
                    
                    self.commandsCache = commands
                    
                    DispatchQueue.main.async {
                        completion(.success(commands + createTicketCommands))
                    }
                } catch {
                    self.commandsCache = []
                    DispatchQueue.main.async {
                        completion(.failure(error))
                    }
                }
            }
        }
    }
    
    func loadFromFile(completion: @escaping (Result<[TicketCommand], Error>) -> Void) {
        if let cachedCommands = commandsCache {
            completion(.success(cachedCommands))
            return
        }
        
        DispatchQueue.global(qos: .background).async { [weak self] in
            guard let self else { return }
            do {
                let data = try Data(contentsOf: self.fileURL)
                let decoder = JSONDecoder()
                let commands = try decoder.decode([TicketCommand].self, from: data)
                
                self.commandsCache = commands
                
                DispatchQueue.main.async {
                    completion(.success(commands))
                }
            } catch {
                self.commandsCache = []
                DispatchQueue.main.async {
                    completion(.failure(error))
                }
            }
        }
    }
    
    func add(command: TicketCommand, completion: ((Error?) -> Void)? = nil) {
       commandsCache?.removeAll(where: { $0.commandId.lowercased() == command.commandId.lowercased() })
        if command.params.message == "ааа" {
            print("фывапролорпавыфывапр")
            print(command.commandId)
            print(command.params.attachments?[0].guid)
        }
        commandsCache?.append(command)
        chatsDataService.saveTicketCommand(with: command)
        
        
//        let commands = chatsDataService.getAllCommands()
//        PyrusServiceDesk.syncManager.syncGetTickets()
//        loadFromFile { result in
//            switch result {
//            case .success(var commands):
//                commands.append(command)
//                self.save(commands: commands, completion: completion)
//            case .failure(let error):
//                completion?(error)
//            }
//        }
    }
    
    func deleteCommand(withId commandId: String, serverTicketId: Int? = nil, completion: ((Error?) -> Void)? = nil) {
//        commandsCache?.removeAll(where: {
//            if $0.commandId.lowercased() == commandId.lowercased() {
//                if $0.params.message == "ааа" {
//                    print("!!!!!!!!!!!!!!!!!!!!!!!")
//                    print($0.commandId)
//                    print($0.params.attachments?[0].guid)
//                }
//                return true
//            }
//            return false
//        })
        var newId: (Int, Int)? = nil
        commandsCache?.removeAll(where: {
            if $0.commandId.lowercased() == commandId.lowercased() {
                if
                    $0.params.requestNewTicket ?? false,
                    $0.params.ticketId ?? 0 < 0,
                    let serverTicketId = serverTicketId
                {
                    newId = ($0.params.ticketId ?? 0, serverTicketId)
                }
                return true
            }
            return false
        })
        if let newId = newId {
            commandsCache?.forEach({ command in
                if command.params.ticketId == newId.0 {
                    command.params.ticketId = newId.1
                }
            })
        }
        chatsDataService.deleteCommand(with: commandId.lowercased(), serverTicketId: serverTicketId)
//        loadFromFile { result in
//            switch result {
//            case .success(var commands):
//                commands.removeAll(where: {$0.commandId.lowercased() == commandId.lowercased()})
//                self.save(commands: commands, completion: completion)
//            case .failure(let error):
//                completion?(error)
//            }
//        }
    }
    
    func clear(completion: ((Error?) -> Void)? = nil) {
        commandsCache = []
        save(commands: [], completion: completion)
    }
    
    func lastLocalReadCommentId(ticketId: Int?) -> Int? {
        guard let ticketId = ticketId else {
            return nil
        }
        var lastLocalId: Int? = nil
        commandsCache?.forEach{
            if
                $0.type == TicketCommandType.readTicket.rawValue,
                $0.params.ticketId == ticketId,
                lastLocalId ?? 0 < $0.params.messageId ?? 0
            {
                lastLocalId = $0.params.messageId
            }
        }
        return lastLocalId
    }
}
