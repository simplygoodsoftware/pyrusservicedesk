import Foundation
let PSD_MESSAGES_STORAGE_KEY : String = "PSDMessagesStorage"
let PSD_USER_DEFAULTS_SUITE_KEY : String = "com.PyrusServiceDesk"
let PSD_WAS_CLOSE_INFO_KEY : String = "com.PyrusServiceDesk.wasCloseInfo"

/**
 The storage for unsend messages.
 The storage is array with dictionary that is contain informatin need to draw message and repeat its sending.
 After cancel of sending or sending with success message is removing from storage.
 If message has attachment PSDMessagesStorage store them too. Data for this attachment is saving to local file that is removes as like message did.
 */
struct PSDMessagesStorage {
    ///The maximum number of messages to store. If has more - they are start deliting from start.
    private static let MAX_SAVED_MESSAGES_NUMBER  = 20
    ///The maximum size of attachment. Bigger attachment will not be saved to store.
    private static let MAX_SAVEDATTACHMENT_SIZE = 5000000//bytes
    ///The key to store local id of message
    private static let MESSAGE_LOCAL_ID_KEY = "localId"
    ///The key to store text of message
    private static let MESSAGE_TEXT_KEY = "messageText"
    ///The key to store ticket id of message
    private static let MESSAGE_TICKET_ID_KEY = "messageTicketId"
    ///The key to store author id of message
    private static let MESSAGE_AUTHOR_ID_KEY = "messageAuthorId"
    ///The key to store rating of message
    private static let MESSAGE_RATING_KEY = "messageRating"
    ///The key to store name of message's attachment - need for drawing, and to create file where is attachment's data is in
    private static let ATTACHMENT_ARRAY_KEY = "attachmentsArray"
    ///The key to store name of message's attachment - need for drawing, and to create file where is attachment's data is in
    private static let ATTACHMENT_NAME_KEY = "attachmentName"
    ///The key to store name of message's attachment - need for drawing, and to create file where is attachment's data is in
    private static let ATTACHMENT_GUID_KEY = "attachmentGuid"
    ///The key to store date of message
    private static let MESSAGE_DATE_KEY = "messageDate"
    ///The key to store size of message's attachment - need for drawing
    private static let ATTACHMENT_SIZE_KEY = "attachmentSize"
    ///The key to store state of message
    private static let MESSAGE_STATE_KEY = "isSending"
    ///The key to store commandId
    private static let COMMAND_ID_KEY = "commandId"
    ///The key to store commandId
    private static let USER_ID_KEY = "userId"
    ///The key to store commandId
    private static let APP_ID_KEY = "appId"
    ///The key to request_new_ticket
    private static let REQUEST_NEW_TICKET = "requestNewTicket"
    
    private static let fileURL: URL = {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        return paths[0].appendingPathComponent("messages.json")
    }()
    
    static func save(message: PSDMessage) {
        PyrusServiceDesk.storeMessages?.removeAll(where: { $0.clientId.lowercased() == message.clientId.lowercased() })
        PyrusServiceDesk.storeMessages?.append(message)
        if let attachments = message.attachments, attachments.count > 0 {
            for attachment in attachments {
                _ = saveToFileAttachment(attachment, messageLocalId: message.commandId ?? message.clientId)
            }
        }
      //  saveMessagesToFile()
    }
    
    static func remove(messageId: String, needSafe: Bool = true, serverTicketId: Int? = nil) {
        var newId: (Int, Int)? = nil
        PyrusServiceDesk.storeMessages?.removeAll(where: {
            if $0.clientId.lowercased() == messageId.lowercased() {
                if
                    $0.requestNewTicket,
                    $0.ticketId < 0,
                    let serverTicketId = serverTicketId
                {
                    newId = ($0.ticketId, serverTicketId)
                }
                return true
            }
            return false
        })
        if let newId = newId {
            PyrusServiceDesk.storeMessages?.forEach({ message in
                if message.ticketId == newId.0 {
                    message.ticketId = newId.1
                }
            })
        }
//        if needSafe {
//            saveMessagesToFile()
//        }
    }
    
    static func getMessages(for ticketId: Int? = nil) -> [PSDMessage] {
        if ticketId == nil {
            return PyrusServiceDesk.storeMessages ?? []
        } else if ticketId == 0 {
            return PyrusServiceDesk.storeMessages?.filter({ $0.ticketId == ticketId && $0.userId == PyrusServiceDesk.currentUserId }) ?? []
        } else {
            return PyrusServiceDesk.storeMessages?.filter({ $0.ticketId == ticketId }) ?? []
        }
    }
    
    static func getSendingMessages(for ticketId: Int? = nil) -> [MessageToPass] {
        let messages: [PSDMessage]
        if ticketId == nil {
            messages = PyrusServiceDesk.storeMessages?.filter({ $0.state == .sending }) ?? []
        } else if ticketId == 0 {
            messages = PyrusServiceDesk.storeMessages?.filter({ $0.state == .sending && $0.ticketId == ticketId && $0.userId == PyrusServiceDesk.currentUserId }) ?? []
        } else {
            messages = PyrusServiceDesk.storeMessages?.filter({ $0.state == .sending && $0.ticketId == ticketId }) ?? []
        }
        return messages.map({ MessageToPass(message: $0, commandId: $0.commandId ?? "") })
    }
    
    static func getNewCreateTicketMessages(_ userId: String? = nil) -> [PSDMessage] {
        if let userId = userId {
            return PyrusServiceDesk.storeMessages?.filter({ $0.requestNewTicket && $0.userId == userId }) ?? [PSDMessage]()
        } else {
            return PyrusServiceDesk.storeMessages?.filter({ $0.requestNewTicket }) ?? [PSDMessage]()
        }
    }
    
    static func loadAttachments() {
        for message in PyrusServiceDesk.storeMessages ?? [] {
            if let attachments = message.attachments, attachments.count > 0 {
                for (i,attachment) in attachments.enumerated(){
                    if attachment.emptyId() && !attachment.isLoading {
                        attachment.isLoading = true
                        PSDMessageSend.passFile(message, attachmentIdex: i, delegate: nil)
                        break
                    }
                }
            }
        }
    }
    
    static func createMessages(from commands: [TicketCommand]) {
        guard PyrusServiceDesk.storeMessages == nil else {
            return
        }
        
        var messages = [PSDMessage]()
        for command in commands {
            let message = PSDMessage(
                text: command.params.message ?? "",
                attachments: nil,
                messageId: nil,
                owner: PSDUsers.user,
                date: nil
            )
//            if let rating = dict[MESSAGE_RATING_KEY] as? Int {
//                message.rating = rating
//            }
            message.clientId = command.params.messageClientId ?? command.commandId
            message.state = .sending//(dict[MESSAGE_STATE_KEY] as? Bool ?? false) ? .sending : .cantSend//.cantSend
            message.date = command.params.date ?? Date()
            message.fromStrorage = false//message.state == .cantSend
            message.isOutgoing = true
            message.ticketId = command.params.ticketId ?? 0
            message.userId = command.userId
            message.appId = command.appId
            message.commandId = command.commandId
            message.requestNewTicket = command.params.requestNewTicket ?? false
            var attachments = [PSDAttachment]()
            if let attachmetsArray = command.params.attachments, attachmetsArray.count > 0 {
                for attachmentData in attachmetsArray {
                    guard attachmentData.name.count > 0,
                          let attachment = PSDFilesManager.getAtttachment(
                            attachmentData.name, messageLocalId: message.commandId ?? ""
                          ) else {
                        continue
                    }
                    attachment.name = attachmentData.name
                    attachment.serverIdentifer = attachmentData.guid
                    attachment.uploadingProgress = 0.0
                    attachments.append(attachment)
                }
            }
            message.attachments = attachments
            if message.text.count > 0 || message.attachments?.count ?? 0 > 0 || message.rating != nil {
                messages.append(message)
            }
        }
        
        PyrusServiceDesk.storeMessages = messages
        
        DispatchQueue.global().async {
            loadAttachments()
        }
    }
    
    static func loadMessages(completion: @escaping ([PSDMessage]) -> Void) {
        guard PyrusServiceDesk.storeMessages == nil else {
            completion(PyrusServiceDesk.storeMessages ?? [])
            return
        }
        
        DispatchQueue.global().async {
            guard FileManager.default.fileExists(atPath: fileURL.path),
                  let data = try? Data(contentsOf: fileURL),
                  let messagesDict = try? JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]] else {
                completion([])
                return
            }
            
            var messages = [PSDMessage]()
            for dict in messagesDict {
                if let message = PSDMessagesStorage.createMessage(from: dict) {
                    messages.append(message)
                }
            }
            completion(messages)
        }
    }
    
    static func saveMessagesToFile() {
        DispatchQueue.global().async {
            guard let storeMessages = PyrusServiceDesk.storeMessages else { return }
            var messages = [[String: Any]]()
            for message in storeMessages {
                if let dict = PSDMessagesStorage.createMessageDict(from: message) {
                    messages.append(dict)
                }
            }
            if let data = try? JSONSerialization.data(withJSONObject: messages, options: []) {
                try? data.write(to: PSDMessagesStorage.fileURL)
            }
        }
    }
    
    private static func createMessageDict(from message: PSDMessage) -> [String: Any]? {
        var hasSomeAttachment = false
        if let attachments = message.attachments, attachments.count > 0 {
            for attachment in attachments{
                if attachment.localPath?.count != 0 {
                    hasSomeAttachment = true
                    break
                }
            }
        }
        
        if message.text.count == 0 && !hasSomeAttachment && message.rating == nil {
            return nil
        }
        
        var messageDict: [String:Any] = [String:Any]()
        messageDict[MESSAGE_LOCAL_ID_KEY] = message.clientId
        messageDict[MESSAGE_TEXT_KEY] = message.text
        if #available(iOS 10.0, *) {
            messageDict[MESSAGE_DATE_KEY] = ISO8601DateFormatter().string(from: message.date)
        }
        messageDict[MESSAGE_RATING_KEY] = message.rating
        messageDict[MESSAGE_TICKET_ID_KEY] = message.ticketId
        messageDict[MESSAGE_AUTHOR_ID_KEY] = message.owner.authorId
        messageDict[MESSAGE_STATE_KEY] = message.state == .sending
        messageDict[COMMAND_ID_KEY] = message.commandId
        messageDict[USER_ID_KEY] = message.userId
        messageDict[APP_ID_KEY] = message.appId
        messageDict[REQUEST_NEW_TICKET] = message.requestNewTicket
        if hasSomeAttachment, let attachments = message.attachments, attachments.count > 0 {
            var attachmentsArray = [[String:Any]]()
            for attachment in attachments{
                if saveToFileAttachment(attachment, messageLocalId: message.clientId) {//if has attachment and it was written to disk - save message to storage
                    var attachmentDict = [String:Any]()
                    attachmentDict[ATTACHMENT_NAME_KEY] = attachment.name
                    attachmentDict[ATTACHMENT_SIZE_KEY] = attachment.size
                    attachmentDict[ATTACHMENT_GUID_KEY] = attachment.serverIdentifer
                    attachmentsArray.append(attachmentDict)
                }
            }
            if attachmentsArray.count > 0 {
                messageDict[ATTACHMENT_ARRAY_KEY] = attachmentsArray
            }
        }
        return messageDict
    }
    
    ///Save message's attachment to local file named same as attachment.
    ///Return status of saving - true if has no attachment or has attachment it was saved successful, of false - if attachment size is too big.
    static func saveToFileAttachment(_ attachment : PSDAttachment, messageLocalId: String)->Bool{
        if attachment.data.count > 0{
            if attachment.data.count > MAX_SAVEDATTACHMENT_SIZE{
                print("attachment is too big to safe")
                return false
            }
            return PSDFilesManager.saveAttchment(attachment, forMessageWith: messageLocalId)
        }
        return false
        
    }
    ///Removes message from storage if its exist by its local Id
    ///- parameter messageId: local id of message that is to be removed
    ///- parameter needSave: is need to save changes in UserDefaults
    private static func removeFromStorage(messageId:String, needSave: Bool)->[[String:Any]]{
        var messagesStorage = getMessagesStorage()
        for (index, dict) in messagesStorage.enumerated() {
            guard let localId = dict[COMMAND_ID_KEY] as? String else{
                continue
            }
            if (localId == messageId) {
                messagesStorage.remove(at: index)
                if let attachmentArray = dict[ATTACHMENT_ARRAY_KEY] as? [[String:Any]]{
                    for attachmentDict in attachmentArray{
                        guard let attachmentName = attachmentDict[ATTACHMENT_NAME_KEY] as? String else{
                            continue
                        }
                        PSDFilesManager.removeLocalFile(fileName: attachmentName, messageLocalId: messageId)
                    }
                }
                
                break
            }
        }
        if(needSave){
            saveToStorage(messagesStorage)
        }
        return messagesStorage
    }
    ///Removes all saved messages in storage
    static func cleanStorage() {
        PyrusServiceDesk.storeMessages = []
        saveMessagesToFile()
//        pyrusUserDefaults()?.removeObject(forKey: PSD_MESSAGES_STORAGE_KEY)
//        let allMessages = messagesFromStorage()
//        for message in allMessages{
//            removeFromStorage(messageId: message.clientId)
//        }
    }
    static private func saveToStorage(messageDict : [String:Any]){
        guard let messageLocalId = messageDict[MESSAGE_LOCAL_ID_KEY] as? String else{
            return
        }
        var messagesStorage = removeFromStorage(messageId: messageLocalId, needSave: false)
        if(messagesStorage.count > MAX_SAVED_MESSAGES_NUMBER){
            if let first = messagesStorage.first, let firstMessageLocalId = first[MESSAGE_LOCAL_ID_KEY] as? String{
                messagesStorage = removeFromStorage(messageId:  firstMessageLocalId, needSave: false)
            }
            
        }
        messagesStorage.append(messageDict)
        saveToStorage(messagesStorage)
    }
    
    ///Return array with saved messages info in storage.
    private static func saveToStorage(_ messagesStorage:[[String:Any]]){
        pyrusUserDefaults()?.set(messagesStorage, forKey: PSD_MESSAGES_STORAGE_KEY)
        pyrusUserDefaults()?.synchronize()
    }
    
    static func pyrusUserDefaults()->UserDefaults?{
        if let pyrusUserDefaults = UserDefaults(suiteName: PSD_USER_DEFAULTS_SUITE_KEY){
            return pyrusUserDefaults
        }
        else{
            return UserDefaults.init(suiteName: PSD_USER_DEFAULTS_SUITE_KEY)
        }
    }
    ///Return array with saved messages info in storage.
    private static func getMessagesStorage() -> [[String: Any]] {
        return pyrusUserDefaults()?.array(forKey: PSD_MESSAGES_STORAGE_KEY) as? [[String:Any]] ?? [[String:Any]]()
    }

    static private func createMessage(from dict: [String: Any]) -> PSDMessage? {
        if PyrusServiceDesk.multichats && dict[MESSAGE_AUTHOR_ID_KEY] as? String != PyrusServiceDesk.authorId {
            return nil
        }
        let message = PSDMessage(text: dict[MESSAGE_TEXT_KEY] as? String ?? "", attachments: nil, messageId: nil, owner: PSDUsers.user, date: nil)
        if let rating = dict[MESSAGE_RATING_KEY] as? Int {
            message.rating = rating
        }
        message.clientId = (dict[MESSAGE_LOCAL_ID_KEY] as? String) ?? message.clientId
        message.state = (dict[MESSAGE_STATE_KEY] as? Bool ?? false) ? .sending : .cantSend//.cantSend
        if #available(iOS 10.0, *) {
            if let dateString = dict[MESSAGE_DATE_KEY] as? String,
               let date = ISO8601DateFormatter().date(from: dateString) {
                message.date = date
            } else {
                message.date = Date()
            }
        }
        message.fromStrorage = message.state == .cantSend
        message.isOutgoing = true
        message.ticketId = dict[MESSAGE_TICKET_ID_KEY] as? Int ?? 0
        message.userId = dict[USER_ID_KEY] as? String ?? ""
        message.appId = dict[APP_ID_KEY] as? String ?? ""
        message.commandId = dict[COMMAND_ID_KEY] as? String
        message.requestNewTicket = dict[REQUEST_NEW_TICKET] as? Bool ?? false
        var attachments = [PSDAttachment]()
        if let attachmetsArray = dict[ATTACHMENT_ARRAY_KEY] as? [[String: Any]], attachmetsArray.count > 0 {
            for attachmentDict in attachmetsArray {
                guard let attName = attachmentDict[ATTACHMENT_NAME_KEY] as? String, attName.count > 0, let attachment = PSDFilesManager.getAtttachment(attName, messageLocalId: message.clientId) else {
                    continue
                }
                attachment.serverIdentifer = attachmentDict[ATTACHMENT_GUID_KEY] as? String
                attachments.append(attachment)
            }
        }
        message.attachments = attachments
        return message
    }
    
    ///Returns true, if there is some messages in storage to set rating
    static func hasRatingInStorage() -> Bool {
        let messages = getMessages()
        for message in messages{
            guard message.rating != nil else{
                continue
            }
            return true
        }
        return false
    }
}
