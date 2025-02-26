import Foundation

enum PSDChatSearchViewCommand {
    case addFakeMessage(messageId: Int)
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
    case addRow(index: Int, lastIndexPath: IndexPath, insertSections: Bool, scrollsToBottom: Bool)
    case addNewRow
    case redrawCell(indexPath: IndexPath, message: PSDRowMessage)
    case insertSections(sections: IndexSet)
    case deleteSections(sections: IndexSet)
    case moveRow(movedIndexPath: IndexPath, newIndexPath: IndexPath)
    case deleteRows(indexPaths: [IndexPath], section: Int)
    case showKeyBoard
    case reloadAll
    case updateTitle(connectionError: Bool)
    case reloadTitle
    case updateBadge(messagesCount: Int)
    case scrollToRow(indexPath: IndexPath)
}

protocol PSDChatViewProtocol: NSObjectProtocol {
    func show(_ action: PSDChatSearchViewCommand)
}
