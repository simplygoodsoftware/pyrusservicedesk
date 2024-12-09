import Foundation

enum PSDChatsCellType {
    case chat
    case header
}

class PSDChatsViewModelProtocol: Hashable {
    static func == (lhs: PSDChatsViewModelProtocol, rhs: PSDChatsViewModelProtocol) -> Bool {
        return true
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(1)
    }
    
    let type: PSDChatsCellType
    
    init(type: PSDChatsCellType) {
        self.type = type
    }
}
