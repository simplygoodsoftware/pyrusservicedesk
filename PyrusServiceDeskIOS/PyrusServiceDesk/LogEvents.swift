import Foundation
///The protocol for log some evnts from PyrusServiceDesk
@objc public protocol LogEvents{
    ///The callback that PyrusServiceDesk was closed
    @objc func logPyrusServiceDesk(event: String)
}
enum EventsLogger {
    case didNotFindMessageAfterUpdate
    static func logEvent(_ logCase: EventsLogger, additionalInfo: String? = nil){
        guard var logString = stringForEvent(.didNotFindMessageAfterUpdate) else{
            return
        }
        if let additionalInfo = additionalInfo{
            logString = logString + " " + additionalInfo
        }
        PyrusServiceDesk.logEvent?.logPyrusServiceDesk(event: logString)
    }
    static private func stringForEvent(_ logCase: EventsLogger) -> String? {
        let defaultString = "PyrusServiceDesk: "
        switch logCase {
        case .didNotFindMessageAfterUpdate:
            return defaultString + "Message was not finded in chat after sent"
        default:
            return nil
        }
    }
}
