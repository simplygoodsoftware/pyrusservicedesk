import Foundation
import CoreData

final class PSDChatsDataService {

    private let coreDataService: CoreDataServiceProtocol
    
    init(coreDataService: CoreDataServiceProtocol) {
        self.coreDataService = coreDataService
    }
    
    init() {
        self.coreDataService = CoreDataService()
    }

}

extension PSDChatsDataService: PSDChatsDataServiceProtocol {
    func deleteAllObjects() {
        coreDataService.deleteAllObjects(forEntityName: "DBAttachment")
        coreDataService.deleteAllObjects(forEntityName: "DBMessage")
        coreDataService.deleteAllObjects(forEntityName: "DBChat")
        coreDataService.deleteAllObjects(forEntityName: "DBTicketCommand")
        coreDataService.deleteAllObjects(forEntityName: "DBClient")
    }
    
    func saveClientModels(with clientModels: [PSDClientInfo]) {
        coreDataService.deleteAllObjects(forEntityName: "DBClient")
        for clientModel in clientModels {
            saveClientModel(with: clientModel)
        }
    }
    
    func saveClientModel(with clientModel: PSDClientInfo) {
        coreDataService.save { context in
            let dbClient = DBClient(context: context)
            dbClient.appId = clientModel.clientId
            dbClient.name = clientModel.clientName
            dbClient.appIcon = clientModel.clientIcon
        }
    }
    
    func getAllClients() -> [PSDClientInfo] {
        do {
            let dbClients = try coreDataService.fetchClients()
            let clients: [PSDClientInfo] = dbClients.compactMap { dbClient in
                let client = PSDClientInfo(
                    clientId: dbClient.appId ?? "",
                    clientName: dbClient.name ?? "",
                    clientIcon: dbClient.appIcon ?? ""
                )
                return client
            }
            return clients
        } catch {
            print("\(error)")
            return []
        }
    }

    func saveChatModels(with chatModels: [PSDChat]) {
        coreDataService.deleteAllObjects(forEntityName: "DBChat")
        coreDataService.save { context in
            self.saveChatModel(with: chatModels, context: context)
        }
//        for chatModel in chatModels {
//            saveChatModel(with: chatModel)
//        }
    }
    
    func saveChatModel(with chatModels: [PSDChat], context: NSManagedObjectContext) {
       // coreDataService.save { context in
        for chatModel in chatModels {
            let dbChat = DBChat(context: context)
            dbChat.chatId = Int64(chatModel.chatId ?? 0)
            dbChat.date = chatModel.date
            dbChat.isActive = chatModel.isActive
            dbChat.isRead = chatModel.isRead
            dbChat.lastReadedCommentId = Int64(chatModel.lastReadedCommentId ?? 0)
            dbChat.showRating = chatModel.showRating
            dbChat.showRatingText = chatModel.showRatingText
            dbChat.subject = chatModel.subject
            dbChat.userId = chatModel.userId
            if dbChat.messages == nil {
                dbChat.messages = NSOrderedSet()
            }
            
            for message in chatModel.messages {
                let dbMessage = DBMessage(context: context)
                dbMessage.messageId = message.messageId
                dbMessage.appId = message.appId
                dbMessage.userId = message.userId
                dbMessage.clientId = message.clientId
                dbMessage.commandId = message.commandId
                dbMessage.date = message.date
                dbMessage.fromStorage = message.fromStrorage
                dbMessage.isOutgoing = message.isOutgoing
                dbMessage.isRatingMessage = message.isRatingMessage
                dbMessage.isWelcomeMessage = message.isWelcomeMessage
                dbMessage.requestNewTicket = message.requestNewTicket
                dbMessage.state = message.state.rawValue
                dbMessage.text = message.text
                dbMessage.ticketId = Int64(message.ticketId)
                dbMessage.authorId = message.owner.authorId
                dbMessage.authorName = message.owner.name
                dbMessage.authorAvatarId = message.owner.imagePath
                if let rating = message.rating {
                    dbMessage.rating = Int32(rating)
                }
                
                if dbMessage.attachments == nil {
                    dbMessage.attachments = NSOrderedSet()
                }
                
                if let attachments = message.attachments {
                    for attachment in attachments {
                        let dbAttachment = DBAttachment(context: context)
                        dbAttachment.name = attachment.name
                        dbAttachment.canOpen = attachment.canOpen
                        dbAttachment.data = attachment.data
                        dbAttachment.isImage = attachment.isImage
                        dbAttachment.isVideo = attachment.isVideo
                        dbAttachment.localId = attachment.localId
                        dbAttachment.serverIdentifier = attachment.serverIdentifer
                        dbAttachment.size = Int64(attachment.size)
                        dbAttachment.localPath = attachment.localPath
                        dbAttachment.uploadingProgress = Float(attachment.uploadingProgress)
                        
                        dbMessage.addToAttachments(dbAttachment)
                    }
                }
                
                dbChat.addToMessages(dbMessage)
            }
        }
    }
    
    func saveTicketCommand(with ticketCommand: TicketCommand) {
        coreDataService.save { context in
            let fetchRequest = DBTicketCommand.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", ticketCommand.commandId.lowercased() as CVarArg)
            if let dbTicketCommand = try? context.fetch(fetchRequest).first {
                guard let attachments = ticketCommand.params.attachments else { return }
                dbTicketCommand.attachments = NSOrderedSet()
                for attachmentData in attachments {
                    let dbAttachmentData = DBAttachmentData(context: context)
                    dbAttachmentData.guid = attachmentData.guid
                    dbAttachmentData.type = Int32(attachmentData.type)
                    dbAttachmentData.name = attachmentData.name
                    dbTicketCommand.addToAttachments(dbAttachmentData)
                }
                return
            }
            
            let dbTicketCommand = DBTicketCommand(context: context)
            dbTicketCommand.id = ticketCommand.commandId.lowercased()
            dbTicketCommand.appId = ticketCommand.appId
            dbTicketCommand.message = ticketCommand.params.message
            if let messageId = ticketCommand.params.messageId {
                dbTicketCommand.messageId = Int64(messageId)
            }
            if let requestNewTicket = ticketCommand.params.requestNewTicket {
                dbTicketCommand.requestNewTicket = requestNewTicket
            }
            if let ticketId = ticketCommand.params.ticketId {
                dbTicketCommand.ticketId = Int64(ticketId)
            }
            dbTicketCommand.token = ticketCommand.params.token
            dbTicketCommand.tokenType = ticketCommand.params.type
            dbTicketCommand.userId = ticketCommand.userId
            dbTicketCommand.type = Int32(ticketCommand.type)
            dbTicketCommand.date = ticketCommand.params.date
            dbTicketCommand.clientId = ticketCommand.params.messageClientId
            
            if dbTicketCommand.attachments == nil {
                dbTicketCommand.attachments = NSOrderedSet()
            }
            
            for attachmentData in ticketCommand.params.attachments ?? [] {
                let dbAttachmentData = DBAttachmentData(context: context)
                dbAttachmentData.guid = attachmentData.guid
                dbAttachmentData.type = Int32(attachmentData.type)
                dbAttachmentData.name = attachmentData.name
                dbTicketCommand.addToAttachments(dbAttachmentData)
            }            
        }
    }
    
    func deleteChats(chatModels: [PSDChat]) {
        for model in chatModels {
            do {
                try coreDataService.deleteChat(id: model.chatId ?? 0)
            } catch {
                print("Error deleting channel with ID: \(model.chatId), \(error)")
            }
        }
    }
    
    func getAllCommands() -> [TicketCommand] {
        do {
            let dbCommands = try coreDataService.fetchCommands()
            let commands: [TicketCommand] = dbCommands.compactMap { dbCommand in
                guard let commandId = dbCommand.id else { return nil }
                var attachmentsData: [AttachmentData]? = nil
                if let dbAttachments = dbCommand.attachments?.array as? [DBAttachmentData] {
                    let attachments: [AttachmentData] = dbAttachments.compactMap { dbAttachment in
                        let attachment = AttachmentData(
                            type: Int(dbAttachment.type),
                            name: dbAttachment.name ?? "",
                            guid: dbAttachment.guid
                        )
                        return attachment
                    }
                    attachmentsData = attachments
                }
                
                let command = TicketCommand(
                    commandId: dbCommand.id ?? "",
                    type: TicketCommandType(rawValue: Int(dbCommand.type)) ?? .readTicket,
                    appId: dbCommand.appId,
                    userId: dbCommand.userId,
                    params: TicketCommandParams(
                        ticketId: Int(dbCommand.ticketId),
                        appId: dbCommand.appId,
                        requestNewTicket: dbCommand.requestNewTicket,
                        userId: dbCommand.userId,
                        message: dbCommand.message,
                        attachments: attachmentsData,
                        authorId: dbCommand.authorId,
                        token: dbCommand.token,
                        type: dbCommand.tokenType,
                        messageId: Int(dbCommand.messageId),
                        date: dbCommand.date,
                        messageClientId: dbCommand.clientId
                    )
                )
                return command
            }
            return commands
        } catch {
            print("\(error)")
            return []
        }
    }

    func getAllChats() -> [PSDChat] {
        do {
            let dbChats = try coreDataService.fetchChats()
            let chats: [PSDChat] = dbChats.compactMap { dbChat in
                let chat = PSDChat(chatId: Int(dbChat.chatId), date: dbChat.date ?? Date(), messages: [])
                chat.subject = dbChat.subject
                chat.isActive = dbChat.isActive
                chat.userId = dbChat.userId
                chat.isRead = dbChat.isRead
                chat.showRating = dbChat.showRating
                chat.showRatingText = dbChat.showRatingText
                chat.lastReadedCommentId = Int(dbChat.lastReadedCommentId)
                
                if let dbMessages = dbChat.messages?.array as? [DBMessage] {
                    let messages: [PSDMessage] = dbMessages.compactMap { dbMessage in
                        guard dbMessage.text != nil || dbMessage.attachments?.count ?? 0 > 0 || dbMessage.rating != nil
                        else {
                            return nil
                        }
                        let user: PSDUser
                        if dbMessage.isOutgoing {
                            user = PSDUsers.user
                        } else {
                            user = PSDUsers.supportUsersContain(name: dbMessage.authorName ?? "", imagePath: dbMessage.authorAvatarId ?? "", authorId: dbMessage.authorId)
                        }
                        let message = PSDMessage(text: dbMessage.text, attachments: nil, messageId: dbMessage.messageId, owner: user, date: dbMessage.date)
                        message.messageId = dbMessage.messageId ?? ""
                        message.appId = dbMessage.appId
                        message.userId = dbMessage.userId
                        message.clientId = dbMessage.clientId ?? ""
                        message.commandId = dbMessage.commandId
                        message.fromStrorage = dbMessage.fromStorage
                        message.isOutgoing = dbMessage.isOutgoing
                        message.isRatingMessage = dbMessage.isRatingMessage
                        message.isWelcomeMessage = dbMessage.isWelcomeMessage
                        message.requestNewTicket = dbMessage.requestNewTicket
                        message.ticketId = Int(dbMessage.ticketId)
                        message.rating = Int(dbMessage.rating)
                        
                        switch dbMessage.state {
                        case 0:
                            message.state = .sending
                        case 1:
                            message.state = .sent
                        default:
                            message.state = .cantSend
                        }
                        
                        if let dbAttachments = dbMessage.attachments?.array as? [DBAttachment] {
                            let attachments: [PSDAttachment] = dbAttachments.compactMap { dbAttachment in
                                let attachment = PSDAttachment(localPath: dbAttachment.localPath, data: dbAttachment.data, serverIdentifer: dbAttachment.serverIdentifier)
                                attachment.name = dbAttachment.name ?? ""
                                attachment.uploadingProgress = CGFloat(dbAttachment.uploadingProgress)
                                attachment.isImage = dbAttachment.isImage
                                attachment.isVideo = dbAttachment.isVideo
                                attachment.size = Int(dbAttachment.size)
                                attachment.localId = dbAttachment.localId ?? ""
                                return attachment
                            }
                            message.attachments = attachments
                        }
                        
                        return message
                    }
                    chat.messages = messages
                    if let lastMessage = messages.last, let userId = chat.userId {
                        chat.lastComment = lastMessage
                        if PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId == userId,
                           PyrusServiceDesk.lastNoteId ?? 0 < Int(lastMessage.messageId) ?? 0 {
                            PyrusServiceDesk.lastNoteId = Int(lastMessage.messageId)
                        } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == userId }),
                                  user.lastNoteId ?? 0 < Int(lastMessage.messageId) ?? 0 {
                            user.lastNoteId = Int(lastMessage.messageId)
                        }
                    }
                }
                return chat
            }
            return chats
        } catch {
            print("\(error)")
            return []
        }
    }

    func deleteCommand(with id: String, serverTicketId: Int?) {
        if let serverTicketId {
            coreDataService.save { context in
                let fetchRequest = DBTicketCommand.fetchRequest()
                fetchRequest.predicate = NSPredicate(format: "id == %@", id.lowercased() as CVarArg)
                if let dbTicketCommand = try? context.fetch(fetchRequest).first,
                   dbTicketCommand.requestNewTicket,
                   dbTicketCommand.ticketId < 0 {
                    let newId = (dbTicketCommand.ticketId, serverTicketId)
                    let fetchRequest = DBTicketCommand.fetchRequest()
                    fetchRequest.predicate = NSPredicate(format: "id == %lld", Int64(dbTicketCommand.ticketId))
                    if let dbCommands = try? context.fetch(fetchRequest) {
                        for dbCommand in dbCommands {
                            if dbCommand.ticketId == newId.0 {
                                dbCommand.ticketId = Int64(newId.1)
                            }
                        }
                    }
                }
            }
        }
        
        do {
            try coreDataService.deleteCommand(id: id)
        } catch {
            print("\(error)")
        }
    }
    
    func deleteChannel(with chatId: Int) {
        do {
            try coreDataService.deleteChat(id: chatId)
        } catch {
            print("\(error)")
        }
    }
}
