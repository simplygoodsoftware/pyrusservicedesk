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
        
        let  request : URLRequest = URLRequest.createRequest(type:.chats, parameters: [String: Any]())
        
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
                            PyrusServiceDesk.mainController?.closeServiceDesk()
                        }
                    }
                }

                completion([])
            }
            do{
                let chatsData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                let chatsArray = chatsData["tickets"] as? NSArray ?? NSArray()
                let chats = generateChats(from:chatsArray)
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
    private static func generateChats(from response:NSArray)->[PSDChat]
    {
        var chats : [PSDChat] = []
        for i in 0..<response.count{
            let dic :[String:Any] = response[i] as! [String : Any]
            var messages : [PSDMessage] = [PSDMessage]()
            var date : Date = Date()
            
            let firstMessage :PSDMessage = PSDMessage.init(text: dic.stringOfKey(subjectParameter), attachments:nil, messageId: nil, owner: nil, date: nil)
            messages.append(firstMessage)
            if let lastComment = dic["last_comment"] as? [String : Any]{
                date =  lastComment.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
                let lastMessage :PSDMessage = PSDMessage.init(text: lastComment.stringOfKey("body"), attachments:nil, messageId: lastComment.stringOfKey(commentIdParameter), owner: nil, date: nil)
                messages.append(lastMessage)
            }
            let chat = PSDChat.init(date: date, messages: messages)
            chat.isRead = dic["is_read"] as? Bool ??  true
            chats.append(chat)
        }
        return sortByLastMessage(chats)
    }
    private static func sortByLastMessage(_ chats:[PSDChat])->[PSDChat]{
        
        return chats.sorted(by: { $0.date ?? Date() > $1.date ?? Date()})
    }
}
