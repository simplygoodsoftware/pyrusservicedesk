import Foundation

@objc public class PSDUserInfo: NSObject {
    let clientId: String
    let clientName: String
    let userId: String
    var userName: String
    let secretKey: String?
    var lastNoteId: Int?
    var authors: [AuthorInfo] = []

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
        
        return self.clientId == other.clientId && self.userId == other.userId
    }
    
    @objc public class AuthorInfo: NSObject {
        let id: String
        let name: String
        let phone: String?
        
        init(id: String, name: String, phone: String?) {
            self.id = id
            self.name = name
            self.phone = phone
        }
    }
}
