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
struct PSDMessagesStorage{
    ///The maximum number of messages to store. If has more - they are start deliting from start.
    private static let MAX_SAVED_MESSAGES_NUMBER  = 20
    ///The maximum size of attachment. Bigger attachment will not be saved to store.
    private static let MAX_SAVEDATTACHMENT_SIZE = 5000000//bytes
    ///The key to store local id of message
    private static let MESSAGE_LOCAL_ID_KEY = "localId"
    ///The key to store text of message
    private static let MESSAGE_TEXT_KEY = "messageText"
    ///The key to store rating of message
    private static let MESSAGE_RATING_KEY = "messageRating"
    ///The key to store name of message's attachment - need for drawing, and to create file where is attachment's data is in
    private static let ATTACHMENT_ARRAY_KEY = "attachmentsArray"
    ///The key to store name of message's attachment - need for drawing, and to create file where is attachment's data is in
    private static let ATTACHMENT_NAME_KEY = "attachmentName"
    ///The key to store date of message
    private static let MESSAGE_DATE_KEY = "messageDate"
    ///The key to store size of message's attachment - need for drawing
    private static let ATTACHMENT_SIZE_KEY = "attachmentSize"
    ///Seve message in storage - if message has text - save its text, if has attachment - save attachment to file
    static func saveInStorage(message : PSDMessage){
        var hasSomeAttachment = false
        if let attachments = message.attachments, attachments.count > 0{
            for attachment in attachments{
                if attachment.localPath?.count != 0{
                    hasSomeAttachment = true
                    break
                }
            }
        }
        
        if message.text.count == 0 && !hasSomeAttachment && message.rating == nil{
            return
        }
        
        var messageDict : [String:Any] = [String:Any]()
        messageDict[MESSAGE_LOCAL_ID_KEY] = message.clientId
        messageDict[MESSAGE_TEXT_KEY] = message.text
        messageDict[MESSAGE_DATE_KEY] = message.date
        messageDict[MESSAGE_RATING_KEY] = message.rating
        DispatchQueue.global().async {
            
            if hasSomeAttachment, let attachments = message.attachments, attachments.count > 0{
                var attachmentsArray = [[String:Any]]()
                for attachment in attachments{
                    if saveToFileAttachment(attachment, messageLocalId: message.clientId){//if has attachment and it was written to disk - save message to storage
                        var attachmentDict = [String:Any]()
                        attachmentDict[ATTACHMENT_NAME_KEY] = attachment.name
                        attachmentDict[ATTACHMENT_SIZE_KEY] = attachment.size
                        attachmentsArray.append(attachmentDict)
                    }
                }
                if attachmentsArray.count > 0{
                    messageDict[ATTACHMENT_ARRAY_KEY] = attachmentsArray
                }
            }
            DispatchQueue.main.async {
                saveToStorage(messageDict: messageDict)
            }
        }
        
    }
    ///Save message's attachment to local file named same as attachment.
    ///Return status of saving - true if has no attachment or has attachment it was saved successful, of false - if attachment size is too big.
    private static func saveToFileAttachment(_ attachment : PSDAttachment, messageLocalId: String)->Bool{
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
            guard let localId = dict[MESSAGE_LOCAL_ID_KEY] as? String else{
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
        let allMessages = messagesFromStorage()
        for message in allMessages{
            removeFromStorage(messageId: message.clientId)
        }
    }
    static private func saveToStorage(messageDict : [String:Any]){
        guard let messageLocalId = messageDict[MESSAGE_LOCAL_ID_KEY] as? String else{
            return
        }
        var messagesStorage = removeFromStorage(messageId:  messageLocalId, needSave: false)
        if(messagesStorage.count > MAX_SAVED_MESSAGES_NUMBER){
            if let first = messagesStorage.first, let firstMessageLocalId = first[MESSAGE_LOCAL_ID_KEY] as? String{
                messagesStorage = removeFromStorage(messageId:  firstMessageLocalId, needSave: false)
            }
            
        }
        messagesStorage.append(messageDict)
        saveToStorage(messagesStorage)
    }
    ///Removes message from storage if its exist by its local Id, save changes in UserDefaults
    static func removeFromStorage(messageId:String){
        DispatchQueue.main.async {
            let _ = removeFromStorage(messageId: messageId, needSave: true)
        }
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
    private static func resaveInStorageMessage(_ message: PSDMessage) {
        removeFromStorage(messageId: message.messageId)
        saveInStorage(message: message)
    }
    ///Returns [PSDMessage] in storage
    static func messagesFromStorage() -> [PSDMessage] {
        var arrayWithMessages = [PSDMessage]()
        let messagesStorage = getMessagesStorage()
        for dict in messagesStorage {
            let message = PSDMessage(text: dict[MESSAGE_TEXT_KEY] as? String ?? "", attachments: nil, messageId: nil, owner: PSDUsers.user, date: nil)
            if let rating = dict[MESSAGE_RATING_KEY] as? Int {
                message.rating = rating
            }
            message.clientId = (dict[MESSAGE_LOCAL_ID_KEY] as? String) ?? message.clientId
            message.state = .cantSend
            message.date = dict[MESSAGE_DATE_KEY] as? Date ?? Date()
            message.fromStrorage = true
            var attachments = [PSDAttachment]()
            if let attachmetsArray = dict[ATTACHMENT_ARRAY_KEY] as? [[String: Any]], attachmetsArray.count > 0 {
                for attachmentDict in attachmetsArray {
                    guard let attName = attachmentDict[ATTACHMENT_NAME_KEY] as? String, attName.count > 0, let attachment = PSDFilesManager.getAtttachment(attName, messageLocalId: message.clientId) else {
                        continue
                    }
                    attachments.append(attachment)
                }
            }
            message.attachments = attachments
            if message.attachments?.count ?? 0 > 0 || message.text.count > 0 || message.rating != nil {
                if dict[MESSAGE_DATE_KEY] as? Date == nil {
                    resaveInStorageMessage(message)//resave massage with date to avoid nil next time
                }
                arrayWithMessages.append(message)
            }else{
                ///this is break data - remove it from storage
                removeFromStorage(messageId: message.messageId)
            }
           
        }
        return arrayWithMessages
    }
    ///Returns true, if there is some messages in storage to set rating
    static func hasRatingInStorage() -> Bool {
        let messages = messagesFromStorage()
        for message in messages{
            guard message.rating != nil else{
                continue
            }
            return true
        }
        return false
    }
}
