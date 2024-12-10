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
            parameters["user_id"] = PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
            parameters["app_id"] = PyrusServiceDesk.clientId
            parameters["security_key"] = PyrusServiceDesk.securityKey
        }
        parameters["need_full_info"] = PyrusServiceDesk.multichats
        parameters["api_sign"] = PyrusServiceDesk.apiSign()
        parameters["author_id"] = PyrusServiceDesk.authorId
        parameters["author_name"] = PyrusServiceDesk.authorName
        parameters["last_note_id"] = PyrusServiceDesk.lastNoteId
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
//                if PyrusServiceDesk.chats.count == 0 {
//                    PyrusServiceDesk.chats = []
//                }
                completion(nil, nil, nil, nil, false)
            } else {
                do{
                    let chatsData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                    let chatsArray = chatsData["tickets"] as? NSArray ?? NSArray()
                    let chats = generateChats(from: chatsArray)
                    PyrusServiceDesk.chats = chats
                    let clientsArray = chatsData["applications"] as? NSArray ?? NSArray()
                    let clients = generateClients(from: clientsArray)
                    let authorAccessDenied = chatsData["author_access_denied"] as? [String]
                    
                    do {
                        let commandsArray = chatsData["commands_result"] as? NSArray ?? NSArray()
                        let jsonData = try JSONSerialization.data(withJSONObject: commandsArray, options: [])
                        let decoder = JSONDecoder()
                        let commands = try decoder.decode([TicketCommandResult].self, from: jsonData)
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
        var clients = PyrusServiceDesk.clients
        var serverClients = [PSDClientInfo]()
        for i in 0..<response.count {
            guard let dic: [String: Any] = response[i] as? [String: Any] else {
                continue
            }
            let clientId = dic["app_id"] as? String ?? ""
            let clientName = dic["org_name"] as? String ?? "iiko"
            let clientIcon = dic["org_logo_url"] as? String ?? ""
            let client = PSDClientInfo(clientId: clientId, clientName: clientName, clientIcon: clientIcon)
            if !clients.contains(client) {
                clients.append(client)
            }
            if !serverClients.contains(client) {
                serverClients.append(client)
            }
        }
        
        if serverClients.count != clients.count {
            for client in clients {
                if !serverClients.contains(client) {
                    clients.removeAll(where: { $0.clientId == client.clientId })
                }
            }
        }
        
        return clients
    }
    
    private static func generateChats(from response:NSArray) -> [PSDChat] {
        var chats: [PSDChat] = []
        for i in 0..<response.count {
            let dic: [String: Any] = response[i] as! [String: Any]
            var date: Date = Date()
            var lastMessage: PSDMessage?
            if let lastComment = dic["last_comment"] as? [String: Any] {
                date = lastComment.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
                lastMessage = PSDMessage.init(text: lastComment.stringOfKey("body"), attachments:nil, messageId: lastComment.stringOfKey(commentIdParameter), owner: nil, date: date)
            }
            let userId = dic["user_id"] as? String ?? ""
            if PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId == userId,
               PyrusServiceDesk.lastNoteId ?? 0 < Int(lastMessage?.messageId ?? "") ?? 0 {
                PyrusServiceDesk.lastNoteId = Int(lastMessage?.messageId ?? "")
            } else if let user = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == userId }),
                      user.lastNoteId ?? 0 < Int(lastMessage?.messageId ?? "") ?? 0 {
                user.lastNoteId = Int(lastMessage?.messageId ?? "")
            }
           
            let ticketId = dic["ticket_id"] as? Int
            var messages: [PSDMessage] = [PSDMessage]()
            let newMessages = PSDGetChat.generateMessages(from: dic["comments"] as? NSArray ?? NSArray())
            if let storeChat = PyrusServiceDesk.chats.first(where: { $0.chatId == ticketId }) {
                messages = storeChat.messages + newMessages
            } else {
                messages = newMessages
            }
            
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
            chat.isActive = dic["is_active"] as? Bool ?? true
            chats.append(chat)
        }
        return sortByLastMessage(chats)
    }
    
    private static func sortByLastMessage(_ chats: [PSDChat]) -> [PSDChat] {
        return chats.sorted(by: {
            if $0.isActive, !$1.isActive {
                return true
            }
            if !$0.isActive, $1.isActive {
                return false
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
            chats.append(chat)
        }
        return sortByLastMessage(chats)

    }
}
