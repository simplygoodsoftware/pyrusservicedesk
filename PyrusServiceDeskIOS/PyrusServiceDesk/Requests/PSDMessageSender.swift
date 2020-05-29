import Foundation

let commentIdParameter = "comment_id"
let ticketIdParameter = "ticket_id"
let subjectParameter = "subject"
let createdAtParameter = "created_at"
let attachmentsParameter = "attachments"
let guidParameter = "guid"


class PSDMessageSender: NSObject {
    /**
     Pass message to server.
     - parameter messageToPass: PSDMessage need to be passed.
     - parameter chatId: The id of chat as String where message is sending, if this is new chat, pass "" or DEFAULT_CHAT_ID
     - parameter delegate: PSDMessageSendDelegate object to receive completion or error.
     - parameter completion: comptetion block needs to be performed ufter passing ends. Pass chatId if chat was new, if new pass chatId on success, or 0 if end with error
     */
    func pass(_ messageToPass:PSDMessage, to chatId:String, delegate:PSDMessageSendDelegate?, completion: @escaping(_ chatId : String?) -> Void)
    {
        if(PSDChatTableView.isNewChat(chatId)){
            let task = PSDMessageSender.passFirst(messageToPass.text, messageToPass.attachments){
                newChatId in
                //If passFirst end with success send new ChatId to delegate
                if newChatId.count>0{
                    delegate?.change(newChatId)
                    
                    PSDMessageSender.showResult(of: messageToPass, success: true, delegate: delegate)
                    
                }
                else{
                    PSDMessageSender.showResult(of: messageToPass, success: false, delegate: delegate)
                }
                completion(newChatId)
                PSDMessageSend.clearAndRemove(sender:self)
            }
            PSDMessageSend.taskArray.append(task)
        }
        else {
            let task = PSDMessageSender.pass(messageToPass.text,messageToPass.attachments, to: chatId){
                (commentId: String?, attachments: NSArray?) in
                if commentId != nil &&  commentId?.count ?? 0>0{
                    //put attachments id
                    if let attachments = attachments{
                        for (i,attachmentId) in attachments.enumerated(){
                            if let attArr = messageToPass.attachments, attArr.count > 0{
                                messageToPass.attachments?[i].serverIdentifer = "\(attachmentId)"
                            }
                        }
                    }
                    messageToPass.messageId = commentId!
                    PSDMessageSender.showResult(of: messageToPass, success: true, delegate: delegate)
                }
                else{
                    PSDMessageSender.showResult(of: messageToPass, success: false, delegate: delegate)
                }
                completion(nil)
            }
            PSDMessageSend.taskArray.append(task)
            
            //messages to exist chat no need to be in queue, so we can remove it
            PSDMessageSend.clearAndRemove(sender:self)
        }
    }
    ///Show result
    ///
    static func showResult(of messageToPass:PSDMessage, success:Bool, delegate:PSDMessageSendDelegate?){
        if(success){
            PSDMessagesStorage.removeFromStorage(messageId: messageToPass.localId)
        }
        
        
        let newState : messageState = success ? .sent : .cantSend
        let newProgress :CGFloat = success ? 1.0 : 0.0
        
        messageToPass.state = newState
        for attachment in (messageToPass.attachments ?? [PSDAttachment]()){
            attachment.uploadingProgress = newProgress
            attachment.size = attachment.data.count
            attachment.data = Data()//clear saved data
        }
        delegate?.refresh(message:messageToPass, changedToSent: success)
    }
    private static let commentParameter = "comment"
    private static let userNameParameter = "user_name"
    private static let ticketParameter = "ticket"
    private static let descriptionParameter = "description"

    /**
     Pass message to exist chat.
     - Parameters:
     - message: Message (text) that need to send.
     - attachments: PSDAttachment object that need to send.
     - chatId: The id of chat where message is sent to.
     - completion: Completion of passing message.
     - commentId: Return id of new message as String. If Request end with error return nil or "0" if received bad data from server.
     */
    private static func pass(_ message:String, _ attachments:[PSDAttachment]?, to chatId:String, completion: @escaping (_ commentId: String?, _ attachments: NSArray?) -> Void)->URLSessionDataTask
    {
        //Generate additional parameters for request body
        var parameters = [String: Any]()
        parameters[commentParameter] = message
        parameters[userNameParameter] = PyrusServiceDesk.userName
        if let attachments = attachments, attachments.count > 0{
            parameters[attachmentsParameter] = generateAttacments(attachments)
        }
        
        var  request : URLRequest
        if(PyrusServiceDesk.oneChat){
            request = URLRequest.createRequest(type:.updateFeed, parameters: parameters)
        }
        else{
            request = URLRequest.createRequest(with:chatId,type:.update, parameters: parameters)
        }
        //print("pass request body \(String(describing: String(data: request.httpBody!, encoding: String.Encoding.utf8)))")
        let task = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            
            guard let data = data, error == nil else {                                                 // check for fundamental networking error
                completion(nil,nil)
                return
            }
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 {           // check for http errors
                completion(nil,nil)
            }
            
            if(data.count>0){
                
                do{
                    let messageData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any] ?? [String: Any]()
                    completion(messageData.stringOfKey(commentIdParameter), messageData[attachmentsParameter] as? NSArray)
                }catch{
                    //print("Pass error when convert to dictionary")
                }
                
                
            }
            else{
                completion(nil,nil)
            }
        }
        task.resume()
        return task
    }
    /**
     Pass message to sever. As complition create new chat.
     - Parameters:
     - message: Message (text) that need to send.
     - attachments: PSDAttachment object that need to send.
     - completion: Completion of passing message.
     - ticketId: Return id of new chat as String. If Request end with error return "".
     */
    private static func passFirst(_ message:String, _ attachments:[PSDAttachment]?, completion: @escaping (_ ticketId: String) -> Void)->URLSessionDataTask
    {
        var parameters = [String: Any]()
        parameters[userNameParameter] = PyrusServiceDesk.userName
        parameters[ticketParameter] = generateTiket(message,attachments ?? [PSDAttachment]())
        
        let  request : URLRequest = URLRequest.createRequest(type:.createNew, parameters: parameters)
        // print("request parameters = \(parameters)")
        // print("passFirst request body \(String(describing: String(data: request.httpBody!, encoding: String.Encoding.utf8)))")
        
        let task = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {                                                 // check for fundamental networking error
                completion("")
                return
            }
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 {           // check for http errors
                completion("")
            }
            
            do{
                let movieData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any] ?? [String: Any]()
                completion(movieData.stringOfKey(ticketIdParameter))
            }catch{
                //print("passFirst error when convert to dictionary")
            }
            
        }
        task.resume()
        return task
    }
    /**
     Create additional [key:value] with ticket for request body.
     */
    private static func generateTiket(_ allString: String,_ attachments: [PSDAttachment])->[String: Any]{
        var subjectString : String = ""
        var descriptionString : String = ""
        
        let firstParagraph :String = allString.components(separatedBy: CharacterSet.newlines).first ?? ""
        let stringToSplit :String = allString.components(separatedBy: ".").first ?? ""
        subjectString = firstParagraph.count>stringToSplit.count ? stringToSplit:firstParagraph
        if subjectString.count == 0{
            subjectString = allString
        }
        descriptionString = allString
        
        if(subjectString.count==0){
            if attachments.count  > 0{
                subjectString = attachments[0].name
            }else{
                subjectString = "Empty_Description".localizedPSD()
            }
        }
        
        if attachments.count > 0 {
            return [subjectParameter:subjectString,descriptionParameter:descriptionString, attachmentsParameter : generateAttacments(attachments)]
        }
        return [subjectParameter:subjectString,descriptionParameter:descriptionString]
    }
    /**
     Create additional [key:value] of attachment for request body.
     */
    private static func generateAttacments(_ attachments: [PSDAttachment])-> NSArray{
        let arrayWithAttach = NSMutableArray()
        for attachment in attachments{
            var guidArray : [String : Any] = [String : Any]()
            guidArray[guidParameter] = attachment.serverIdentifer
            guidArray["type"] = 0
            guidArray["name"] = attachment.name
            arrayWithAttach.add(guidArray)
        }
        return arrayWithAttach
    }
}
