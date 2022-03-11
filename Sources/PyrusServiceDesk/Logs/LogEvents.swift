import Foundation
///The protocol for log some events from PyrusServiceDesk
@objc public protocol LogEvents{
    ///The callback that PyrusServiceDesk was closed
    @objc func logPyrusServiceDesk(event: String)
}
enum EventsLogger {
    case didNotFindMessageAfterUpdate
    case emptyClientId
    case previewOpenForEmptyAttachment
    case invalidPushToken
    case resignFirstResponder
    case openPSD
    case tooManyRefresh
    case invalidDomain
    static func logEvent(_ logCase: EventsLogger, additionalInfo: String? = nil){
        var logString = stringForEvent(logCase)
        if let additionalInfo = additionalInfo{
            logString = logString + ": " + additionalInfo
        }
        PyrusServiceDesk.logEvent?.logPyrusServiceDesk(event: logString)
        PyrusLogger.shared.logEvent(logString)
    }
    static private func stringForEvent(_ logCase: EventsLogger) -> String{
        let defaultString = "PyrusServiceDesk: "
        switch logCase {
        case .didNotFindMessageAfterUpdate:
            return defaultString + "Message was not finded in chat after sent"
        case .emptyClientId:
            return defaultString + "Empty clientId"
        case .previewOpenForEmptyAttachment:
            return defaultString + "Try to open preview for attachment with empty id"
        case .invalidPushToken:
            return defaultString + "Main application passed empty push token"
        case .resignFirstResponder:
            return defaultString + "ResignFirstResponder was called"
        case .openPSD:
            return defaultString + "PyrusServiceDesk open"
        case .tooManyRefresh:
            return defaultString + "PyrusServiceDesk too many refreshes"
        case .invalidDomain:
            return defaultString + "PyrusServiceDesk invalid domain"
        }
    }
}
