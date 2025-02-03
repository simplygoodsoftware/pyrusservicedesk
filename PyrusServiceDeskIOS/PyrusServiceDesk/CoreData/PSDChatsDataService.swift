import Foundation

final class PSDChatsDataService {

    private let coreDataService: CoreDataServiceProtocol
    
    init(coreDataService: CoreDataServiceProtocol) {
        self.coreDataService = coreDataService
    }

}

extension PSDChatsDataService: PSDChatsDataServiceProtocol {

    func saveChatModels(with chatModels: [PSDChat]) {
        coreDataService.deleteAllObjects(forEntityName: "DBChat")
        for chatModel in chatModels {
            saveChatModel(with: chatModel)
        }
    }
    
    func saveChatModel(with chatModel: PSDChat) {
        coreDataService.save { context in
            let dbChat = DBChat(context: context)
            dbChat.chatId = Int64(chatModel.chatId ?? 0)//chatModel.chatId as? Int64 ?? 0
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
    
//    func saveMessagesModels(with messageModels: [MessageModel], in channelModel: ChannelModel) {
//        coreDataService.deleteAllMessages(for: channelModel.id)
//        coreDataService.save { context in
//            let fetchRequest = DBChannel.fetchRequest()
//            fetchRequest.predicate = NSPredicate(format: "id == %@", channelModel.id as CVarArg)
//            guard let dbChannel = try? context.fetch(fetchRequest).first else {
//                return
//            }
//            
//            for messageModel in messageModels {
//                let dbMessage = DBMessage(context: context)
//                dbMessage.uuid = messageModel.uuid
//                dbMessage.userName = messageModel.userName
//                dbMessage.date = messageModel.date
//                dbMessage.userID = messageModel.userID
//                dbMessage.text = messageModel.text
//                dbChannel.addToMessages(dbMessage)
//            }
//        }
//    }

//    func saveMessageModel(with messageModel: MessageModel, in channelModel: ChannelModel) {
//        coreDataService.save { context in
//            let fetchRequest = DBChannel.fetchRequest()
//            fetchRequest.predicate = NSPredicate(format: "id == %@", channelModel.id as CVarArg)
//            let dbChannel = try context.fetch(fetchRequest).first
//
//            guard
//                let dbChannel
//            else {
//                return
//            }
//
//            let dbMessage = DBMessage(context: context)
//            dbMessage.uuid = messageModel.uuid
//            dbMessage.userName = messageModel.userName
//            dbMessage.date = messageModel.date
//            dbMessage.userID = messageModel.userID
//            dbMessage.text = messageModel.text
//            dbChannel.addToMessages(dbMessage)
//        }
//    }
    
    func deleteChats(chatModels: [PSDChat]) {
        for model in chatModels {
            do {
                try coreDataService.deleteChat(id: model.chatId ?? 0)
            } catch {
                print("Error deleting channel with ID: \(model.chatId), \(error)")
            }
        }
    }

    func getAllChats() -> [PSDChat] {
        do {
            let dbChannel = try coreDataService.fetchChats()
            let chats: [PSDChat] = dbChannel.compactMap { dbChat in
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
                        guard dbMessage.text != nil || dbMessage.attachments?.count ?? 0 > 0
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
                    if let message = messages.last {
                        chat.lastComment = message
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

//    func getMessages(for channelUUID: String) -> [MessageModel] {
//        do {
//            let dbMessages = try coreDataService.fetchMessages(for: channelUUID)
//            let messages: [MessageModel] = dbMessages.compactMap { dbMessage in
//                guard
//                    let uuid = dbMessage.uuid,
//                    let text = dbMessage.text,
//                    let userName = dbMessage.userName,
//                    let userID = dbMessage.userID,
//                    let date = dbMessage.date
//                else {
//                    return nil
//                }
//
//                return MessageModel(
//                    uuid: uuid,
//                    text: text,
//                    userID: userID,
//                    userName: userName,
//                    date: date
//                )
//            }
//
//            return messages
//        } catch {
//            Logger.shared.printLog(log: "\(error)")
//            return []
//        }
//    }
    
    func deleteChannel(with chatId: Int) {
        do {
            try coreDataService.deleteChat(id: chatId)
        } catch {
            print("\(error)")
        }
    }
}
