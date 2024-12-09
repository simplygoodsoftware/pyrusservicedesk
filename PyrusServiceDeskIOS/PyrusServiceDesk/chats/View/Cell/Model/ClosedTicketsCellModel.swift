import Foundation

class ClosedTicketsCellModel: PSDChatsViewModelProtocol {
    let title = "Закрытые обращения"
    let count: Int
    let isOpen: Bool
    weak var delegate: ClosedTicketsCellDelegate?
    
    init(count: Int, isOpen: Bool, delegate: ClosedTicketsCellDelegate?) {
        self.count = count
        self.isOpen = isOpen
        self.delegate = delegate
        super.init(type: .header)
    }
    
    static func == (lhs: ClosedTicketsCellModel, rhs: ClosedTicketsCellModel) -> Bool {
        return lhs.count == rhs.count
    }
    
    override func hash(into hasher: inout Hasher) {
        hasher.combine(count)
    }
}
