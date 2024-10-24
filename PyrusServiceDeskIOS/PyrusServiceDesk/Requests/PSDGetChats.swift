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
    static func get(completion: @escaping (_ chatsArray: [PSDChat]?) -> Void)
    {
        //remove old session if it is
        remove()
        var parameters = [String: Any]()
        if PyrusServiceDesk.multichats {
            parameters["user_id"] = PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
            parameters["security_key"] = PyrusServiceDesk.securityKey
        }
        parameters["need_full_info"] = PyrusServiceDesk.multichats
        parameters["api_sign"] = PyrusServiceDesk.apiSign()
      //  parameters["author_id"] = PyrusServiceDesk.authorId
        if PyrusServiceDesk.additionalUsers.count > 0 {
            var additional_users = [[String: Any]]()
            for user in PyrusServiceDesk.additionalUsers {
                var  additional_user = [String: Any]()
                additional_user["app_id"] = user.clientId
                additional_user["user_id"] = user.userId
                additional_user["security_key"] = user.secretKey
                additional_users.append(additional_user)
            }
            parameters["additional_users"] = additional_users
        }
        
        let  request : URLRequest = URLRequest.createRequest(type:.chats, parameters: parameters)
        
        PSDGetChats.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {                                                 // check for fundamental networking error
                completion(nil)
                return
            }
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 {           // check for http errors
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
                if PyrusServiceDesk.chats.count == 0 {
                    PyrusServiceDesk.chats = []
                }
                completion([])
            }
            do{
                let chatsData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                let chatsArray = chatsData["tickets"] as? NSArray ?? NSArray()
                let chats = generateChats(from: chatsArray)
                PyrusServiceDesk.chats = chats
                completion(chats)
            }catch{
                //print("PSDGetChats error when convert to dictionary")
            }
            
        }
        PSDGetChats.sessionTask?.resume()
    }
    /**
     Cancel session task if its exist
     */
    static func remove(){
        if(PSDGetChats.sessionTask != nil){
            PSDGetChats.sessionTask?.cancel()
            PSDGetChats.sessionTask = nil
        }
    }
    
    private static func generateChats(from response:NSArray) -> [PSDChat] {
        var chats : [PSDChat] = []
        for i in 0..<response.count {
            let dic :[String:Any] = response[i] as! [String : Any]
            var date : Date = Date()
            var lastMessage: PSDMessage?
//            var messages : [PSDMessage] = [PSDMessage]()
//            let firstMessage :PSDMessage = PSDMessage.init(text: dic.stringOfKey(subjectParameter), attachments:nil, messageId: nil, owner: nil, date: nil)
//            messages.append(firstMessage)
            if let lastComment = dic["last_comment"] as? [String : Any] {
                date =  lastComment.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
                lastMessage = PSDMessage.init(text: lastComment.stringOfKey("body"), attachments:nil, messageId: lastComment.stringOfKey(commentIdParameter), owner: nil, date: nil)
//                messages.append(lastMessage)
            }
//            
            let ticketId = dic["ticket_id"] as? Int
            var messages : [PSDMessage] = [PSDMessage]()
            messages = PSDGetChat.generateMessages(from: dic["comments"] as? NSArray ?? NSArray())
            let chat = PSDChat.init(chatId: ticketId, date: date, messages: messages)
            chat.subject = dic["subject"] as? String
            chat.isRead = dic["is_read"] as? Bool ??  true
            chat.userId = dic["user_id"] as? String ?? ""
            chat.lastComment = lastMessage
            chats.append(chat)
        }
        
        return sortByLastMessage(chats)
    }
    
    private static func sortByLastMessage(_ chats: [PSDChat]) -> [PSDChat] {
        return chats.sorted(by: { $0.date ?? Date() > $1.date ?? Date() })
    }
}
