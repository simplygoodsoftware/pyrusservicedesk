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
import Foundation
import UIKit

final class PSDUploader: NSObject {
    // Очередь запуска задач с явным QoS во избежание priority inversion
    static private let dispatchQueue = DispatchQueue(
        label: "PSDUploader.launch",
        qos: .userInitiated,
        attributes: .concurrent
    )
    // Семафор для троттлинга — по одному аплоаду за раз
    private static let semaphore = DispatchSemaphore(value: 1)
    private static let boundary = "---------------------------a6dn39dgbgg672zxz0d73h2jnd78wh"

    // Очередь для синхронизации состояния (tasksMap, downloadsSession) с явным QoS
    private let stateQueue = DispatchQueue(label: "PSDUploader.state", qos: .userInitiated)

    // Карта задач + флаг, чтобы не сигналить дважды
    struct TaskEntry {
        var data: PSDTasksData
        var didFinish: Bool = false // защищает от двойного endUpload/signal
    }

    var tasksMap: [URLSessionTask: TaskEntry] = [:]

    private var downloadsSession: URLSession?

    private func createSession() -> URLSession {
        let configuration = URLSessionConfiguration.default
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.urlCache = nil

        // Делегатная очередь не на main, с QoS
        let delegateQueue = OperationQueue()
        delegateQueue.qualityOfService = .userInitiated
        delegateQueue.maxConcurrentOperationCount = 1 // опционально, чтобы упорядочить колбэки

        return URLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)
    }

    func createUploadTask(from messageWithAttachment: PSDMessage, indexOfAttachment: Int, delegate: PSDMessageSendDelegate?) {
        guard let attachments = messageWithAttachment.attachments, attachments.count > indexOfAttachment else { return }
        guard attachments[indexOfAttachment].data.count > 0 else { return }

        let taskData = PSDTasksData(messageWithAttachment, delegate: delegate, file: attachments[indexOfAttachment])

        var request = URLRequest.createUploadRequest()
        request.setValue("gzip, deflate", forHTTPHeaderField: "Accept-Encoding")
        request.addUserAgent()
        request.setValue("multipart/form-data; boundary=\(PSDUploader.boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append(PSDUploader.createPrefixData(from: taskData.file))
        body.append(taskData.file.data)
        body.append(PSDUploader.createPostfixData())

        if let streamSize = try? PSDUploader.calculateContentLength(for: taskData.file) {
            request.setValue("\(streamSize)", forHTTPHeaderField: "Content-Length")
        }

        // Локально получить/создать сессию под замком
        stateQueue.sync {
            if downloadsSession == nil {
                downloadsSession = createSession()
            }
        }

        // Не блокируем вызывающий поток. Троттлим внутри рабочей очереди.
        PSDUploader.dispatchQueue.async { [weak self] in
            guard let self = self else { return }

            // Таймаут, чтобы не висеть вечно при форс-мажорах
            let waitResult = PSDUploader.semaphore.wait(timeout: .now() + .seconds(60))
            guard waitResult == .success else {
                return
            }

            // Создаём таск и регистрируем его атомарно
            var taskRef: URLSessionUploadTask?
            self.stateQueue.sync {
                if let session = self.downloadsSession {
                    let task = session.uploadTask(with: request, from: body)
                    self.tasksMap[task] = TaskEntry(data: taskData, didFinish: false)
                    taskRef = task
                }
            }

            guard let task = taskRef else {
                // Если по какой-то причине не создали таск — сразу освобождаем слот
                PSDUploader.semaphore.signal()
                return
            }

            task.priority = URLSessionTask.highPriority
            task.resume()
        }
    }

    func stopAll() {
        // Снимем все задачи и инвалидацию тоже под замком
        var session: URLSession?
        stateQueue.sync {
            session = downloadsSession
        }
        session?.getTasksWithCompletionHandler { (_, _, downloadTasks) in
            for task in downloadTasks { task.cancel() }
            session?.invalidateAndCancel()
            self.stateQueue.sync { self.downloadsSession = nil }
        }
    }

    func stopUpload(task: URLSessionTask) {
        task.cancel()
        var removed: PSDTasksData?
        stateQueue.sync {
            if var entry = tasksMap[task] {
                removed = entry.data
                tasksMap.removeValue(forKey: task)
                // Если мы сами сняли задачу до колбэка — освободим семафор,
                // если endUpload/complete ещё не происходил.
                if entry.didFinish == false {
                    entry.didFinish = true
                    PSDUploader.semaphore.signal()
                }
            }
        }
        if let data = removed {
            data.sendingDelegate?.remove(message: data.message)
            let userInfo: [String: Any] = ["commandId": data.message.commandId ?? ""]
            NotificationCenter.default.post(name: .removeMesssageNotification, object: nil, userInfo: userInfo)
            PSDMessagesStorage.remove(messageId: data.message.clientId)
        }
    }

    // MARK: endUpload — безопасно сигналим один раз
    private func endUpload(_ guid: String?, task: URLSessionTask) {
        var uploadData: PSDTasksData?

        stateQueue.sync {
            guard var entry = tasksMap[task], entry.didFinish == false else { return }
            entry.didFinish = true
            tasksMap[task] = entry
            uploadData = entry.data
        }

        guard let data = uploadData else { return }

        // Сначала освобождаем слот — чтобы не держать троттлинг, пока делаем логику
        PSDUploader.semaphore.signal()

        if let guid, guid.isEmpty == false {
            PSDTasksData.updateMessage(data.message, with: data.file, uploadingProgress: 0.95)
            data.file.serverIdentifer = guid
            let userInfo: [String: Any] = ["commandId": data.message.commandId ?? ""]
            NotificationCenter.default.post(name: .refreshNotification, object: nil, userInfo: userInfo)
            PSDMessageSend.pass(data.message, delegate: data.sendingDelegate)
        } else {
            // Ошибка
            data.file.isLoading = false
            PSDTasksData.updateMessage(data.message, with: data.file, uploadingProgress: 0.0)
            PSDMessageSend.fileSendingEndWithError(data.message, delegate: data.sendingDelegate)
        }

        // Удаляем таск из карты (после логики)
        stateQueue.sync {
            tasksMap.removeValue(forKey: task)
        }
    }

    ///Create user Agent String
    private static func userAgent()->String{
        let device : UIDevice = UIDevice.current
        let infoDict = Bundle.main.infoDictionary
        let appVersion :String = infoDict?["CFBundleVersion"] as! String

        return "\(CustomizationHelper.chatTitle)/\(appVersion)/\(PyrusServiceDesk.authorName)/\(device.systemVersion)"

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
}

extension PSDUploader: URLSessionTaskDelegate, URLSessionDelegate, URLSessionDataDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        // Если была ошибка — завершаем с пустым guid
        if let _ = error {
            endUpload("", task: task)
        } else {
            // Если успех, но по какой-то причине didReceive data не дал нам guid,
            // завершим как ошибку, чтобы не “утек” семафор.
            // Этот вызов будет no-op, если endUpload уже был вызван в didReceive.
            endUpload("", task: task)
        }
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask,
                    didReceive response: URLResponse,
                    completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        completionHandler(.allow)
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        // Пытаемся вытащить guid; если не вышло — не беда, didComplete закроет задачу как ошибку.
        if
            let dict = (try? JSONSerialization.jsonObject(with: data, options: .allowFragments)) as? [String: Any]
        {
            let guid = dict.stringOfKey(guidParameter)
            if !guid.isEmpty {
                endUpload(guid, task: dataTask)
            }
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask,
                    didSendBodyData bytesSent: Int64, totalBytesSent: Int64, totalBytesExpectedToSend: Int64) {
        var uploadData: PSDTasksData?
        stateQueue.sync {
            uploadData = tasksMap[task]?.data
        }
        if let u = uploadData, totalBytesExpectedToSend > 0 {
            let progress = Float(totalBytesSent) / Float(totalBytesExpectedToSend)
            PSDTasksData.updateMessage(u.message, with: u.file, uploadingProgress: min(0.90, CGFloat(progress)))
            u.sendingDelegate?.refresh(message: u.message, changedToSent: false)
        }
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask,
                    willCacheResponse proposedResponse: CachedURLResponse,
                    completionHandler: @escaping (CachedURLResponse?) -> Void) {
        completionHandler(nil)
    }
}

// Уведомления
extension Notification.Name {
    static let refreshNotification = Notification.Name("refreshNotification")
    static let removeMesssageNotification = Notification.Name("removeMesssageNotification")
}
