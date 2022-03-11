class PSDTasksData : NSObject {
    
    weak var sendingDelegate: PSDMessageSendDelegate?
    var message: PSDMessage
    var file: PSDAttachment
    init (_ message: PSDMessage, delegate: PSDMessageSendDelegate?, file: PSDAttachment) {
        self.sendingDelegate = delegate
        self.file = file
        self.message = message
        super.init()
        
        //show to delegate that upload was started
        PSDTasksData.updateMessage(message, with: file, uploadingProgress: 0.0)
        self.message.state = .sending
        self.sendingDelegate?.refresh(message: self.message,changedToSent:false)
    }
    fileprivate static func updateMessage(_ message: PSDMessage, with file: PSDAttachment, uploadingProgress: CGFloat){
        file.uploadingProgress = uploadingProgress
        if let attachments = message.attachments{
            for attachment in attachments{
                if attachment.localId == file.localId{
                    attachment.uploadingProgress = uploadingProgress
                    break
                }
            }
        }
    }
   
}
/**
 Has two step:
 1)Pass file to server and receive its id.
 2)After file passing, generates and pass message to current chat.
 */
class PSDUploader: NSObject {
    
    private static let boundary = "---------------------------a6dn39dgbgg672zxz0d73h2jnd78wh"
    
    /**
     Create new task to session.
     - parameter messageWithAttachment: PSDMessage object with file that need to be passed to server.
     - parameter delegate: PSDMessageSendDelegate object to receive completion or error. Used in second step.
     */
    func createUploadTask(from messageWithAttachment: PSDMessage, indexOfAttachment: Int,delegate: PSDMessageSendDelegate?) {
        guard let attachments = messageWithAttachment.attachments, attachments.count > indexOfAttachment else{
            return
        }
        if(attachments[indexOfAttachment].data.count == 0){
            return
        }
        let taskData = PSDTasksData.init(messageWithAttachment, delegate: delegate, file: attachments[indexOfAttachment])
        
        var request = URLRequest.createUploadRequest()
        
        
        request.setValue("gzip, deflate", forHTTPHeaderField:"Accept-Encoding")
        request.setValue(PSDUploader.userAgent(), forHTTPHeaderField: "User-Agent")
        request.setValue("multipart/form-data; boundary=\(PSDUploader.boundary)", forHTTPHeaderField: "Content-Type")
        
        var data: Data = Data()
        
        data.append(PSDUploader.createPrefixData(from: taskData.file))
        
        data.append(taskData.file.data)
        data.append(PSDUploader.createPostfixData())
        
        do {
            let streamSize = try PSDUploader.calculateContentLength(for: taskData.file)
            request.setValue("\(streamSize)", forHTTPHeaderField: "Content-Length")
        }
        catch {
        }
        
        if downloadsSession == nil {
            downloadsSession = createSession()
        }
        let task = downloadsSession!.uploadTask(with: request, from: data)
        
        tasksMap[task] = taskData
        
        task.resume()

    }
    var tasksMap : [URLSessionTask : PSDTasksData] = [URLSessionTask : PSDTasksData]()
    func stopAll(){
        self.downloadsSession?.getTasksWithCompletionHandler { (dataTasks: Array, uploadTasks: Array, downloadTasks: Array) in
            for task in downloadTasks {
                task.cancel()
            }
            self.downloadsSession?.invalidateAndCancel()
            self.downloadsSession = nil
        }
    }
    func stopUpload(task:URLSessionTask){
        task.cancel()
        if tasksMap[task] != nil{
            tasksMap[task]!.sendingDelegate?.remove(message: tasksMap[task]!.message)
            PSDMessagesStorage.removeFromStorage(messageId:tasksMap[task]!.message.clientId)
        }
        
        
    }
    ///Create user Agent String
    private static func userAgent()->String{
        let device : UIDevice = UIDevice.current
        let infoDict = Bundle.main.infoDictionary
        let appVersion :String = infoDict?["CFBundleVersion"] as! String
        
        return "\(CustomizationHelper.chatTitle)/\(appVersion)/\(PyrusServiceDesk.userName)/\(device.systemVersion)"
        
    }
    ///Header for Data
    private static func createPrefixData(from attachment: PSDAttachment) -> Data {
        var data = Data()
        data.append("\r\n--\(PSDUploader.boundary)\r\n".data(using: String.Encoding.utf8)!)
        data.append("Content-Disposition: form-data; name=\"userfile\"; filename=\"\(attachment.name)\"\r\n".data(using: String.Encoding.utf8)!)
        data.append("Content-Type: application/octet-stream\r\n\r\n".data(using: String.Encoding.utf8)!)
        return data
    }
    
    ///Footer for Data
    private static func createPostfixData() -> Data {
        return Data("\r\n--\(PSDUploader.boundary)--\r\n".data(using: String.Encoding.utf8)!)
    }
    ///Calculate body size
    static func calculateContentLength(for attachment:PSDAttachment) throws -> UInt64 {
        var size: UInt64 = 0
        size += UInt64(attachment.data.count)
        size += UInt64(self.createPrefixData(from: attachment).count)
        size += UInt64(self.createPostfixData().count)
        return size
    }
    private func downloadsSessionCreate() -> URLSession{
        let configuration = URLSessionConfiguration.default
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.urlCache = nil
        return URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue.main)
    }
    var downloadsSession: URLSession?
    private func  createSession() -> URLSession{
        let configuration = URLSessionConfiguration.default
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.urlCache = nil
        return URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue.main)
    }
    

    //MARK: second step
    ///End download. If end with some error pass "" as guid.
    ///If end with error pass it to delegate
    ///If end with success send attachment id to server
    private func endUpload(_ guid:String, task : URLSessionTask)
    {
        if let uploadData  = tasksMap[task]{
            if guid.count>0{
                uploadData.file.serverIdentifer = guid
                PSDTasksData.updateMessage(uploadData.message, with: uploadData.file, uploadingProgress: 0.95)
                uploadData.sendingDelegate?.refresh(message: uploadData.message,changedToSent: false)
                PSDMessageSend.pass(uploadData.message, delegate: uploadData.sendingDelegate)
            }
            else{
                //some error happend
                PSDTasksData.updateMessage(uploadData.message, with: uploadData.file, uploadingProgress: 0.0)
                uploadData.message.state = .cantSend
                uploadData.sendingDelegate?.refresh(message: uploadData.message,changedToSent: false)
                PSDMessageSend.fileSendingEndWithError(uploadData.message, delegate: uploadData.sendingDelegate)
            }
            tasksMap.removeValue(forKey: task)
        }
        
    }
}
extension PSDUploader : URLSessionTaskDelegate, URLSessionDelegate, URLSessionDataDelegate{
    //MARK: deleagte methods
    /*func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
     }*/
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        // the task finished
        if error != nil {
            self.endUpload("", task: task)
        } else {
            //print("The upload was successful.")
        }
        
    }
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        
        completionHandler(URLSession.ResponseDisposition.allow)
    }
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data){
        do{
            let receiveData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any] ?? [String: Any]()
            self.endUpload(receiveData.stringOfKey(guidParameter), task: dataTask)
        }catch{
            //print("Upload file error when convert to dictionary")
        }
        
    }
    func urlSession(_ session: URLSession, task: URLSessionTask, didSendBodyData bytesSent: Int64, totalBytesSent: Int64, totalBytesExpectedToSend: Int64) {
        
        if let uploadData  = tasksMap[task]{
            let progress = Float(totalBytesSent) / Float(totalBytesExpectedToSend)
            PSDTasksData.updateMessage(uploadData.message, with: uploadData.file, uploadingProgress: min(0.90,CGFloat(progress)))
            uploadData.sendingDelegate?.refresh(message: uploadData.message,changedToSent: false)
        }
        
        
        
    }
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, willCacheResponse proposedResponse: CachedURLResponse, completionHandler: @escaping (CachedURLResponse?) -> Void) {
        completionHandler(nil)
    }
}
