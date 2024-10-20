import Foundation

@objc public class PSDUserInfo: NSObject {
    let clientId: String
    let clientName: String
    let userId: String
    let userName: String
    let secretKey: String?

    public init(appId: String, clientName: String, userId: String, userName: String, secretKey: String?) {
        self.clientId = appId
        self.clientName = clientName
        self.userId = userId
        self.userName = userName
        self.secretKey = secretKey
    }
    
    override public func isEqual(_ object: Any?) -> Bool {
        guard let other = object as? PSDUserInfo else {
            return false
        }
        
        return self.clientId == other.clientId &&
        self.clientName == other.clientName &&
        self.userId == other.userId &&
        self.userName == other.userName &&
        self.secretKey == other.secretKey
    }
}