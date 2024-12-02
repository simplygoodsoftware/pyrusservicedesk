import Foundation

class TicketCommandRepository {
    private let fileURL: URL
    private var commandsCache: [TicketCommand]?
    
    init(filename: String = "ticketCommands.json") {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        self.fileURL = paths[0].appendingPathComponent(filename)
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
                            break
                        }
                        
                    }
                }
                let messageToPass = message.message
                let requestNewTicket = PyrusServiceDesk.multichats && messageToPass.ticketId == 0
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
        loadFromFile { result in
            switch result {
            case .success(var commands):
                commands.append(command)
                self.save(commands: commands, completion: completion)
            case .failure(let error):
                completion?(error)
            }
        }
    }
    
    func deleteCommand(withId commandId: String, completion: ((Error?) -> Void)? = nil) {
        loadFromFile { result in
            switch result {
            case .success(var commands):
                commands.removeAll(where: {$0.commandId.lowercased() == commandId.lowercased()})
                self.save(commands: commands, completion: completion)
            case .failure(let error):
                completion?(error)
            }
        }
    }
    
    func clear(completion: ((Error?) -> Void)? = nil) {
        commandsCache = []
        save(commands: [], completion: completion)
    }
}
