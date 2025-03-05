import Foundation

enum PSDChatSearchViewCommand {
    case updateButtons(buttons: [ButtonData]?)
    case updateRows
    case removeNoConnectionView
    case endRefreshing
    case reloadChat
    case needShowRate(showRate: Bool)
    case showNoConnectionView
    case scrollsToBottom(animated: Bool)
    case endLoading
    case dataIsShown
    case drawTableWithData
    case updateTableMatrix(matrix: [[PSDRowMessage]])
    case addRow(scrollsToBottom: Bool)
    case addNewRow
    case redrawCell(indexPath: IndexPath, message: PSDRowMessage)
    case showKeyBoard
    case reloadAll(animated: Bool)
    case updateTitle(connectionError: Bool)
    case reloadTitle
    case updateBadge(messagesCount: Int)
    case scrollToRow(indexPath: IndexPath)
    case updateActive(isActive: Bool)
}

protocol PSDChatViewProtocol: NSObjectProtocol {
    func show(_ action: PSDChatSearchViewCommand)
}
