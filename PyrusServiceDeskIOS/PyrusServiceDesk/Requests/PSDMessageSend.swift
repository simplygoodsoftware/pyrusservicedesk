import UIKit

protocol PSDMessageSendDelegate: class {
    func change(_ chatId:String)
    ///Pass delegate to refresh object with new data. Attention! may call from background!
    ///- parameter changedToSent: If state of message was changed from !.sent to .sent
    func refresh(message:PSDMessage, changedToSent: Bool)
    func remove(message:PSDMessage)
}
struct PSDMessageSend {
    ///DispatchSemaphore to pass message one by one
    private static let semaphore = DispatchSemaphore(value: 1)
    /**
     Create new PSDUploader.
     Has two step:
     1)Pass file to server and receive its id.
     2)After file passing, generates and pass message to current chat.
     - parameter file: PSDAttachment object with data that need to be passed to server.
     - parameter chatId: Id of chat to which will need to attach this attachment after its id will be received.
     - parameter delegate: PSDMessageSendDelegate object to receive completion or error. Used in second step.
     */
    static private func passFile(_ messageWithAttachment:PSDMessage, attachmentIdex:Int, to chatId:String,delegate:PSDMessageSendDelegate?)
    {
        if(session == nil){
            session = PSDUploader.init()
        }
        if(messageWithAttachment.attachments != nil){
            session!.createUploadTask(from: messageWithAttachment,indexOfAttachment:attachmentIdex, chatId: chatId, delegate: delegate)
        }
        
    }
    /**
     Stop uploding attachment (if it is)
     */
    static func stopUpload(_ attachment:PSDAttachment){
        if session != nil{
            for (task, data) in session!.tasksMap{
                if(data.file == attachment){
                    session!.stopUpload(task: task)
                    break
                }
            }
        }
        
    }
    static func fileSendingEndWithError(_ messageToPass:PSDMessage, to chatId:String?, delegate:PSDMessageSendDelegate?){
        didEndPassMessage(messageToPass, to: chatId, delegate: delegate)
    }
    /**
     Stops all current session tasks and uploads.
     */
    static func stopAll(){
        for task in taskArray{
            task.cancel()
        }
        taskArray.removeAll()
        session?.stopAll()
        session = nil
        needWhait =  false
        passQueueArray.removeAll()
        passingMessagesIds.removeAll()
    }
    //Array with all active tasks of pass messages.
    static var taskArray = [URLSessionDataTask]()
   
    
    static var session : PSDUploader?
    
    ///Array with all PSDMessageSender
    static private var messageSenders = [PSDMessageSender]()
    
    ///Store an array for messages ids that needs to be send.
    ///PSDMessageSender and PSDUploader must clean themselves after completion, even if they end with unsuccess
    static private var passingMessagesIds = [String]()
    ///Indicator to wait for first message sent.
    static private var needWhait = false
    ///Array with messages in queue waiting for first message end sent.
    ///If first message sending end with error - error will be passed to all messages in array.
    static private var passQueueArray = [PSDMessage]()
    
    static private let dispatchQueue = DispatchQueue(label: "PSDSendMesasges")
    /**
     Pass message to server.
     - parameter messageToPass: PSDMessage need to be passed.
     - parameter chatId: The id of chat as String where message is sending, if this is new chat, pass "" or DEFAULT_CHAT_ID
     - parameter delegate: PSDMessageSendDelegate object to receive completion or error.
     */
    static func pass(_ messageToPass:PSDMessage, to chatId:String, delegate:PSDMessageSendDelegate?)
    {
        PSDMessagesStorage.saveInStorage(message:messageToPass)
        dispatchQueue.async {
        if (PSDChatTableView.isNewChat(chatId) && !PSDMessageSend.passingMessagesIds.contains(messageToPass.clientId)){
            if needWhait{
                passQueueArray.append(messageToPass)
                return
            }
            else{
                needWhait = true
            }
            
        }
        if !(PSDChatTableView.isNewChat(chatId)){
            if PSDMessageSend.passingMessagesIds.contains(messageToPass.clientId){
                //при отпрвке атачей эта же функция вызывается снова, продолжаем отправку не блокируя очередь
            }
            else{
                PSDMessageSend.passingMessagesIds.append(messageToPass.clientId)
                _ = PSDMessageSend.semaphore.wait(timeout: DispatchTime.distantFuture)
            }
        }
        
        var hasUnsendAttachments = false
        if let attachments = messageToPass.attachments, attachments.count > 0{
            for (i,attachment) in attachments.enumerated(){
                if attachment.emptyId(){
                    PSDMessageSend.passFile(messageToPass, attachmentIdex: i, to: chatId, delegate: delegate)
                    hasUnsendAttachments = true
                    break
                }
                
            }
            
        }
        if !hasUnsendAttachments{
            let sender = PSDMessageSender()
                sender.pass(messageToPass, to: chatId, delegate: delegate, completion: {
                    newChatId in
                    didEndPassMessage(messageToPass, to: newChatId, delegate: delegate)
                })
            messageSenders.append(sender)
        }
        }
        
    }
    static private func didEndPassMessage(_ messageToPass:PSDMessage, to newChatId:String?, delegate:PSDMessageSendDelegate?){
        if needWhait{
            needWhait = false
            if let newChatId = newChatId  {
                for message in passQueueArray{
                    if newChatId.count > 0{
                        pass(message, to: newChatId, delegate: delegate)
                    }
                    else{
                        PSDMessageSender.showResult(of: message, success: false, delegate: delegate)
                    }
                }
                passQueueArray.removeAll()
            }
        }
        PSDMessageSend.passingMessagesIds.removeAll(where: {$0 == messageToPass.clientId})
        semaphore.signal()
    }
    static func clearAndRemove(sender:PSDMessageSender){
        if let index = messageSenders.firstIndex(of: sender) {
            messageSenders.remove(at: index)
        }
    }
    
}
