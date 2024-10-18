import Foundation

struct PSDCreateChat {
    private static let ATTACHMENTS_KEY = "attachments"
    private static let SUBJECT_KEY = "subject"
    private static let DESCRIPTION_KEY = "description"
    private static let TICKET_KEY = "ticket"
    private static let USER_NAME_KEY = "user_name"
    private static let TICKET_ID_KEY = "ticket_id"
    
    private static var sessionTask : URLSessionDataTask? = nil
    /**
     Create new chat.
     On completion returns Int if it was received, or empty nil, if no connection.
     */
    static func create(subject: String, description: String, attachments: [PSDAttachment]? = [], completion: @escaping (_ chatId: Int?) -> Void)
    {
        //remove old session if it is
        remove()
        
        var ticket = [String: Any]()
        if let attachments = attachments, attachments.count > 0 {
            ticket[ATTACHMENTS_KEY] = PSDMessageSender.generateAttacments(attachments)
        }
        ticket[SUBJECT_KEY] = subject
        ticket[DESCRIPTION_KEY] = description
        var parameters = [String: Any]()
        parameters[TICKET_KEY] = ticket
        parameters[USER_NAME_KEY] = PyrusServiceDesk.authorName
        let request: URLRequest = URLRequest.createRequest(type: .createChat, parameters: parameters)
        
        PSDCreateChat.sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {                                             // check for fundamental networking error
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

                completion(nil)
            }
            do {
                let chatData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                let chatId = chatData[TICKET_ID_KEY] as? Int ?? 0
                completion(chatId)
            } catch {
                //print("PSDCreateChat error when convert to Integer")
            }
            
        }
        PSDCreateChat.sessionTask?.resume()
    }
    
    /**
     Cancel session task if its exist
     */
    static func remove() {
        if PSDCreateChat.sessionTask != nil {
            PSDCreateChat.sessionTask?.cancel()
            PSDCreateChat.sessionTask = nil
        }
    }
}
