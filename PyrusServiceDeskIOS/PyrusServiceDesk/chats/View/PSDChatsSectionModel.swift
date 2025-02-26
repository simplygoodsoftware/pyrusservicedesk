import UIKit


struct PSDChatsSectionModel: Hashable {
    let id = UUID()
    var title: String = ""
    var isOpen: Bool = false
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(title)
        hasher.combine(isOpen)
    }
    
    static func == (lhs: PSDChatsSectionModel, rhs: PSDChatsSectionModel) -> Bool {
        return lhs.id == rhs.id && lhs.title == rhs.title && lhs.isOpen == rhs.isOpen
    }
}
