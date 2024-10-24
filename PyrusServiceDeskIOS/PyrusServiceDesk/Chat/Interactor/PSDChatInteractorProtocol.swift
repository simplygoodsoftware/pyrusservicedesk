import Foundation

enum PSDChatInteractorCommand {
    case viewDidload
    case send(message: String, attachments: [PSDAttachment])
    case sendRate(rateValue: Int)
    case refresh
    case addNewRow
}

protocol PSDChatInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: PSDChatInteractorCommand)
}
