import UIKit

protocol PSDChatsSectionModelProtocol: Hashable {
    var title: String { get }
    var isOpen: Bool { get }
}

struct PSDChatsSectionModel: PSDChatsSectionModelProtocol {
    let id = UUID()
    var title: String = ""
    var isOpen: Bool = false
}
