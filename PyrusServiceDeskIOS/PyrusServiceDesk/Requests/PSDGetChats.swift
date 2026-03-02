import Foundation
/**
 Get chats from server.
 */
struct PSDGetChats {
    private static let RATING_SETTINGS_KEY = "rating_settings"
    private static let WELCOME_MESSAGE = "welcome_message"
    private static var sessionTask : URLSessionDataTask? = nil
    /**
     Get chats from server.
     On completion returns [PSDChat] if it was received, or empty nil, if no connection.
     */
    static func get(commands: [[String: Any?]?] = [], completion: @escaping (GetTicketsResponse) -> Void) {
        //remove old session if it is
        remove()
        var parameters = [String: Any]()
        if PyrusServiceDesk.multichats {
            if PyrusServiceDesk.customUserId?.count ?? 0 > 0 {
                parameters["user_id"] = PyrusServiceDesk.customUserId
            }
            parameters["app_id"] = PyrusServiceDesk.clientId
            parameters["security_key"] = PyrusServiceDesk.securityKey
        }
        parameters["need_full_info"] = true
        parameters["api_sign"] = PyrusServiceDesk.apiSign()
        parameters["author_id"] = PyrusServiceDesk.authorId
        parameters["author_name"] = PyrusServiceDesk.authorName
        parameters["last_note_id"] = PyrusServiceDesk.lastNoteId
        
        if PyrusServiceDesk.needShowLoading {
            parameters["last_note_id"] = 0
        }
        
        var additional_users = [[String: Any]]()
        if PyrusServiceDesk.additionalUsers.count > 0 {
            for user in PyrusServiceDesk.additionalUsers {
                var additional_user = [String: Any]()
                additional_user["app_id"] = user.clientId
                additional_user["user_id"] = user.userId
                additional_user["security_key"] = user.secretKey
                additional_user["last_note_id"] = user.lastNoteId
                additional_users.append(additional_user)
            }
        }
        for clientId in PyrusServiceDesk.anonimClients {
            var additional_user = [String: Any]()
            additional_user["app_id"] = clientId
            additional_user["last_note_id"] = 0
            additional_users.append(additional_user)
        }
        parameters["additional_users"] = additional_users
        parameters["commands"] = commands
        
        var announcementsChepoints = [[String: Any]]()
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [
            .withInternetDateTime,
            .withFractionalSeconds
        ]
        for client in PyrusServiceDesk.clients {
            var announcementsChepoint = [String: Any]()
            announcementsChepoint["app_id"] = client.clientId
            announcementsChepoint["last_helpy_announcement_id"] = client.lasAnnoncementId
            announcementsChepoint["last_helpy_announcement_change_datetime"] = client.lasAnnouncementUpdateDate
            announcementsChepoints.append(announcementsChepoint)
        }
        
        if PyrusServiceDesk.clients.count == 0 {
            var announcementsChepoint = [String: Any]()
            announcementsChepoint["app_id"] = PyrusServiceDesk.clientId
            announcementsChepoints.append(announcementsChepoint)
        }
        
        parameters["helpy_announcement_feed_checkpoints"] = announcementsChepoints
        
        let request: URLRequest = URLRequest.createRequest(type:.chats, parameters: parameters)
        
        let startTime1 = CFAbsoluteTimeGetCurrent()
        print("GetTickets: \(Date()), commands count: \(commands.count)")
        
        PSDGetChats.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else { // check for fundamental networking error
                completion(GetTicketsResponse(complete: false))
                return
            }
            var startTime2 = CFAbsoluteTimeGetCurrent() - startTime1
            print("⏱ getChats req completed in \(startTime2) seconds")
            startTime2 = CFAbsoluteTimeGetCurrent()
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 { // check for http errors
                DispatchQueue.main.async {
                    if httpStatus.statusCode == 429 {
                        SyncManager.removeLastActivityDate()
                        completion(GetTicketsResponse(complete: true))
                    }
                    if httpStatus.statusCode == 403 {
                        if let onFailed = PyrusServiceDesk.onAuthorizationFailed {
                            onFailed()
                        } else {
                            if !PyrusServiceDesk.multichats {
                                PyrusServiceDesk.mainController?.closeServiceDesk()
                            }
                        }
                    }
                }
                let chatsData = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                print("Запрос не прошел, код ошибки (\(httpStatus.statusCode), ошибка: \(String(describing: error))")
                completion(GetTicketsResponse(complete: false))
            } else {
                do{
                    let jsonString = String(decoding: data, as: UTF8.self)
                    let fixed = jsonString.replacingOccurrences(
                        of: #"\"attachments\":\[\s*\"attachments\":"#,
                        with: "\"attachments\":[",
                        options: .regularExpression
                    )

                    let fixedData = Data(fixed.utf8)
                    let chatsData = try JSONSerialization.jsonObject(with: fixedData, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                    
                    let clientsArray = chatsData["applications"] as? NSArray ?? NSArray()
                    let clientsResult = generateClients(from: clientsArray)
                    let clients = clientsResult.clients
                    let announcementsResult = generateAnnouncements(from: clientsResult.serverAnnouncements)
                    var startTime3 = CFAbsoluteTimeGetCurrent() - startTime2
                    print("⏱ getChats serialization completed in \(startTime3) seconds")
                    startTime3 = CFAbsoluteTimeGetCurrent()
                    let chatsArray = chatsData["tickets"] as? [[String : Any]] ?? [[String: Any]]()
                    let chats = generateChats(from: chatsArray, clients: clients)
                    var startTime4 = CFAbsoluteTimeGetCurrent() - startTime3
                    print("⏱ generateChats completed in \(startTime4) seconds")
                    startTime4 = CFAbsoluteTimeGetCurrent()

                    let authorAccessDenied = chatsData["author_access_denied"] as? [String]
                    let commands = generateCommadsResults(from: chatsData)
                    
                    let response = GetTicketsResponse(
                        complete: true,
                        chats: chats,
                        clients: clients,
                        commandsResult: commands,
                        authorAccessDenied: authorAccessDenied,
                        announcementsResult: announcementsResult
                    )
                    completion(response)
                } catch {
                    print("PSDGetChats error when convert to dictionary")
                }
            }
            
        }
        PSDGetChats.sessionTask?.resume()
    }
    /**
     Cancel session task if its exist
     */
    static func remove() {
        if PSDGetChats.sessionTask != nil {
            PSDGetChats.sessionTask?.cancel()
            PSDGetChats.sessionTask = nil
        }
    }
    
    private static func generateCommadsResults(from chatsData: [String: Any]) -> [TicketCommandResult]? {
        do {
            let commandsArray = chatsData["commands_result"] as? NSArray ?? NSArray()
            let jsonData = try JSONSerialization.data(withJSONObject: commandsArray, options: [])
            let decoder = JSONDecoder()
            let commands = try decoder.decode([TicketCommandResult].self, from: jsonData)
            return commands
        } catch {
            return nil
        }
    }
    
    private static func generateAnnouncements(from announcementsResponse: [String: AnnouncementsResponse]) -> AnnouncementsResult {
        var announcements: [PSDAnnouncement] = []
        var deletedAnnouncementsIds = Set<String>()
        
        for (appId, announcementResponse) in announcementsResponse {
            var isRead = true
            var lastOrderIndex = PyrusServiceDesk.announcements.last?.orderIndex ?? -1
            for newAnnouncement in announcementResponse.newAnnouncements ?? [] {
                let announcement = PSDAnnouncement(
                    id: newAnnouncement.id,
                    text: getText(from: newAnnouncement.content),
                    date: newAnnouncement.createdAt,
                    isRead: isRead,
                    attachments: getAttachments(from: newAnnouncement.content),
                    appId: appId,
                    orderIndex: lastOrderIndex + 1
                )
                announcements.append(announcement)
                lastOrderIndex += 1
                if newAnnouncement.id == announcementResponse.inboxItem.lastReadMessageId {
                    isRead = false
                }
            }
            
            for changedAnnouncement in announcementResponse.announcementChanges ?? [] {
                if changedAnnouncement.type == .deleted {
                    deletedAnnouncementsIds.insert(changedAnnouncement.messageId)
                    announcements.removeAll(where: { $0.id == changedAnnouncement.messageId })
                } else if changedAnnouncement.type == .edited {
                    if !announcements.contains(where: { $0.id == changedAnnouncement.messageId }) {
                        let localAnn = PyrusServiceDesk.announcements.first(where: { $0.id == changedAnnouncement.messageId })
                        let announcement = PSDAnnouncement(
                            id: changedAnnouncement.messageId,
                            text: getText(from: changedAnnouncement.content),
                            date: localAnn?.date ?? changedAnnouncement.performedAt,
                            isRead: localAnn?.isRead ?? true,
                            attachments: getAttachments(from: changedAnnouncement.content),
                            appId: appId,
                            orderIndex: localAnn?.orderIndex ?? 0
                        )
                        announcements.append(announcement)
                    }
                }
            }
        }
        
        return AnnouncementsResult(newAnnouncements: announcements, deletedAnnouncementsIds: deletedAnnouncementsIds)
        
        func getText(from content: Content?) -> String {
            guard let content else { return "" }
            var text: String = ""
            for block in content.richTextDocument.richTextBlocks ?? [] {
                text += block.richTextInlines.map(\.string).joined() + "\n"
            }
            text = text.trimmingCharacters(in: .whitespacesAndNewlines)
            return text
        }
        
        func getAttachments(from content: Content?) -> [PSDAnnouncementAttachment] {
            guard let content else { return [] }
            let attachments: [PSDAnnouncementAttachment] = (content.attachments ?? []).map { attach in
                PSDAnnouncementAttachment(
                    id: attach.id,
                    name: attach.name,
                    size: attach.size ?? 0,
                    width: attach.width ?? 0,
                    height: attach.height ?? 0,
                    media: attach.media ?? false
                )
            }
            return attachments
        }
    }
    
    private static func generateClients(from response: NSArray) -> ClientsResult {
        let updateAccessCommands = PyrusServiceDesk.repository.getCommands().filter({ $0.type == TicketCommandType.updateAccess.rawValue }).sorted(by: { $0.params.date ?? Date() > $1.params.date ?? Date() })
        var clients = PyrusServiceDesk.clients
        var serverClients = [PSDClientInfo]()
        var serverAnnouncements = [String: AnnouncementsResponse]()
        
        for i in 0..<response.count {
            guard let dic: [String: Any] = response[i] as? [String: Any] else {
                continue
            }
            let clientId = dic["app_id"] as? String ?? ""
            let clientName = dic["org_name"] as? String ?? "iiko"
            let clientIcon = dic["org_logo_url"] as? String ?? ""
            let clientDescription = dic["org_description"] as? String
            
            let client = PSDClientInfo(clientId: clientId, clientName: clientName, clientIcon: clientIcon)
            client.clientDescription = clientDescription
            client.welcomeMessage = dic[PSDGetChats.WELCOME_MESSAGE] as? String
            if let ratingSettings = dic[PSDGetChats.RATING_SETTINGS_KEY] as? NSDictionary {
                do {
                    let jsonData = try JSONSerialization.data(withJSONObject: ratingSettings, options: [])
                    let decoder = JSONDecoder()
                    let settings = try decoder.decode(PSDRatingSettings.self, from: jsonData)
                    client.ratingSettings = settings
                } catch {
                    print("Error decoding rating settings JSON: \(error)")
                }
            }
            
            let announcementsDict = dic["helpy_announcement_feed_delta"] as? [String: Any] ?? [:]
            let announcementsInfo = decodeAnnouncement(from: announcementsDict)
            serverAnnouncements[clientId] = announcementsInfo
            client.lasAnnouncementUpdateDate = announcementsInfo?.inboxItem.lastMessageDatetimeUTC
            client.lasAnnoncementReadId = announcementsInfo?.inboxItem.lastReadMessageId
            client.announcementsUnreadCount = announcementsInfo?.inboxItem.unreadCount ?? 0
            if let id = announcementsInfo?.newAnnouncements?.last?.id {
                client.lasAnnoncementId = id
            }

            if !clients.contains(client) {
                clients.append(client)
            } else if let storeClient = clients.first(where: { $0.clientId == client.clientId }) {
                storeClient.clientName = client.clientName
                storeClient.clientDescription = client.clientDescription
                storeClient.ratingSettings = client.ratingSettings
                storeClient.welcomeMessage = client.welcomeMessage
                if storeClient.clientIcon != client.clientIcon {
                    storeClient.clientIcon = client.clientIcon
                }
                storeClient.lasAnnouncementUpdateDate = client.lasAnnouncementUpdateDate
                storeClient.lasAnnoncementReadId = client.lasAnnoncementReadId
                storeClient.announcementsUnreadCount = client.announcementsUnreadCount
                if let id = client.lasAnnoncementId {
                    storeClient.lasAnnoncementId = id
                }
            }
            if !serverClients.contains(client) {
                serverClients.append(client)
            }
            
            let extraUsers = dic["extra_users"] as? NSArray ?? []
            for extraUser in extraUsers {
                guard let extraUserDic: [String: Any] = extraUser as? [String: Any] else {
                    continue
                }
                let userId = extraUserDic["user_id"] as? String ?? ""
                let userName = extraUserDic["title"] as? String ?? ""
                let user = PSDUserInfo(appId: clientId, clientName: clientName, userId: userId, userName: userName, secretKey: nil)
                if PyrusServiceDesk.clientId == clientId && PyrusServiceDesk.customUserId?.count ?? 0 == 0  {
                    PyrusServiceDesk.customUserId = userId
                    PyrusServiceDesk.userName = userName
                    PyrusServiceDesk.lastNoteId = 0
                    PyrusServiceDesk.anonimClients.insert(clientId)
                } else {
                    PyrusServiceDesk.additionalUsers.append(user)
                    if PyrusServiceDesk.additionalUsers.first(where: { $0.clientId == clientId && $0.userId?.count ?? 0 == 0 }) != nil {
                        PyrusServiceDesk.additionalUsers.removeAll(where: { $0.clientId == clientId && $0.userId?.count ?? 0 == 0 })
                        PyrusServiceDesk.anonimClients.insert(clientId)
                    }
                }
                DispatchQueue.main.async {
                    PyrusServiceDesk.syncManager.syncGetTickets()
                    PyrusServiceDesk.extraUsersCallback?.addUser(user: user)
                    NotificationCenter.default.post(name: .createMenuNotification, object: nil)
                }
            }
            
            let authorsInfo = dic["author_info"] as? [String: Any] ?? [:]
            for userId in authorsInfo.keys {
                let authors = authorsInfo[userId] as? NSArray ?? []
                var userAuthors: [PSDUserInfo.AuthorInfo] = []
                for author in authors {
                    guard let authorDic: [String: Any] = author as? [String: Any] else {
                        continue
                    }
                    let name = authorDic["name"] as? String ?? ""
                    let id = authorDic["author_id"] as? String ?? ""
                    let phone = authorDic["phone"] as? String ?? ""
                    var hasAccess = authorDic["has_access"] as? Bool ?? true
                    if let commandHasAccess = updateAccessCommands.first(where: { $0.userId == userId && $0.params.authorId == id })?.params.hasAccess {
                        hasAccess = commandHasAccess
                    }
                    let authorInfo = PSDUserInfo.AuthorInfo(id: id, name: name, phone: phone, hasAccess: hasAccess)
                    userAuthors.append(authorInfo)
                }
                if PyrusServiceDesk.customUserId == userId {
                    PyrusServiceDesk.authors = userAuthors
                } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == userId }) {
                    user.authors = userAuthors
                }
            }
        }
        
        for client in clients {
            if !serverClients.contains(client) {
                clients.removeAll(where: { $0.clientId == client.clientId })
            }
        }
        
        return ClientsResult(clients: clients, serverAnnouncements: serverAnnouncements)
    }
    
    private static func decodeAnnouncement(from dict: [String: Any]) -> AnnouncementsResponse? {
        do {
            let data = try JSONSerialization.data(
                withJSONObject: dict,
                options: []
            )
            
            let decoder = JSONDecoder()
            
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            
            decoder.dateDecodingStrategy = .custom { decoder in
                let container = try decoder.singleValueContainer()
                let string = try container.decode(String.self)
                return formatter.date(from: string)!
            }
            
            let announcementsResponse = try decoder.decode(AnnouncementsResponse.self, from: data)
            return announcementsResponse
        } catch {
            print(error)

            if let decodingError = error as? DecodingError {
                print(decodingError)
            }
            
            return nil
        }
    }
    
    private static func generateChats(from response: [[String: Any]], clients: [PSDClientInfo]) -> [PSDChat] {
        var chats: [PSDChat] = []
        chats.reserveCapacity(response.count)

        let usersById: [String: PSDUserInfo] = Dictionary(
            uniqueKeysWithValues: PyrusServiceDesk.additionalUsers.map { ($0.userId ?? "", $0) }
        )

        let currentUserId = PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId

        for dic in response {

            let userId = dic["user_id"] as? String ?? ""

            // базовая дата
            var chatDate = (dic[createdAtParameter] as? String)?.fastParseISODate() ?? Date()
            var lastMessage: PSDMessage?

            // last_comment
            if let lastComment = dic["last_comment"] as? [String: Any] {
                chatDate = (lastComment[createdAtParameter] as? String)?.fastParseISODate() ?? Date()

                lastMessage = PSDMessage(
                    text: lastComment["body"] as? String ?? "",
                    attachments: nil,
                    messageId: lastComment[commentIdParameter] as? String ?? "",
                    owner: nil,
                    date: chatDate
                )
            }

            // messages
            let messages = PSDGetChat.generateMessages(
                from: dic["comments"] as? NSArray ?? []
            )

            // прокидываем данные из последнего сообщения
            if let last = messages.last {
                lastMessage?.attachments = last.attachments
                lastMessage?.owner = last.owner
                lastMessage?.isOutgoing = last.isOutgoing
            }

            // обновление lastNoteId
            if let messageId = Int(lastMessage?.messageId ?? "0") {
                if userId == currentUserId {
                    if (PyrusServiceDesk.lastNoteId ?? 0) < messageId {
                        PyrusServiceDesk.lastNoteId = messageId
                    }
                } else if let user = usersById[userId],
                          (user.lastNoteId ?? 0) < messageId {
                    user.lastNoteId = messageId
                }
            }

            // chat
            let chat = PSDChat(
                chatId: dic["ticket_id"] as? Int,
                date: chatDate,
                messages: messages
            )

            chat.subject = dic["subject"] as? String
            chat.isRead = dic["is_read"] as? Bool ?? true
            chat.userId = userId
            chat.lastComment = lastMessage
            chat.lastReadedCommentId = dic["last_read_comment_id"] as? Int
            chat.showRating = dic["show_rating"] as? Bool ?? false
            if isMoreThan24Hours(from: lastMessage?.date ?? Date()) {
                chat.showRating = false
            }
            chat.showRatingText = dic["show_rating_text"] as? String

            if !chat.showRating || !PyrusServiceDesk.multichats {
                chat.isActive = dic["is_active"] as? Bool ?? true
            }

            chats.append(chat)
        }

        return sortByLastMessage(chats)
    }
    
    private static func isMoreThan24Hours(from date: Date) -> Bool {
        let timeInterval = Date().timeIntervalSince(date)
        return timeInterval > 24 * 60 * 60 // 24 часа
    }
    
    static func sortByLastMessage(_ chats: [PSDChat]) -> [PSDChat] {
        chats.sorted { lhs, rhs in

            if lhs.isActive != rhs.isActive {
                return lhs.isActive
            }

            let lhsDate = lhs.date ?? .distantPast
            let rhsDate = rhs.date ?? .distantPast

            if lhsDate != rhsDate {
                return lhsDate > rhsDate
            }

            let lhsId = Int(lhs.lastComment?.messageId ?? "0") ?? 0
            let rhsId = Int(rhs.lastComment?.messageId ?? "0") ?? 0

            return lhsId > rhsId
        }
    }

    
    static func getSortedChatForMessages(_ messages: [PSDMessage]) -> [PSDChat] {
        var chats = [PSDChat]()
        messages.forEach {
            let chat = PSDChat(chatId: $0.ticketId, date: $0.date, messages: [])
            chat.subject = $0.text
            chat.lastComment = $0
            chat.lastComment?.isOutgoing = true
            chat.userId = $0.userId
            chats.append(chat)
        }
        return chats

    }
}

extension Notification.Name {
    static let createMenuNotification = Notification.Name("CreateMenuNotification")
}

let commentIdParameter = "comment_id"
let ticketIdParameter = "ticket_id"
let ratingParameter = "rating"
let subjectParameter = "subject"
let createdAtParameter = "created_at"
let attachmentsParameter = "attachments"
let guidParameter = "guid"
let CLIENT_ID_KEY = "client_id"
let EXTRA_FIELDS_KEY = "extra_fields"

