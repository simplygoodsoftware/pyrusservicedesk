import Foundation

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
    private override init() {
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
        PyrusLogger.loggerQueue.async { [weak self] in
            guard let self = self else{
                return
            }
            if self.loglines.count >= LOGLINES_BUFFER{
                self.flush2disk()
            }
            self.loglines.append(line)
        }
    }
    ///Returns data from logs files in gZip format
    ///- parameter controller: The LogsSendProtocol, that
    func collectLogs(in controller: LogsSendProtocol) {
        PyrusLogger.loggerQueue.async { [weak self] in
            let version = Bundle.main.object(forInfoDictionaryKey: BUNDLE_VERSION_KEY) as? String
            let device = UIDevice.current
            let intro = "Build = \(version ?? DEFAULT_VERSION), IOS = \(device.systemVersion), \(UIDevice.modelName)"
            self?.loglines.append("Finalizing log. \(intro), name = \(device.name)")
            
            let content = self?.getLocalLog()
            let body = intro.appending("\n\n\(content ?? "error getLocalLog")")
            let data = body.data(using: .utf8)
            let dataZ = Data.gzipData(data)
            let url = URL(string: String(format: LOG_FILE_NAME, PyrusServiceDesk.userId))
            DispatchQueue.main.async {
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
    static func directoryOldLogs() -> URL? {
        let paths = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)
        let cachesDirectory = URL(fileURLWithPath: paths.last ?? "")
        return cachesDirectory.appendingPathComponent(OLD_LOGS_PATH, isDirectory: true)
    }
    static func oldLogFileURL() -> URL? {
        let fileManager = FileManager.default
        guard let directoryToOldLogs = directoryOldLogs() else {
            return nil
        }
        if !fileManager.fileExists(atPath: directoryToOldLogs.path) {
            try? fileManager.createDirectory(atPath: directoryToOldLogs.path, withIntermediateDirectories: true, attributes: nil)
        }
        let oldLogFileName = Date().stringWithFormat(LOG_DATE_FILE_FORMAT)
        return directoryToOldLogs.appendingPathComponent(oldLogFileName, isDirectory: false)
        
    }
    func flush2disk() {
        guard loglines.count > 0 else {
            return
        }
        var lines = loglines.joined()
        loglines.removeAll()
        lines.append("\n")
        let docUrl = PSDFilesManager.getDocumentsDirectory()
        if !FileManager.default.fileExists(atPath: docUrl.path){
            try? FileManager.default.createDirectory(at: docUrl, withIntermediateDirectories: true, attributes: nil)
        }
        var fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.path)
        if fileHandler == nil {
            try? "".write(toFile: PyrusLogger.logPath.path, atomically: true, encoding: .utf8)
                fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.path)
        }
        guard let data = lines.data(using: .utf8) else {
            return
        }
        fileHandler?.synchronizeFile()
        fileHandler?.seekToEndOfFile()
        fileHandler?.write(data)
        fileHandler?.synchronizeFile()
        fileHandler?.closeFile()
        checkFileSize()
    }
    func checkFileSize() {
        guard let attributes = try? FileManager.default.attributesOfItem(atPath:  PyrusLogger.logPath.path) else {
            return
        }
        let fileSize = attributes[.size] as? Int
        if fileSize ?? 0 > MAX_LOG_SIZE, let url = PyrusLogger.oldLogFileURL() {
            try? FileManager.default.moveItem(at: PyrusLogger.logPath, to: url)
        }
            
        
    }
    func getLocalLog() -> String {
        flush2disk()
        let log = try? String(contentsOfFile: PyrusLogger.logPath.path, encoding: .utf8)
        let oldLogs = getSortedLogFiles()
        var oldLog: String = ""
        if oldLogs.count > 0, let lastLog = oldLogs.last, let pathForLogs = PyrusLogger.directoryOldLogs(){
            let pathForOldFile = pathForLogs.appendingPathComponent(lastLog)
            try? oldLog = String(contentsOfFile: pathForOldFile.path, encoding: .utf8)
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
            guard let files = try? fileManger.contentsOfDirectory(atPath: directory.path) else {
                return logFiles
            }
            for file in files {
                logFiles.append(file)
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
                let pathToRemove = pathForLogs.appendingPathComponent(path, isDirectory: false)
                try? fileManger.removeItem(atPath: pathToRemove.path)
            }
        }
    }
}
private let LOGLINES_BUFFER = 300
private let MAX_LOG_SIZE = 10000000
private let MAX_LOG_FILES_COUNT = 1
private let LOG_FILE_PATH = "local_log.txt"
private let BUNDLE_VERSION_KEY = "CFBundleVersion"
private let OLD_LOGS_PATH = "PSDOldLogs"
private let DEFAULT_VERSION = "error get version"
private let LOG_FILE_NAME = "pyrus_ios_%lu.txt.gz"
private let LOG_DATE_FORMAT = "dd(Z) HH:mm:ss.SSS"
private let LOG_DATE_FILE_FORMAT = "d_MM_YYYY_HH_mm_ss"
