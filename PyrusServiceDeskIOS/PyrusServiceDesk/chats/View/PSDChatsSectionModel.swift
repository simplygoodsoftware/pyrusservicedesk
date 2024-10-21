import UIKit

protocol PSDChatsSectionModelProtocol: Hashable {
    var title: String { get }
    var isOpen: Bool { get }
}

struct PSDChatsSectionModel: PSDChatsSectionModelProtocol {
    var title: String = ""
    var isOpen: Bool = false
}
