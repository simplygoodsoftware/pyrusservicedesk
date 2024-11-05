import Foundation

struct PSDClientInfo: Hashable {
    let clientId: String
    let clientName: String
    let clientIcon: String
    var image: UIImage? = nil
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(clientId)
    }
    
    static func == (lhs: PSDClientInfo, rhs: PSDClientInfo) -> Bool {
        return lhs.clientId == rhs.clientId
    }
}
