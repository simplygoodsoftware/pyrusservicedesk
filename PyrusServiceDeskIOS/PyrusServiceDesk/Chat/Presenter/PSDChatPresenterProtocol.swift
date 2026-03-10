import Foundation

enum PSDChatPresenterCommand {
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
    case addMessage(scrollsToBottom: Bool, message: PSDRowMessage)
    case addNewRow
    case redrawCell(indexPath: IndexPath, message: PSDRowMessage)
    case showKeyBoard
    case reloadAll(animated: Bool)
    case updateTitle(connectionError: Bool)
    case reloadTitle
    case updateBadge(messagesCount: Int)
    case scrollToRow(indexPath: IndexPath)
    case scrollToRowTop(indexPath: IndexPath)
    case updateActive(isActive: Bool)
    case updateInfo(ticketId: Int, userName: String, createdAt: Date)
    case showRatingComment(ratingText: String?, rating: Int)
    case updateOperatorTime(timeMessage: String?)
    case updateDraft(ticketId: Int)
}


protocol PSDChatPresenterProtocol {
    func doWork(_ action: PSDChatPresenterCommand)
}

