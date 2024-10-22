import Foundation

enum PSDChatInteractorCommand {
    case viewDidload
    case send(message: String, attachments: [PSDAttachment])
    case sendRate(rateValue: Int)
}

protocol PSDChatInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: PSDChatInteractorCommand)
}
