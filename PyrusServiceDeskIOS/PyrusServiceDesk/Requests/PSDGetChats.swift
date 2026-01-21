import Foundation
/**
 Get chats from server.
 */
struct PSDGetChats {
    private static var sessionTask : URLSessionDataTask? = nil
    /**
     Get chats from server.
     On completion returns [PSDChat] if it was received, or empty nil, if no connection.
     */
    static func get(commands: [[String: Any?]?] = [], completion: @escaping (_ chatsArray: [PSDChat]?, _ commandsResults: [TicketCommandResult]?, _ authorAccessDenied: [String]?, _ clients: [PSDClientInfo]?, _ complete: Bool) -> Void) {
        //remove old session if it is
        remove()
        var parameters = [String: Any]()
        if PyrusServiceDesk.multichats {
            parameters["user_id"] = PyrusServiceDesk.customUserId
            parameters["app_id"] = PyrusServiceDesk.clientId
            parameters["security_key"] = PyrusServiceDesk.securityKey
        }
        parameters["need_full_info"] = PyrusServiceDesk.multichats
        parameters["api_sign"] = PyrusServiceDesk.apiSign()
        parameters["author_id"] = PyrusServiceDesk.authorId
        parameters["author_name"] = PyrusServiceDesk.authorName
        parameters["last_note_id"] = PyrusServiceDesk.lastNoteId
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
        
        let request: URLRequest = URLRequest.createRequest(type:.chats, parameters: parameters)
        
        let startTime1 = CFAbsoluteTimeGetCurrent()
        print("GETTICKETS TIME: \(Date.now)")
        PSDGetChats.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else { // check for fundamental networking error
                completion(nil, nil, nil, nil, false)
                return
            }
            var startTime2 = CFAbsoluteTimeGetCurrent() - startTime1
            print("⏱ getChats req completed in \(startTime2) seconds")
            startTime2 = CFAbsoluteTimeGetCurrent()
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 { // check for http errors
                DispatchQueue.main.async {
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
//                if PyrusServiceDesk.chats.count == 0 {
//                    PyrusServiceDesk.chats = []
//                }
                completion(nil, nil, nil, nil, false)
            } else {
                do{
                    let chatsData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                    var startTime3 = CFAbsoluteTimeGetCurrent() - startTime2
                    print("⏱ getChats serialization completed in \(startTime3) seconds")
                    startTime3 = CFAbsoluteTimeGetCurrent()
                    let chatsArray = chatsData["tickets"] as? [[String : Any]] ?? [[String: Any]]()
                    let chats = generateChats(from: chatsArray)
                    var startTime4 = CFAbsoluteTimeGetCurrent() - startTime3
                    print("⏱ generateChats completed in \(startTime4) seconds")
                    startTime4 = CFAbsoluteTimeGetCurrent()
                   // PyrusServiceDesk.chats = chats
                    let clientsArray = chatsData["applications"] as? NSArray ?? NSArray()
                    let clients = generateClients(from: clientsArray)
                    var startTime5 = CFAbsoluteTimeGetCurrent() - startTime4
                    print("⏱ generateClients completed in \(startTime5) seconds")
                    startTime5 = CFAbsoluteTimeGetCurrent()

                    let authorAccessDenied = chatsData["author_access_denied"] as? [String]
                    
                    do {
                        let commandsArray = chatsData["commands_result"] as? NSArray ?? NSArray()
                        let jsonData = try JSONSerialization.data(withJSONObject: commandsArray, options: [])
                        let decoder = JSONDecoder()
                        let commands = try decoder.decode([TicketCommandResult].self, from: jsonData)
                        let startTime6 = CFAbsoluteTimeGetCurrent() - startTime5
                        print("⏱ generateComands completed in \(startTime6) seconds, commandsCount: \(commands.count)")
                        completion(chats, commands, authorAccessDenied, clients, true)
                    } catch {
                        completion(chats, nil, authorAccessDenied, clients, true)
                    }
                    //                PyrusServiceDesk.chats = chats
                } catch { 
                    //print("PSDGetChats error when convert to dictionary")
                }
            }
            
        }
        PSDGetChats.sessionTask?.resume()
    }
    /**
     Cancel session task if its exist
     */
    static func remove(){
        if PSDGetChats.sessionTask != nil {
            PSDGetChats.sessionTask?.cancel()
            PSDGetChats.sessionTask = nil
        }
    }
    
    private static func generateClients(from response: NSArray) -> [PSDClientInfo] {
        let updateAccessCommands = PyrusServiceDesk.repository.getCommands().filter({ $0.type == TicketCommandType.updateAccess.rawValue }).sorted(by: { $0.params.date ?? Date() > $1.params.date ?? Date() })
        var clients = PyrusServiceDesk.clients
        var serverClients = [PSDClientInfo]()
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

            if !clients.contains(client) {
                clients.append(client)
            } else if let storeClient = clients.first(where: { $0.clientId == client.clientId }) {
                storeClient.clientName = client.clientName
                storeClient.clientDescription = client.clientDescription
                if storeClient.clientIcon != client.clientIcon {
                    storeClient.clientIcon = client.clientIcon
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
        
        return clients
    }
    
    private static func generateChats(from response: [[String: Any]]) -> [PSDChat] {

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
            chat.showRatingText = dic["show_rating_text"] as? String

            if !chat.showRating {
                chat.isActive = dic["is_active"] as? Bool ?? true
            }

            chats.append(chat)
        }

        return sortByLastMessage(chats)
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
