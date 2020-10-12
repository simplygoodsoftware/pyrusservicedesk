import Foundation
/**
 Get chats from server.
 */
struct PSDGetChats {
    private static var sessionTask : URLSessionDataTask? = nil
    /**
     Get chats from server.
     - parameter delegate: PSDGetDelegate. Works only if showError is true.  If delegate not equal to nil - calls showNoConnectionView(), when no internet connection. Else remembers the current ViewController. And if it has not changed when response receive, on it displays an error.
     - parameter needShowError: Bool. Pass true if need to show error. If don't need it (for example in auto reloading) pass false.
     On completion returns [PSDChat] if it was received, or empty nil, if no connection.
     */
    static func get(delegate: PSDGetDelegate?, needShowError:Bool, completion: @escaping (_ chatsArray: [PSDChat]?) -> Void)
    {
        //remove old session if it is
        remove()
        var topViewController : UIViewController? = nil
        DispatchQueue.main.async {
            //if need show error - remember current top UIViewController
            if needShowError && delegate == nil{
                    topViewController = UIApplication.topViewController()
            }
        }
        
        
        let  request : URLRequest = URLRequest.createRequest(type:.chats, parameters: [String: Any]())
        
        PSDGetChats.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {                                                 // check for fundamental networking error
                completion(nil)
                if needShowError {
                    DispatchQueue.main.async {
                        if(PSDGetChats.sessionTask?.state != .canceling){
                            delegate?.showNoConnectionView()
                            showError(nil, on:topViewController)
                        }
                        
                    }
                }
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
                if needShowError {
                    DispatchQueue.main.async {showError(httpStatus.statusCode, on:topViewController)}
                }
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
        var unread = 0
        for i in 0..<response.count{
            let dic :[String:Any] = response[i] as! [String : Any]
            var messages : [PSDMessage] = [PSDMessage]()
            var date : Date = Date()
            
            let firstMessage :PSDMessage = PSDMessage.init(text: dic.stringOfKey(subjectParameter), attachments:nil, messageId: nil, owner: nil, date: nil)
            messages.append(firstMessage)
            if let lastComment = dic["last_comment"] as? [String : Any]{
                date =  lastComment.stringOfKey(createdAtParameter).dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'")
                let lastMessage :PSDMessage = PSDMessage.init(text: lastComment.stringOfKey("body"), attachments:nil, messageId: nil, owner: nil, date: nil)
                messages.append(lastMessage)
            }
            let chat = PSDChat.init(chatId: dic.stringOfKey(ticketIdParameter), date: date, messages: messages)
            let isRead = dic["is_read"] as? Bool ??  true
            if !isRead{
                unread=unread+1
            }
            chat.isRead = isRead
            chats.append(chat)
        }
        DispatchQueue.main.async {
            refreshChatsCount(chats.count)
            refreshNewMessagesCount(unread)
        }
        
        return sortByLastMessage(chats)
    }
    private static func sortByLastMessage(_ chats:[PSDChat])->[PSDChat]{
        
        return chats.sorted(by: { $0.date ?? Date() > $1.date ?? Date()})
    }
    static func refreshChatsCount(_ chats:Int){
        if PyrusServiceDesk.chatsCount != chats{
            PyrusServiceDesk.chatsCount = chats
            NotificationCenter.default.post(name: CHATS_NOTIFICATION_NAME, object:chats, userInfo: nil)
        }
        
    }
    static func refreshNewMessagesCount(_ unread:Int){
        if PyrusServiceDesk.newMessagesCount != unread{
            PyrusServiceDesk.newMessagesCount = unread
            NotificationCenter.default.post(name: MESSAGES_NUMBER_NOTIFICATION_NAME, object: unread, userInfo: nil)
            if(unread>0){
                PyrusServiceDesk.subscriber?.onNewReply()
            }
        }
    }
}
