import Foundation

let LOG_DATE_FORMAT = "dd(Z) HH:mm:ss.SSS"
let LOG_DATE_FILE_FORMAT = "d_MM_YYYY_HH_mm_ss"
protocol LogsSendProtocol: FileChooser {
    func sendData(_ data: Data?, with Url: URL?)
}

///The class for log events to file.
///Usage : PyrusLogger.shared.logEvent(logString)
class PyrusLogger: NSObject {
    static let shared = PyrusLogger()
    fileprivate var loglines = [String]()
    @objc func saveLocalLogToDisk() {
        PyrusLogger.loggerQueue.async { [weak self] in
            self?.flush2disk()
        }
    }
    override init() {
        super.init()
        checkAndRemoveOldLogs()
        NotificationCenter.default.addObserver(self, selector: #selector(saveLocalLogToDisk), name: UIApplication.willResignActiveNotification, object: nil)
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    ///Write the lines to file
    func logEvent(_ loggedString: String) {
        let tId: mach_port_t = pthread_mach_thread_np(pthread_self())
        let line = "\(Date().stringWithFormat(LOG_DATE_FORMAT)) [\(tId)] \(loggedString)\n"
        print("logEvent = \(line)")//test
        PyrusLogger.loggerQueue.async { [weak self] in
            if self?.loglines.count ?? 0 >= LOGLINES_BUFFER{
                self?.flush2disk()
            }
            self?.loglines.append(line)
        }
    }
    ///Returns data from logs files in gZip format
    ///- parameter controller: The LogsSendProtocol, that
    func collectLogs(in controller: LogsSendProtocol) {
        print("collect logs start")//test
        PyrusLogger.loggerQueue.async { [weak self] in
            let version = Bundle.main.object(forInfoDictionaryKey: BUNDLE_VERSION_KEY) as? String
            let device = UIDevice.current
            let intro = "Build = \(version ?? DEFAULT_VERSION), IOS = \(device.systemVersion), \(UIDevice.modelName)"
            self?.loglines.append("Finalizing log. \(intro), name = \(device.name)")
            
            let content = self?.getLocalLog()
            let body = intro.appending("\n\n\(content ?? "error getLocalLog")")
            print("body = \(body)")
            let data = body.data(using: .utf8)
            let dataZ = Data.gzipData(data)
            let url = URL(string: String(format: LOG_FILE_NAME, PyrusServiceDesk.userId))
            DispatchQueue.main.async {
                print("collect logs end, controller = \(controller), dataZ = \(dataZ?.count), url = \(url)")//test
                controller.sendData(dataZ, with: url)
            }
        }
    }
}
private extension PyrusLogger {
    static let loggerQueue = DispatchQueue(label: "com.pyrus_service_desk.logger.queue.serial")
    static let logPath: URL = {
        var pyrusPath = PSDFilesManager.getDocumentsDirectory()
        pyrusPath.appendPathComponent(LOG_FILE_PATH, isDirectory: false)
        return pyrusPath
    }()
    static func directoryOldLogs() -> String? {
        return (NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true).last as NSString?)?.appendingPathComponent(OLD_LOGS_PATH)//test PSDFilesManager.getDocumentsDirectory()?
    }
    static func oldLogFileURL() -> URL? {
        let fileManager = FileManager.default
        guard let directoryToOldLogs = directoryOldLogs() else {
            return nil
        }
        if !fileManager.fileExists(atPath: directoryToOldLogs) {
            do {
                try fileManager.createDirectory(atPath: directoryToOldLogs, withIntermediateDirectories: true, attributes: nil)
            } catch {
            }
        }
        var oldLogFileName = Date().stringWithFormat(LOG_DATE_FILE_FORMAT)
        oldLogFileName = (directoryToOldLogs as NSString).appendingPathComponent(oldLogFileName)
        return URL(string: oldLogFileName)
        
    }
    func flush2disk() {
        guard loglines.count > 0 else {
            print("loglines is empty")//test
            return
        }
        var lines = loglines.joined()
        loglines.removeAll()
        lines.append("\n")
        let docUrl = PSDFilesManager.getDocumentsDirectory()
        if !FileManager.default.fileExists(atPath: docUrl.path){
            do {
                try FileManager.default.createDirectory(at: docUrl, withIntermediateDirectories: true, attributes: nil)
            }catch {
                print("Error create folder: \(error.localizedDescription)")//test
            }
        }
        var fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.path)
        print("logPath = \(PyrusLogger.logPath.path), fileHandler = \(fileHandler)")//test
        if fileHandler == nil {
            do {
                
                try "".write(toFile: PyrusLogger.logPath.path, atomically: true, encoding: .utf8)
                fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.path)
            } catch { error
                print("error WritingAtPath = \(error)")//test
            }
        }
        guard let data = lines.data(using: .utf8) else {
            return
        }
        fileHandler?.synchronizeFile()
        fileHandler?.seekToEndOfFile()
        fileHandler?.write(data)
        fileHandler?.synchronizeFile()
        fileHandler?.closeFile()
        print("flush2disk successed: \(fileHandler != nil)")//test
        checkFileSize()
    }
    func checkFileSize() {
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath:  PyrusLogger.logPath.path)
            let fileSize = attributes[.size] as? Int//test
            if fileSize ?? 0 > MAX_LOG_SIZE, let url = PyrusLogger.oldLogFileURL() {
                try FileManager.default.moveItem(at: PyrusLogger.logPath, to: url)
            }
        } catch {
        }
    }
    func getLocalLog() -> String {
        flush2disk()
        var log: String? = nil
        do {
            try log = String(contentsOfFile: PyrusLogger.logPath.path, encoding: .utf8)
        } catch {
        }
        let oldLogs = getSortedLogFiles()
        var oldLog: String = ""
        if oldLogs.count > 0, let lastLog = oldLogs.last, let pathForLogs = PyrusLogger.directoryOldLogs(){
            let pathForOldFile = (pathForLogs as NSString).appendingPathComponent(lastLog)
            do {
                try oldLog = String(contentsOfFile: pathForOldFile, encoding: .utf8)
            } catch {
            }
        }
        
        guard let log_ = log else {
            return oldLog
        }
        if oldLog.count > 0 {
            oldLog.append("\n\(log_)")
        }
        return log_
    }
    func getSortedLogFiles() -> [String] {
        let fileManger = FileManager.default
        var logFiles = [String]()
        if let directory = PyrusLogger.directoryOldLogs(){
            do{
                let files = try fileManger.contentsOfDirectory(atPath: directory)
                for file in files {
                    logFiles.append(file)
                }
            } catch {
            }
        }
        logFiles.sort(by: {
            (str1, str2) in
            let firstFileName = (str1 as NSString).deletingPathExtension
            let secondFileName = (str2 as NSString).deletingPathExtension
            guard let firstDate = Date.getDate(from: firstFileName, with: LOG_DATE_FILE_FORMAT), let secondDate = Date.getDate(from: secondFileName, with: LOG_DATE_FILE_FORMAT) else {
                return false
            }
            return firstDate.compare(secondDate) == .orderedDescending
        })
        return logFiles
    }
    func checkAndRemoveOldLogs() {
        PyrusLogger.loggerQueue.async { [weak self] in
            guard let pathForLogs = PyrusLogger.directoryOldLogs(), let logFiles = self?.getSortedLogFiles(), logFiles.count > MAX_LOG_FILES_COUNT else {
                return
            }
            let fileManger = FileManager.default
            for (i,path) in logFiles.enumerated(){
                guard i < logFiles.count - MAX_LOG_FILES_COUNT else {
                    return
                }
                let pathToRemove = (pathForLogs as NSString).appendingPathComponent(path)
                do{
                    try fileManger.removeItem(atPath: pathToRemove)
                } catch {
                    
                }
            }
        }
    }
}
private let LOGLINES_BUFFER = 300
private let MAX_LOG_SIZE = 10000000
private let MAX_LOG_FILES_COUNT = 1
private let LOG_FILE_PATH = "local_log.txt"
private let BUNDLE_VERSION_KEY = "CFBundleVersion"
private let OLD_LOGS_PATH = "oldLogs"
private let DEFAULT_VERSION = "error get version"
private let LOG_FILE_NAME = "pyrus_ios_%lu.txt.gz"
