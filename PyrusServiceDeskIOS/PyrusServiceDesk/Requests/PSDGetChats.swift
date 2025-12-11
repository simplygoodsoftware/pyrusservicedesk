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
    static func get(commands: [[String: Any?]?] = [], completion: @escaping (_ chatsArray: [PSDChat]?, _ commandsResults: [TicketCommandResult]?, _ authorAccessDenied: [String]?, _ clients: [PSDClientInfo]?, _ complete: Bool) -> Void) {
        //remove old session if it is
        remove()
        var parameters = [String: Any]()
        if PyrusServiceDesk.multichats {
            parameters["user_id"] = PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
            parameters["app_id"] = PyrusServiceDesk.clientId
            parameters["security_key"] = PyrusServiceDesk.securityKey
        }
        parameters["need_full_info"] = true//PyrusServiceDesk.multichats
        parameters["api_sign"] = PyrusServiceDesk.apiSign()
        parameters["author_id"] = PyrusServiceDesk.authorId
        parameters["author_name"] = PyrusServiceDesk.authorName
        parameters["last_note_id"] = PyrusServiceDesk.lastNoteId
        
        if let fieldsData = PyrusServiceDesk.fieldsData {
            parameters[EXTRA_FIELDS_KEY] = fieldsData
        }
        
        if PyrusServiceDesk.needShowLoading {
            parameters["last_note_id"] = 0
        }
        
        if PyrusServiceDesk.additionalUsers.count > 0 {
            var additional_users = [[String: Any]]()
            for user in PyrusServiceDesk.additionalUsers {
                var  additional_user = [String: Any]()
                additional_user["app_id"] = user.clientId
                additional_user["user_id"] = user.userId
                additional_user["security_key"] = user.secretKey
                additional_user["last_note_id"] = user.lastNoteId
                additional_users.append(additional_user)
            }
            parameters["additional_users"] = additional_users
        }
        parameters["commands"] = commands
        
        let request: URLRequest = URLRequest.createRequest(type:.chats, parameters: parameters)
//        print("app_id: \(PyrusServiceDesk.clientId), user_id: \(PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId), secret_key: \(PyrusServiceDesk.securityKey), lastNoteId: \(parameters["last_note_id"])")
        print("GetTickets: \(Date())")
        PSDGetChats.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else { // check for fundamental networking error
                completion(nil, nil, nil, nil, false)
                return
            }
            
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
                print("Запрос не прошел, код ошибки (\(httpStatus.statusCode), ошибка: \(String(describing: error))")
                completion(nil, nil, nil, nil, false)
            } else {
                do{
                    let chatsData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                    let clientsArray = chatsData["applications"] as? NSArray ?? NSArray()
                    let clients = generateClients(from: clientsArray)
                    let chatsArray = chatsData["tickets"] as? NSArray ?? NSArray()
                    let chats = generateChats(from: chatsArray, clients: clients)
                    let authorAccessDenied = chatsData["author_access_denied"] as? [String]
//                    print("количество чатов: \(chats.count)")
                    do {
                        let commandsArray = chatsData["commands_result"] as? NSArray ?? NSArray()
                        let jsonData = try JSONSerialization.data(withJSONObject: commandsArray, options: [])
                        let decoder = JSONDecoder()
                        let commands = try decoder.decode([TicketCommandResult].self, from: jsonData)
                        completion(chats, commands, authorAccessDenied, clients, true)
                    } catch {
                        completion(chats, nil, authorAccessDenied, clients, true)
                    }
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
    
    private static func generateClients(from response: NSArray) -> [PSDClientInfo] {
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
                PyrusServiceDesk.additionalUsers.append(user)
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
                    if authorDic["has_access"] as? Bool ?? false {
                        let name = authorDic["name"] as? String ?? ""
                        let id = authorDic["author_id"] as? String ?? ""
                        let phone = authorDic["phone"] as? String ?? ""
                        let authorInfo = PSDUserInfo.AuthorInfo(id: id, name: name, phone: phone)
                        userAuthors.append(authorInfo)
                    }
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
    
    static func updateClientIcon(client: PSDClientInfo) {
        
    }
    
    private static func generateChats(from response: NSArray, clients: [PSDClientInfo]) -> [PSDChat] {
        var chats: [PSDChat] = []
        for i in 0..<response.count {
            let dic: [String: Any] = response[i] as! [String: Any]
            var date: Date = dic.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
            var lastMessage: PSDMessage?
            if let lastComment = dic["last_comment"] as? [String: Any] {
                date = lastComment.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
                lastMessage = PSDMessage.init(text: lastComment.stringOfKey("body"), attachments:nil, messageId: lastComment.stringOfKey(commentIdParameter), owner: nil, date: date)
            }
            
            let userId = dic["user_id"] as? String ?? ""
            let ticketId = dic["ticket_id"] as? Int
            var messages: [PSDMessage] = [PSDMessage]()
            let newMessages = PSDGetChat.generateMessages(from: dic["comments"] as? NSArray ?? NSArray())
            
            messages = newMessages
            
            if let message = messages.last {
                lastMessage?.attachments = message.attachments
                lastMessage?.owner = message.owner
                lastMessage?.isOutgoing = message.isOutgoing
            }
            
            let chat = PSDChat.init(chatId: ticketId, date: date, messages: messages)
            chat.subject = dic["subject"] as? String
            chat.isRead = dic["is_read"] as? Bool ??  true
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
        return chats.sorted(by: {
            if $0.isActive, !$1.isActive {
                return true
            }
            if !$0.isActive, $1.isActive {
                return false
            }
            if $0.date ?? Date() == $1.date ?? Date() {
                return Int($0.lastComment?.messageId ?? "0") ?? 0 > Int($1.lastComment?.messageId ?? "0") ?? 0
            }
            return $0.date ?? Date() > $1.date ?? Date()
        })
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
