import Foundation

struct ClosedTicketsCellModel: Hashable {
    
    let title = "ClosedTickets".localizedPSD()
    let count: Int
    let isOpen: Bool
    weak var delegate: ClosedTicketsCellDelegate?
    
    init(count: Int, isOpen: Bool, delegate: ClosedTicketsCellDelegate?) {
        self.count = count
        self.isOpen = isOpen
        self.delegate = delegate
    }
    
    static func == (lhs: ClosedTicketsCellModel, rhs: ClosedTicketsCellModel) -> Bool {
        return lhs.count == rhs.count
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(count)
    }
}
