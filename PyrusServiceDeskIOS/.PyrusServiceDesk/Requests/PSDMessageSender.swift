import Foundation

let commentIdParameter = "comment_id"
let ticketIdParameter = "ticket_id"
let ratingParameter = "rating"
let subjectParameter = "subject"
let createdAtParameter = "created_at"
let attachmentsParameter = "attachments"
let guidParameter = "guid"
let CLIENT_ID_KEY = "client_id"


class PSDMessageSender: NSObject {
    /**
     Pass message to server.
     - parameter messageToPass: PSDMessage need to be passed.
     - parameter delegate: PSDMessageSendDelegate object to receive completion or error.
     - parameter completion: comptetion block. 
     */
    func pass(_ messageToPass:PSDMessage, delegate:PSDMessageSendDelegate?, completion: @escaping() -> Void) {
        let task = PSDMessageSender.pass(messageToPass.text, messageToPass.attachments, rating: messageToPass.rating, clientId: messageToPass.clientId) {
            commentId, attachments in
            if let commentId = commentId, commentId.count > 0 {
                //put attachments id
                if let attachments = attachments {
                    for (i,attachmentId) in attachments.enumerated() {
                        guard let attArr = messageToPass.attachments, attArr.count > 0 else {
                            continue
                        }
                        messageToPass.attachments?[i].serverIdentifer = "\(attachmentId)"
                    }
                }
                messageToPass.messageId = commentId
                PSDMessageSender.showResult(of: messageToPass, success: true, delegate: delegate)
            } else {
                PSDMessageSender.showResult(of: messageToPass, success: false, delegate: delegate)
            }
            completion()
        }
        PSDMessageSend.taskArray.append(task)
        
        //messages to exist chat no need to be in queue, so we can remove it
        PSDMessageSend.clearAndRemove(sender:self)
    }
    ///Show result
    ///
    static func showResult(of messageToPass:PSDMessage, success:Bool, delegate:PSDMessageSendDelegate?){
        if(success){
            PSDMessagesStorage.removeFromStorage(messageId: messageToPass.clientId)
            let _ = PyrusServiceDesk.setLastActivityDate()
            PyrusServiceDesk.restartTimer()
        }
        
        
        let newState: messageState = success ? .sent : .cantSend
        let newProgress: CGFloat = success ? 1.0 : 0.0
        
        messageToPass.state = newState
        for attachment in (messageToPass.attachments ?? [PSDAttachment]()){
            attachment.uploadingProgress = newProgress
            attachment.size = attachment.data.count
            attachment.data = Data()//clear saved data
        }
        if messageToPass.fromStrorage{
            messageToPass.date = Date()//mesages from the storage can have old date - change it to the current, to avoid diffrent drawing after second enter into chat
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
     - completion: Completion of passing message.
     - commentId: Return id of new message as String. If Request end with error return nil or "0" if received bad data from server.
     */
    private static func pass(_ message: String, _ attachments: [PSDAttachment]?, rating: Int?, clientId: String, completion: @escaping (_ commentId: String?, _ attachments: NSArray?) -> Void) -> URLSessionDataTask {
        //Generate additional parameters for request body
        var parameters = [String: Any]()
        parameters[commentParameter] = message
        parameters[CLIENT_ID_KEY] = clientId
        if let rating = rating{
            parameters[ratingParameter] = rating
        }
        parameters[userNameParameter] = PyrusServiceDesk.userName
        if let attachments = attachments, attachments.count > 0{
            parameters[attachmentsParameter] = generateAttacments(attachments)
        }
        let request = URLRequest.createRequest(type:.updateFeed, parameters: parameters)
        
        let task = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {                                                 // check for fundamental networking error
                completion(nil,nil)
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
     Create additional [key:value] with ticket for request body.
     */
    private static func generateTiket(_ allString: String,_ attachments: [PSDAttachment], clientId: String)->[String: Any]{
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
