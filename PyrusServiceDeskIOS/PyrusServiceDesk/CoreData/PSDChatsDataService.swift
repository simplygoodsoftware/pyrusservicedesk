import Foundation
import UIKit
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
        coreDataService.deleteAllObjects(forEntityName: "DBClient")
        coreDataService.deleteAllObjects(forEntityName: "DBAttachment")
        coreDataService.deleteAllObjects(forEntityName: "DBMessage")
        coreDataService.deleteAllObjects(forEntityName: "DBChat")
        coreDataService.deleteAllObjects(forEntityName: "DBTicketCommand")
        coreDataService.deleteAllObjects(forEntityName: "DBAnnouncement")
    }
    
    // MARK: Clients
    
    func saveClientModels(with clientModels: [PSDClientInfo]) {
        let ids = clientModels.compactMap({ $0.clientId })
        do {
            if ids.count > 0 {
                try coreDataService.deleteClients(ids: ids)
            }
        } catch {
            print(error)
        }
        
        coreDataService.save(completion: nil) { [weak self] context in
            self?.saveClientModel(with: clientModels, context: context)
        }
    }
    
    func saveClientModel(with clientModels: [PSDClientInfo], context: NSManagedObjectContext) {
        for clientModel in clientModels {
            let fetchRequest = DBClient.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "appId == %@", clientModel.clientId as CVarArg)
            
            let dbClient: DBClient
            if let client = try? context.fetch(fetchRequest).first {
                dbClient = client
            } else {
                dbClient = NSEntityDescription.insertNewObject(forEntityName: "DBClient", into: context) as! DBClient
            }
            
            dbClient.appId = clientModel.clientId
            dbClient.name = clientModel.clientName
            dbClient.appIcon = clientModel.clientIcon
            dbClient.descr = clientModel.clientDescription
            if clientModel.welcomeMessage?.count ?? 0 > 0 {
                dbClient.welcomeMessage = clientModel.welcomeMessage
            }
            if let settings = clientModel.ratingSettings {
                dbClient.ratingType = Int32(settings.type)
                dbClient.ratingSize = Int16(settings.size)
                dbClient.ratingText = settings.ratingText
            }
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
                client.clientDescription = dbClient.descr
                if dbClient.welcomeMessage?.count ?? 0 > 0 {
                    client.welcomeMessage = dbClient.welcomeMessage
                }
                if dbClient.ratingType != 0,
                   dbClient.ratingSize != 0 {
                    client.ratingSettings = PSDRatingSettings(
                        size: Int(dbClient.ratingSize),
                        type: Int(dbClient.ratingType),
                        ratingTextValues: [],
                        ratingText: dbClient.ratingText
                    )
                }
                return client
            }
            return clients
        } catch {
            print("\(error)")
            return []
        }
    }
    
    // MARK: Search
    
    func searchMessages(searchString: String) -> [SearchChatModel] {
        do {
            let dbChats = try coreDataService.fetchChats(searchString: searchString)
            let dbMessages = try coreDataService.fetchMessages(searchString: searchString)
            var chatModels = [SearchChatModel]()
            
            for dbChat in dbChats {
                let chat = PyrusServiceDesk.chats.first { $0.chatId == Int(dbChat.chatId) }
                let authorName = chat?.messages.last?.owner?.authorId == PyrusServiceDesk.authorId
                    ? "Вы"
                    :  chat?.messages.last?.owner?.name ?? ""
                var model = SearchChatModel(
                    id: chat?.chatId ?? 0,
                    date: chat?.lastComment?.date ?? Date(),
                    subject: chat?.subject ?? "",
                    messageText: chat?.lastComment?.text ?? "",
                    messageId: chat?.lastComment?.messageId ?? "",
                    authorName: authorName,
                    isMessage: false, messageAttributedText: NSAttributedString(string: "")
                )
                model.lastMessage = chat?.messages.last
                chatModels.append(model)
            }
            
            for dbMessage in dbMessages {
                if let dbChat = dbMessage.chat {
                    let authorName = dbMessage.authorId == PyrusServiceDesk.authorId
                        ? "Вы"
                        : dbMessage.authorName ?? ""
                    let model = SearchChatModel(
                        id: Int(dbChat.chatId),
                        date: dbMessage.date ?? Date(),
                        subject: dbChat.subject ?? "",
                        messageText: dbMessage.text ?? "",
                        messageId: dbMessage.messageId ?? "",
                        authorName: authorName,
                        isMessage: true, messageAttributedText: NSAttributedString(string: "")
                    )
                    chatModels.append(model)
                }
            }
            return chatModels
        } catch {
            print("Ошибка при выполнении запроса: \(error)")
            return []
        }
    }
    
    func searchMessages(searchString: String, completion: @escaping (Result<[SearchChatModel], Error>) -> Void) {
        coreDataService.fetchChatsAndMessages(searchString: searchString) { result in
            switch result {
            case .success(let (dbMessages, dbChats)):
                var chatModels = [SearchChatModel]()
                for dbChat in dbChats {
                    let chat = PyrusServiceDesk.chats.first { $0.chatId == Int(dbChat.chatId) }
                    let authorName = chat?.messages.last?.owner?.authorId == PyrusServiceDesk.authorId
                    ? "Вы"
                    :  chat?.messages.last?.owner?.name ?? ""
                    var model = SearchChatModel(
                        id: chat?.chatId ?? 0,
                        date: chat?.lastComment?.date ?? Date(),
                        subject: chat?.subject ?? "",
                        messageText: chat?.lastComment?.text ?? "",
                        messageId: chat?.lastComment?.messageId ?? "",
                        authorName: authorName,
                        isMessage: false,
                        messageAttributedText: chat?.lastMessageAttrText ?? AttributedStringCache.cachedString(for: chat?.lastComment?.text ?? "", fontColor: .lastMessageInfo, font: .lastMessageInfo, key: chat?.lastComment?.messageId ?? "")
                    )
                    model.lastMessage = chat?.messages.last
                    chatModels.append(model)
                }
                
                for dbMessage in dbMessages {
                    if let dbChat = dbMessage.chat {
                        let authorName = dbMessage.authorId == PyrusServiceDesk.authorId
                        ? "Вы"
                        : dbMessage.authorName ?? ""
                        let model = SearchChatModel(
                            id: Int(dbChat.chatId),
                            date: dbMessage.date ?? Date(),
                            subject: dbChat.subject ?? "",
                            messageText: dbMessage.text ?? "",
                            messageId: dbMessage.messageId ?? "",
                            authorName: authorName,
                            isMessage: true,
                            messageAttributedText: AttributedStringCache.cachedString(for: dbMessage.text ?? "", fontColor: .lastMessageInfo, font: .lastMessageInfo, key: dbMessage.messageId ?? "")
                        )
                        chatModels.append(model)
                    }
                }
                completion(.success(chatModels))
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    // MARK: - Chats

    func saveChatModels(with chatModels: [PSDChat], completion: ((Result<Void, Error>) -> Void)?) {
        let ids = chatModels.compactMap({ Int64($0.chatId ?? 0) })
        do {
            if ids.count > 0 {
                try coreDataService.deleteChats(ids: ids)
            } else if chatModels.count == 0 {
                coreDataService.deleteAllObjects(forEntityName: "DBChat")
            }
        } catch {
            print(error)
        }
        DispatchQueue.global().async { [weak self] in
            self?.coreDataService.save(completion: completion) { [weak self] context in
                do {
                    try self?.saveChatModels(with: chatModels, context: context)
                } catch { }
            }
        }
    }
    
    func saveChatModels(
        with chatModels: [PSDChat],
        context: NSManagedObjectContext
    ) throws {

        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        context.undoManager = nil

        // Collect IDs

        let chatIds: [Int64] = chatModels.compactMap {
            guard let id = $0.chatId else { return nil }
            return Int64(id)
        }

        let allMessages = chatModels.flatMap { $0.messages }
            .filter { ($0.rating ?? 0) <= 0 }

        let messageIds = allMessages.map { $0.messageId }

        let attachmentIds = allMessages
            .compactMap { $0.attachments }
            .flatMap { $0 }
            .compactMap { $0.serverIdentifer }

        // Fetch existing chats

        let chatRequest: NSFetchRequest<DBChat> = DBChat.fetchRequest()
        chatRequest.predicate = NSPredicate(format: "chatId IN %@", chatIds)

        let existingChats = try context.fetch(chatRequest)
        let chatsById = Dictionary(uniqueKeysWithValues: existingChats.map {
            ($0.chatId, $0)
        })

        // Fetch existing messages

        let messageRequest: NSFetchRequest<DBMessage> = DBMessage.fetchRequest()
        messageRequest.predicate = NSPredicate(format: "messageId IN %@", messageIds)

        let existingMessages = try context.fetch(messageRequest)
        let messagesById = Dictionary(uniqueKeysWithValues: existingMessages.map {
            ($0.messageId, $0)
        })

        // Fetch existing attachments

        let attachmentRequest: NSFetchRequest<DBAttachment> = DBAttachment.fetchRequest()
        attachmentRequest.predicate = NSPredicate(format: "serverIdentifier IN %@", attachmentIds)

        let existingAttachments = try context.fetch(attachmentRequest)
        let attachmentsById: [String: DBAttachment] = Dictionary(
            uniqueKeysWithValues: existingAttachments.compactMap {
                guard let id = $0.serverIdentifier else { return nil }
                return (id, $0)
            }
        )


        // Upsert chats, messages, attachments

        for chatModel in chatModels {

            guard let chatIdValue = chatModel.chatId else { continue }
            let chatId = Int64(chatIdValue)

            let dbChat = chatsById[chatId] ?? DBChat(context: context)

            dbChat.chatId = chatId
            dbChat.date = chatModel.date
            dbChat.isActive = chatModel.isActive
            dbChat.isRead = chatModel.isRead
            dbChat.lastReadedCommentId = Int64(chatModel.lastReadedCommentId ?? 0)
            dbChat.showRating = chatModel.showRating
            dbChat.showRatingText = chatModel.showRatingText
            dbChat.subject = chatModel.subject
            dbChat.userId = chatModel.userId

            if let appId = chatModel.appId, !appId.isEmpty {
                dbChat.appId = appId
            }

            for message in chatModel.messages where (message.rating ?? 0) <= 0 {

                let dbMessage =
                    messagesById[message.messageId] ??
                    DBMessage(context: context)
                
                dbMessage.messageId = message.messageId
                dbMessage.appId = message.appId
                dbMessage.userId = message.userId
                dbMessage.clientId = message.clientId
                dbMessage.commandId = message.commandId
                dbMessage.date = message.date
                dbMessage.fromStorage = message.fromStrorage
                dbMessage.isOutgoing = message.isSupportMessage
                dbMessage.isRatingMessage = message.isRatingMessage
                dbMessage.isWelcomeMessage = message.isWelcomeMessage
                dbMessage.requestNewTicket = message.requestNewTicket
                dbMessage.state = message.state.rawValue
                dbMessage.text = message.text
                dbMessage.ticketId = Int64(message.ticketId)
                dbMessage.authorId = message.owner?.authorId
                dbMessage.authorName = message.owner?.name
                dbMessage.authorAvatarId = message.owner?.imagePath
                dbMessage.isSystem = message.isSystemMessage

                if let rating = message.rating {
                    dbMessage.rating = Int32(rating)
                }

                for attachment in message.attachments ?? [] {

                    guard let serverId = attachment.serverIdentifer else { continue }

                    let dbAttachment =
                        attachmentsById[serverId] ??
                        DBAttachment(context: context)

                    dbAttachment.serverIdentifier = serverId
                    dbAttachment.name = attachment.name
                    dbAttachment.canOpen = attachment.canOpen
                    dbAttachment.data = attachment.data
                    dbAttachment.isImage = attachment.isImage
                    dbAttachment.isVideo = attachment.isVideo
                    dbAttachment.localId = attachment.localId
                    dbAttachment.size = Int64(attachment.size)
                    dbAttachment.localPath = attachment.localPath
                    dbAttachment.uploadingProgress = Float(attachment.uploadingProgress)

                    dbMessage.addToAttachments(dbAttachment)
                }

                dbChat.addToMessages(dbMessage)
            }
        }
    }
    
    func getAllChats(completion: @escaping ([PSDChat]) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self else { return }
            do {
                let dbChats = try coreDataService.fetchChats()
                let chats: [PSDChat] = dbChats.compactMap { dbChat in
                    let chat = PSDChat(chatId: Int(dbChat.chatId),
                                    date: dbChat.date ?? Date(),
                                    messages: [])
                    chat.subject = dbChat.subject
                    chat.isActive = dbChat.isActive
                    chat.userId = dbChat.userId
                    chat.isRead = dbChat.isRead
                    chat.showRating = dbChat.showRating
                    chat.showRatingText = dbChat.showRatingText
                    chat.lastReadedCommentId = Int(dbChat.lastReadedCommentId)
                    chat.appId = dbChat.appId
                    
                    if let dbMessages = dbChat.messages?.array as? [DBMessage] {
                        let messages: [PSDMessage] = dbMessages.compactMap { dbMessage in
                            guard dbMessage.text != nil ||
                                  dbMessage.attachments?.count ?? 0 > 0 ||
                                  (dbMessage.rating != 0) else {
                                return nil
                            }
                            
                            let user: PSDUser? = dbMessage.authorId == PyrusServiceDesk.authorId ?
                                PSDUsers.user :
                                PSDUsers.supportUsersContain(
                                    name: dbMessage.authorName ?? "",
                                    imagePath: dbMessage.authorAvatarId ?? "",
                                    authorId: dbMessage.authorId
                                )
                            
                            let message = PSDMessage(
                                text: dbMessage.text,
                                attachments: nil,
                                messageId: dbMessage.messageId,
                                owner: user,
                                date: dbMessage.date
                            )
                            
                            // Настройка свойств сообщения
                            message.messageId = dbMessage.messageId ?? ""
                            message.appId = dbMessage.appId
                            message.userId = dbMessage.userId
                            message.clientId = dbMessage.clientId ?? ""
                            message.commandId = dbMessage.commandId
                            message.fromStrorage = dbMessage.fromStorage
                            message.isOutgoing = dbMessage.authorId == PyrusServiceDesk.authorId
                            message.isRatingMessage = dbMessage.isRatingMessage
                            message.isWelcomeMessage = dbMessage.isWelcomeMessage
                            message.requestNewTicket = dbMessage.requestNewTicket
                            message.ticketId = Int(dbMessage.ticketId)
                            message.rating = Int(dbMessage.rating)
                            message.isSupportMessage = !dbMessage.isOutgoing
                            message.isSystemMessage = dbMessage.isSystem
                            
                            // Обработка состояния сообщения
                            switch dbMessage.state {
                            case 0: message.state = .sending
                            case 1: message.state = .sent
                            default: message.state = .cantSend
                            }
                            
                            // Обработка вложений
                            if let dbAttachments = dbMessage.attachments?.array as? [DBAttachment] {
                                let attachments: [PSDAttachment] = dbAttachments.compactMap { dbAttachment in
                                    let attachment = PSDAttachment(
                                        localPath: dbAttachment.localPath,
                                        data: dbAttachment.data,
                                        serverIdentifer: dbAttachment.serverIdentifier
                                    )
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
                
                DispatchQueue.main.async {
                    completion(chats)
                }
                
            } catch {
                print("Error fetching chats: \(error)")
                DispatchQueue.main.async {
                    completion([])
                }
            }
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
                chat.appId = dbChat.appId
                
                if let dbMessages = dbChat.messages?.array as? [DBMessage] {
                    let messages: [PSDMessage] = dbMessages.compactMap { dbMessage in
                        guard dbMessage.text != nil || dbMessage.attachments?.count ?? 0 > 0 || (dbMessage.rating != 0)
                        else {
                            return nil
                        }
                        let user: PSDUser?
                        if dbMessage.authorId == PyrusServiceDesk.authorId {
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
                        message.isOutgoing = dbMessage.authorId == PyrusServiceDesk.authorId//dbMessage.isOutgoing
                        message.isRatingMessage = dbMessage.isRatingMessage
                        message.isWelcomeMessage = dbMessage.isWelcomeMessage
                        message.requestNewTicket = dbMessage.requestNewTicket
                        message.ticketId = Int(dbMessage.ticketId)
                        message.rating = Int(dbMessage.rating)
                        message.isSupportMessage = !dbMessage.isOutgoing
                        message.isSystemMessage = dbMessage.isSystem
                        
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
    
    func getChatsHeaders() -> [PSDChat] {
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
                
                return chat
            }
            return chats
        } catch {
            print("\(error)")
            return []
        }
    }
    
    func getChatsHeaders(completion: @escaping ([PSDChat]) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self else { return }
            do {
                let dbChats = try coreDataService.fetchChats()
                let chats: [PSDChat] = dbChats.compactMap { dbChat in
                    let chat = PSDChat(chatId: Int(dbChat.chatId),
                                       date: dbChat.date ?? Date(),
                                       messages: [])
                    chat.subject = dbChat.subject
                    chat.isActive = dbChat.isActive
                    chat.userId = dbChat.userId
                    chat.isRead = dbChat.isRead
                    chat.showRating = dbChat.showRating
                    chat.showRatingText = dbChat.showRatingText
                    chat.lastReadedCommentId = Int(dbChat.lastReadedCommentId)
                    
                    return chat
                }
                
                DispatchQueue.main.async {
                    completion(chats)
                }
                
            } catch {
                print("Error fetching chats: \(error)")
                DispatchQueue.main.async {
                    completion([])
                }
            }
        }
    }
    
    // MARK: - Commands
    
    func saveTicketCommand(with ticketCommand: TicketCommand, completion: ((Result<Void, Error>) -> Void)?) {
        coreDataService.save(completion: completion)  { context in
            let fetchRequest = DBTicketCommand.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", ticketCommand.commandId.lowercased() as CVarArg)
            if let dbTicketCommand = try? context.fetch(fetchRequest).first {
                guard let attachments = ticketCommand.params.attachments else { return }
                dbTicketCommand.attachments = NSOrderedSet()
                for attachmentData in attachments {
                    let dbAttachmentData = NSEntityDescription.insertNewObject(forEntityName: "DBAttachmentData", into: context) as! DBAttachmentData
                    dbAttachmentData.guid = attachmentData.guid
                    dbAttachmentData.type = Int32(attachmentData.type)
                    dbAttachmentData.name = attachmentData.name
                    dbTicketCommand.addToAttachments(dbAttachmentData)
                }
                return
            }
            
            let dbTicketCommand = NSEntityDescription.insertNewObject(forEntityName: "DBTicketCommand", into: context) as! DBTicketCommand
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
            if let rating = ticketCommand.params.rating {
                dbTicketCommand.rating = Int32(rating)
            }
            if let hasAccess = ticketCommand.params.hasAccess {
                dbTicketCommand.hasAccess = hasAccess
            }
            dbTicketCommand.token = ticketCommand.params.token
            dbTicketCommand.tokenType = ticketCommand.params.type
            dbTicketCommand.userId = ticketCommand.userId
            dbTicketCommand.type = Int32(ticketCommand.type)
            dbTicketCommand.date = ticketCommand.params.date
            dbTicketCommand.clientId = ticketCommand.params.messageClientId
            dbTicketCommand.authorId = ticketCommand.params.authorId
            dbTicketCommand.ratingComment = ticketCommand.params.ratingComment
            dbTicketCommand.lastReadAnnouncementId = ticketCommand.params.lastReadAnnouncementId
            
            let attachments = dbTicketCommand.mutableOrderedSetValue(forKey: "attachments")
//            if dbTicketCommand.attachments == nil {
//                dbTicketCommand.attachments = NSOrderedSet()
//            }
            
            for attachmentData in ticketCommand.params.attachments ?? [] {
                let dbAttachmentData = NSEntityDescription.insertNewObject(forEntityName: "DBAttachmentData", into: context) as! DBAttachmentData
                dbAttachmentData.guid = attachmentData.guid
                dbAttachmentData.type = Int32(attachmentData.type)
                dbAttachmentData.name = attachmentData.name
                if !attachments.contains(dbAttachmentData) { attachments.add(dbAttachmentData)
                }
//                dbTicketCommand.addToAttachments(dbAttachmentData)
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
                        ticketId: dbCommand.ticketId == 0 ? nil : Int(dbCommand.ticketId),
                        appId: dbCommand.appId,
                        requestNewTicket: dbCommand.requestNewTicket,
                        userId: dbCommand.userId,
                        message: dbCommand.message,
                        attachments: attachmentsData,
                        authorId: dbCommand.authorId,
                        token: dbCommand.token,
                        type: dbCommand.tokenType,
                        messageId: dbCommand.messageId == 0 ? nil : Int(dbCommand.messageId),
                        rating: Int(dbCommand.rating) == 0 ? nil : Int(dbCommand.rating),
                        ratingComment: dbCommand.ratingComment,
                        date: dbCommand.date,
                        messageClientId: dbCommand.clientId,
                        hasAccess: dbCommand.hasAccess,
                        extraFields: dbCommand.requestNewTicket ? PyrusServiceDesk.fieldsData : nil,
                        lastReadAnnouncementId: dbCommand.lastReadAnnouncementId
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
            
    func resaveBeforeDeleteCommand(commanId: String, serverTicketId: Int?, completion: ((Result<Void, Error>) -> Void)?) {
        if let serverTicketId {
            coreDataService.save(completion: completion) { context in
                let fetchRequest = DBTicketCommand.fetchRequest()
                fetchRequest.predicate = NSPredicate(format: "id == %@", commanId.lowercased() as CVarArg)
                if let dbTicketCommand = try? context.fetch(fetchRequest).first,
                   dbTicketCommand.requestNewTicket,
                   dbTicketCommand.ticketId < 0 {
                    let newId = (dbTicketCommand.ticketId, serverTicketId)
                    let fetchRequest = DBTicketCommand.fetchRequest()
                    fetchRequest.predicate = NSPredicate(format: "ticketId == %lld", Int64(dbTicketCommand.ticketId))
                    if let dbCommands = try? context.fetch(fetchRequest) {
                        for dbCommand in dbCommands {
                            dbCommand.ticketId = Int64(newId.1)
                        }
                    }
                }
            }
        }
    }
    
    func deleteCommand(with id: String, serverTicketId: Int?) {
        do {
            try coreDataService.deleteCommand(id: id)
        } catch {
            print("\(error)")
        }
    }
    
    // MARK: Messages
    
    func getAllMessages(completion: @escaping ([PSDMessage]) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self else { return }
            do {
                let dbMessages = try coreDataService.fetchMessages()
                
                let messages: [PSDMessage] = dbMessages.compactMap { dbMessage in
                    guard dbMessage.text != nil || dbMessage.attachments?.count ?? 0 > 0 || (dbMessage.rating > 0) else {
                        return nil
                    }
                    
                    let user: PSDUser? = dbMessage.authorId == PyrusServiceDesk.authorId ?
                    PSDUsers.user :
                    PSDUsers.supportUsersContain(
                        name: dbMessage.authorName ?? "",
                        imagePath: dbMessage.authorAvatarId ?? "",
                        authorId: dbMessage.authorId
                    )
                    
                    let message = PSDMessage(
                        text: dbMessage.text,
                        attachments: nil,
                        messageId: dbMessage.messageId,
                        owner: user,
                        date: dbMessage.date
                    )
                    
                    // Настройка свойств сообщения
                    message.messageId = dbMessage.messageId ?? ""
                    message.appId = dbMessage.appId
                    message.userId = dbMessage.userId
                    message.clientId = dbMessage.clientId ?? ""
                    message.commandId = dbMessage.commandId
                    message.fromStrorage = dbMessage.fromStorage
                    message.isOutgoing = dbMessage.authorId == PyrusServiceDesk.authorId
                    message.isRatingMessage = dbMessage.isRatingMessage
                    message.isWelcomeMessage = dbMessage.isWelcomeMessage
                    message.requestNewTicket = dbMessage.requestNewTicket
                    message.ticketId = Int(dbMessage.ticketId)
                    message.rating = Int(dbMessage.rating)
                    message.isSupportMessage = !dbMessage.isOutgoing
                    message.isSystemMessage = dbMessage.isSystem
                    
                    // Обработка состояния сообщения
                    switch dbMessage.state {
                    case 0: message.state = .sending
                    case 1: message.state = .sent
                    default: message.state = .cantSend
                    }
                    
                    // Обработка вложений
                    if let dbAttachments = dbMessage.attachments?.array as? [DBAttachment] {
                        let attachments: [PSDAttachment] = dbAttachments.compactMap { dbAttachment in
                            let attachment = PSDAttachment(
                                localPath: dbAttachment.localPath,
                                data: dbAttachment.data,
                                serverIdentifer: dbAttachment.serverIdentifier
                            )
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
                
                if let lastMessage = messages.last {
                    PyrusServiceDesk.lastNoteId = Int(lastMessage.messageId)
                } else {
                    PyrusServiceDesk.lastNoteId = 0
                }
                
                DispatchQueue.main.async {
                    completion(messages)
                }
                
            } catch {
                print("Error fetching messages: \(error)")
                DispatchQueue.main.async {
                    completion([])
                }
            }
        }
    }
    
    func getAllMessages() -> [PSDMessage] {
        do {
            let dbMessages = try coreDataService.fetchMessages()
            let messages: [PSDMessage] = dbMessages.compactMap { dbMessage in
                guard dbMessage.text != nil || dbMessage.attachments?.count ?? 0 > 0 || (dbMessage.rating > 0) else {
                    return nil
                }
                
                let user: PSDUser? = dbMessage.authorId == PyrusServiceDesk.authorId ?
                PSDUsers.user :
                PSDUsers.supportUsersContain(
                    name: dbMessage.authorName ?? "",
                    imagePath: dbMessage.authorAvatarId ?? "",
                    authorId: dbMessage.authorId
                )
                
                let message = PSDMessage(
                    text: dbMessage.text,
                    attachments: nil,
                    messageId: dbMessage.messageId,
                    owner: user,
                    date: dbMessage.date
                )
                
                // Настройка свойств сообщения
                message.messageId = dbMessage.messageId ?? ""
                message.appId = dbMessage.appId
                message.userId = dbMessage.userId
                message.clientId = dbMessage.clientId ?? ""
                message.commandId = dbMessage.commandId
                message.fromStrorage = dbMessage.fromStorage
                message.isOutgoing = dbMessage.authorId == PyrusServiceDesk.authorId
                message.isRatingMessage = dbMessage.isRatingMessage
                message.isWelcomeMessage = dbMessage.isWelcomeMessage
                message.requestNewTicket = dbMessage.requestNewTicket
                message.ticketId = Int(dbMessage.ticketId)
                message.rating = Int(dbMessage.rating)
                message.isSupportMessage = !dbMessage.isOutgoing
                message.isSystemMessage = dbMessage.isSystem
                
                // Обработка состояния сообщения
                switch dbMessage.state {
                case 0: message.state = .sending
                case 1: message.state = .sent
                default: message.state = .cantSend
                }
                
                // Обработка вложений
                if let dbAttachments = dbMessage.attachments?.array as? [DBAttachment] {
                    let attachments: [PSDAttachment] = dbAttachments.compactMap { dbAttachment in
                        let attachment = PSDAttachment(
                            localPath: dbAttachment.localPath,
                            data: dbAttachment.data,
                            serverIdentifer: dbAttachment.serverIdentifier
                        )
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
            
            if let lastMessage = messages.last {
                PyrusServiceDesk.lastNoteId = Int(lastMessage.messageId)
            } else {
                PyrusServiceDesk.lastNoteId = 0
            }
            
            return messages
            
        } catch {
            print("Error fetching messages: \(error)")
            return []
        }
    }
    
    // MARK: - Announcements

    func saveAnnouncementsModels(with announcements: [PSDAnnouncement], completion: ((Result<Void, Error>) -> Void)?) {
        DispatchQueue.global().async { [weak self] in
            self?.coreDataService.save(completion: completion) { [weak self] context in
                do {
                    try self?.saveAnnouncements(with: announcements, context: context)
                } catch { }
            }
        }
    }
    
    func saveAnnouncements(
        with announcements: [PSDAnnouncement],
        context: NSManagedObjectContext
    ) throws {

        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        context.undoManager = nil

        // 1) Соберём идентификаторы
        let announcementIds: [String] = announcements.compactMap { $0.id }
        let allAttachments = announcements.flatMap { $0.attachments }
        let attachmentIds: [String] = allAttachments.compactMap { $0.id }

        // 2) Получим уже существующие анонсы
        let annReq: NSFetchRequest<DBAnnouncement> = DBAnnouncement.fetchRequest()
        annReq.predicate = NSPredicate(format: "id IN %@", announcementIds)
        let existingAnnouncements = try context.fetch(annReq)
        let announcementsById: [String: DBAnnouncement] = Dictionary(
            uniqueKeysWithValues: existingAnnouncements.compactMap {
                guard let id = $0.id else { return nil }
                return (id, $0)
            }
        )

        // 3) Получим уже существующие вложения анонсов
        let attReq: NSFetchRequest<DBAnnouncementAttachment> = DBAnnouncementAttachment.fetchRequest()
        attReq.predicate = NSPredicate(format: "id IN %@", attachmentIds)
        let existingAtts = try context.fetch(attReq)
        let attachmentsById: [String: DBAnnouncementAttachment] = Dictionary(
            uniqueKeysWithValues: existingAtts.compactMap {
                guard let id = $0.id else { return nil }
                return (id, $0)
            }
        )

        // 4) Апсерт анонсов и их вложений
        for model in announcements {
            let id = model.id

            let dbAnn = announcementsById[id] ?? DBAnnouncement(context: context)
            dbAnn.id = id
            dbAnn.appId = model.appId
            dbAnn.date = model.date
            dbAnn.isRead = model.isRead
            dbAnn.text = model.text

            // Вложения
            for a in model.attachments {
                let attId = a.id
                let dbAtt = attachmentsById[attId] ?? DBAnnouncementAttachment(context: context)
                
                dbAtt.id = attId
                dbAtt.name = a.name
                dbAtt.size = Int64(a.size)
                dbAtt.media = a.media
                dbAtt.width = Int64(a.width)
                dbAtt.height = Int64(a.height)
                
                dbAnn.addToAttachments(dbAtt)
            }
        }
    }
    
    func getAllAnnouncements(completion: @escaping ([PSDAnnouncement]) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self else { return }
            
            do {
                let dbAnns = try coreDataService.fetchAnnouncements()
                
                let anns: [PSDAnnouncement] = dbAnns.compactMap { db in
                    var ann = PSDAnnouncement(
                        id: db.id ?? "",
                        text: db.text,
                        date: db.date ?? Date(),
                        isRead: db.isRead,
                        attachments: [],
                        appId: db.appId ?? ""
                    )
                    
                    if let dbAtts = db.attachments?.array as? [DBAnnouncementAttachment] {
                        let atts: [PSDAnnouncementAttachment] = dbAtts.map { dba in
                            let a = PSDAnnouncementAttachment(
                                id: dba.id ?? "",
                                name: dba.name,
                                size: Int(dba.size),
                                width: Int(dba.width),
                                height: Int(dba.height),
                                media: dba.media
                            )
                            return a
                        }
                        ann.attachments = atts
                    }
                    
                    return ann
                }
                
                DispatchQueue.main.async { completion(anns) }
                
            } catch {
                print("Error fetching announcements: \(error)")
                DispatchQueue.main.async { completion([]) }
            }
        }
    }
        
    func getAllAnnouncements() -> [PSDAnnouncement] {
        do {
            let dbAnns = try coreDataService.fetchAnnouncements()
            
            let anns: [PSDAnnouncement] = dbAnns.compactMap { db in
                var ann = PSDAnnouncement(
                    id: db.id ?? "",
                    text: db.text,
                    date: db.date ?? Date(),
                    isRead: db.isRead,
                    attachments: [],
                    appId: db.appId ?? ""
                )
                
                if let dbAtts = db.attachments?.array as? [DBAnnouncementAttachment] {
                    let atts: [PSDAnnouncementAttachment] = dbAtts.map { dba in
                        let a = PSDAnnouncementAttachment(
                            id: dba.id ?? "",
                            name: dba.name,
                            size: Int(dba.size),
                            width: Int(dba.width),
                            height: Int(dba.height),
                            media: dba.media
                        )
                        return a
                    }
                    ann.attachments = atts
                }
                
                return ann
            }
            
            return anns
            
        } catch {
            print("Error fetching announcements: \(error)")
            return []
        }
    }


}

private extension UIFont {
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}

private extension UIColor {
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}
