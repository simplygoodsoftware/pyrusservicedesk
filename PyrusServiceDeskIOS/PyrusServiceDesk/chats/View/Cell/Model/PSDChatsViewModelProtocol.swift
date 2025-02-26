import Foundation

enum PSDChatsCellType {
    case chat
    case header
}

struct PSDChatsViewModel: Hashable {
    static func == (lhs: PSDChatsViewModel, rhs: PSDChatsViewModel) -> Bool {
        return lhs.type == rhs.type && lhs.data == rhs.data
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(type)
        hasher.combine(data)
    }
    let data: AnyHashable
    let type: PSDChatsCellType
}
