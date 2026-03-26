
import Foundation

extension String {
    
    enum CallersType: String {
        case message = "message"
        case chat = "chat"
        case lastMessage = "chat's last message"
        case none = "none"
    }
    
    /**
     Returns date (timeZone is UTC) from string with expected format.
     - parameter format: Is expected format of date.
     */
    func dateFromString(format: String, callerType: CallersType = .none, id: String? = nil) -> Date {
        if self.count == 0 {
            addLog(callerType: callerType, id: id)
            return Date()
        }
        
        let dateFormatter = DateFormatter()
        dateFormatter.timeZone = TimeZone(abbreviation: "UTC")
        dateFormatter.dateFormat = format
        guard let date = dateFormatter.date(from: self) else {
            print("Pyrus Service Desk Error: Date conversion failed due to mismatched format.")
            addLog(callerType: callerType, id: id)
            return Date()
        }
        return date
    }
    
    private func addLog(callerType: CallersType, id: String?) {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yy HH:mm:ss zzz"
        let errorMessage = "\(formatter.string(from: Date())) Date conversion failed: caller = \(callerType.rawValue), id = \(id ?? "nil")"
        let log = LogInfo(
            message: errorMessage,
            stack: PyrusLogger.shared.getLogFileContent()
        )
        PyrusServiceDesk.syncManager.logsToSend.append(log)
    }
}
