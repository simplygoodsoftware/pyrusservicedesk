import Foundation
import zlib

let LOG_DATE_FORMAT = "dd(Z) HH:mm:ss.SSS"
let LOG_DATE_FILE_FORMAT = "d_MM_YYYY_HH_mm_ss"

class PyrusLogger: NSObject {
    static let shared = PyrusLogger()
    fileprivate var loglines = [String]()
    func logEvent(_ loggedString: String) {
        let tId: mach_port_t = pthread_mach_thread_np(pthread_self())
        let line = "\(Date().stringWithFormat(LOG_DATE_FORMAT)) [\(tId)] \(loggedString)\n"
        PyrusLogger.loggerQueue.async { [weak self] in
            if self?.loglines.count ?? 0 >= LOGLINES_BUFFER{
                self?.flush2disk()
            }
            self?.loglines.append(line)
        }
    }
    func collectLogs() {
        PyrusLogger.loggerQueue.async { [weak self] in
            let version = Bundle.main.object(forInfoDictionaryKey: BUNDLE_VERSION_KEY) as? String
            let device = UIDevice.current
            let intro = "Build = \(version ?? DEFAULT_VERSION), IOS = \(device.systemVersion), \(UIDevice.modelName)"
            self?.loglines.append("Finalizing log. \(intro), name = \(device.name)")
            
            let content = self?.getLocalLog()
            let body = intro.appending("\n\n\(content ?? "error getLocalLog")")
            let lsizekb = 0
            let data = body.data(using: .utf8)
            let dataZ = data.com
        }
    }
}
private extension PyrusLogger {
    static let loggerQueue = DispatchQueue(label: "com.pyrus_service_desk.logger.queue.serial")
    static let logPath: URL = {
        var pyrusPath = PSDFilesManager.getDocumentsDirectory()
        pyrusPath.appendPathComponent(LOG_FILE_PATH)
        return pyrusPath
    }()
    static func directoryOldLogs() -> String? {
        return (NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true).last as NSString?)?.appendingPathComponent(OLD_LOGS_PATH)
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
            return
        }
        var lines = loglines.joined()
        loglines.removeAll()
        lines.append("\n")
        var fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.absoluteString)
        if fileHandler == nil {
            do {
                try "".write(toFile: PyrusLogger.logPath.absoluteString, atomically: true, encoding: .utf8)
                fileHandler = FileHandle(forWritingAtPath: PyrusLogger.logPath.absoluteString)
            } catch {
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
        checkFileSize()
    }
    func checkFileSize() {
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath:  PyrusLogger.logPath.absoluteString)
            let fileSize = attributes[.size] as? Int//test
            if fileSize ?? 0 > MAX_LOG_SIZE, let url = PyrusLogger.oldLogFileURL() {
                try FileManager.default.moveItem(at: PyrusLogger.logPath, to: url)
            }
        } catch {
        }
    }
    func getLocalLog() -> String {
        flush2disk()
        let log : String?
        do {
            try log = String(contentsOfFile: PyrusLogger.logPath.absoluteString, encoding: .utf8)
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
            let date = Date.getDate(from: firstFileName, with: LOG_DATE_FILE_FORMAT)
            let firstDate = Date.getDate(from: firstFileName, with: LOG_DATE_FILE_FORMAT)
            let secondDate = Date.getDate(from: firstFileName, with: LOG_DATE_FILE_FORMAT)
            return firstDate?.compare(secondDate) ?? false
        })
        return logFiles
    }
}
private let LOGLINES_BUFFER = 300
private let MAX_LOG_SIZE = 10000000
private let LOG_FILE_PATH = "local_log.txt"
private let BUNDLE_VERSION_KEY = "CFBundleVersion"
private let OLD_LOGS_PATH = "oldLogs"
private let DEFAULT_VERSION = "error get version"
