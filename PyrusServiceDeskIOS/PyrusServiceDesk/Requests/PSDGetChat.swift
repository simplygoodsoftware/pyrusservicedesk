import Foundation
protocol  PSDGetDelegate : class{
    func showNoConnectionView()
}
/**
Get chat from server.
 */
struct PSDGetChat {
    private static let SHOW_RATING_KEY = "show_rating"
    private static let KEEP_UNREAD_RATING_KEY = "keep_unread"
    private static var chatGetters : [Int: ChatGetter] = [Int: ChatGetter]()
  //  private static var sessionTask : URLSessionDataTask? = nil
    /**
     Get chat from server.
     - parameter needShowError: Bool. Pass true if need to show error. If don't need it (for example in auto reloading) pass false.
     - parameter delegate: PSDGetDelegate. Works only if showError is true. If not equal to nil - calls showNoConnectionView(), when no internet connection. Else remembers the current ViewController. And if it has not changed when response receive, on it displays an error.
     On completion returns PSDChat object if it was received.
     */
    static func get(needShowError: Bool, delegate: PSDGetDelegate?, keepUnread: Bool = false, completion: @escaping (_ chat: PSDChat?) -> Void) {
        //remove old session if it is
        remove()
        var topViewController : UIViewController? = nil
        DispatchQueue.main.async {
            //if need show error - remember current top UIViewController
            if needShowError && delegate == nil{
                topViewController = UIApplication.topViewController()
            }
        }
        let parameters = [KEEP_UNREAD_RATING_KEY: keepUnread]
        let request = URLRequest.createRequest(type: .chatFeed, parameters: parameters)
    
        let localId = UUID().uuidString
        let task = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                // check for fundamental networking error
                if needShowError {
                    DispatchQueue.main.async {
                        for (_, chatGetter) in chatGetters{
                            if(chatGetter.localId == localId){
                                if(chatGetter.task?.state != .canceling){
                                    chatGetter.delegate?.showNoConnectionView()
                                    showError(nil, on:topViewController)
                                }
                            }
                        }
                    }
                }
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
                
                if needShowError {
                    DispatchQueue.main.async {showError(httpStatus.statusCode, on:topViewController)}
                }
                
            }
            do{
                let chatData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                completion(generateChat(from:chatData))
            }catch{
                //print("PSDGetChat error when convert to dictionary")
            }
            
        }
        let chatGetter = ChatGetter.init(task: task, delegate: delegate, localId: localId)
        chatGetters[task.taskIdentifier] = chatGetter
       // PSDGetChat.sessionTask?.taskIdentifier
        task.resume()
    }
    private static func removeChatGetter(with taskIdentifier:Int){
        chatGetters.removeValue(forKey: taskIdentifier)
    }
    /**
     Cancel session task if its exist
     */
    static func remove(){
        for (taskIdentifier, chatGetter) in chatGetters{
            chatGetter.task?.cancel()
            removeChatGetter(with:taskIdentifier)
        }
    }
    static func isActive()->Bool{
        for (_, chatGetter) in chatGetters{
            if chatGetter.task?.state == .running{
                return true
            }
        }
        return false
    }
    private static func generateChat(from response:[String: Any]) -> PSDChat
    {
        var massages : [PSDMessage] = [PSDMessage]()
        massages = PSDGetChat.generateMessages(from: response["comments"] as? NSArray ?? NSArray())
        let chat = PSDChat(date: Date(), messages: massages)
        chat.showRating = (response[PSDGetChat.SHOW_RATING_KEY] as? Bool) ?? false
        return chat
    }
    private static func generateMessages(from array:NSArray) -> [PSDMessage]
    {
        var messages : [PSDMessage] = []
        if(array.count == 0){
            return messages
        }
        for i in 0...array.count-1{
            guard let dic :[String:Any] = array[i] as? [String : Any] else{
                continue
            }
            let date : Date =  (dic[createdAtParameter] as? String)?.dateFromString(format: "yyyy-MM-dd'T'HH:mm:ss'Z'") ?? Date()
            let IsInbound : Bool = dic["is_inbound"] as? Bool ??  true
            let user :PSDUser
            if IsInbound{
                user = PSDUsers.user
            }else{
                user = createUser(from: dic)
                
            }
            var textForMessage: String? = nil
            var attachmentsForMessage: [PSDAttachment]? = nil
            var rating: Int? = nil
            
            //Check is message has only attachments and no text
            if dic.stringOfKey("body").count>0{
                textForMessage = dic.stringOfKey("body")
                
            }
            if dic.intOfKey(ratingParameter) != 0 {
                rating = dic.intOfKey(ratingParameter)
            }
            //Check is message has some attachments, if has -  create one new messages for each attachment
            if let ats = dic[attachmentsParameter] as? [[String : Any]], ats.count > 0{
                for attachDict in ats{
                    if attachmentsForMessage == nil{
                        attachmentsForMessage = [PSDAttachment]()
                    }
                    let attachment : PSDAttachment = generateAttachment(from: attachDict)
                    attachmentsForMessage?.append(attachment)
                }
            }
            if (attachmentsForMessage?.count ?? 0) > 0 || (textForMessage?.count ?? 0) > 0 || rating != nil{
                let message = PSDMessage(text: textForMessage, attachments:attachmentsForMessage, messageId: dic.stringOfKey(commentIdParameter), owner: user, date: date)
                message.rating = rating
                let clientId = dic.stringOfKey(CLIENT_ID_KEY)
                if clientId.count > 0 {
                    message.clientId = clientId
                }
                messages.append(message)
            }
        }
        return messages
    }
    private static func generateAttachment(from response:[String: Any]) -> PSDAttachment{
        let attachment : PSDAttachment =  PSDAttachment.init(localPath: "", data: nil,serverIdentifer:response.stringOfKey("id"))
        attachment.name = response.stringOfKey("name")
        attachment.size = response.intOfKey("size")
        //\"attachments\":[{\"id\":69201887,\"name\":\"local_log.txt\",\"size\":32103,\"is_text\":true,\"is_video\":false}
        return attachment
    }
    private static func createUser(from dic:[String: Any])->PSDUser{
        let response = dic["author"] as? [String : Any]
        
        if(response != nil){
            let user : PSDUser = PSDUsers.supportUsersContain(name: response!.stringOfKey("name"), imagePath: response!.stringOfKey("avatar_id"))
            return user
        }
        else{
            return PSDUser(personId: "0", name: "Support_Default_Name".localizedPSD(), type: .support, imagePath: "")
        }
        
        
    }
    
}
private class ChatGetter : NSObject{
    var task : URLSessionDataTask?
    var localId : String
    weak var delegate : PSDGetDelegate?
    init(task: URLSessionDataTask?, delegate: PSDGetDelegate?, localId: String)  {
        self.task = task
        self.delegate = delegate
        self.localId = localId
    }
}
