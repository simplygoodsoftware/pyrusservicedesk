import Foundation

enum PSDChatInteractorCommand {
    case viewDidload
    case send(message: String, attachments: [PSDAttachment])
    case sendRate(rateValue: Int)
    case refresh
    case addNewRow
    case sendAgainMessage(indexPath: IndexPath)
    case deleteMessage(indexPath: IndexPath)
    case forceRefresh(showFakeMessage: Int?)
    case reloadChat
    case updateNoConnectionVisible(visible: Bool)
    case startGettingInfo
    case viewWillDisappear
    case viewDidAppear
    case scrollButtonVisibleUpdated(isHidden: Bool)
}

protocol PSDChatInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: PSDChatInteractorCommand)
}
