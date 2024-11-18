
import Foundation

@objc public protocol DeniedAccessCallBack {
    
    @objc func deleteUsers(userIds: [String])
}

