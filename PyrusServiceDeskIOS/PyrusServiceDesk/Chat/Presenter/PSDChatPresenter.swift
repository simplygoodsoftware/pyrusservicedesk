import Foundation

class PSDChatPresenter {
    weak var view: PSDChatViewProtocol?
}

extension PSDChatPresenter: PSDChatPresenterProtocol {
    func doWork(_ action: PSDChatPresenterCommand) {
        switch action {
        case .addFakeMessage(messageId: let messageId):
            view?.show(.addFakeMessage(messageId: messageId))
        case .updateButtons(buttons: let buttons):
            view?.show(.updateButtons(buttons: buttons))
        case .updateRows(indexPaths: let indexPaths):
            view?.show(.updateRows(indexPaths: indexPaths))
        case .removeNoConnectionView:
            view?.show(.removeNoConnectionView)
        case .endRefreshing:
            view?.show(.endRefreshing)
        case .reloadChat:
            view?.show(.reloadChat)
        case .needShowRate(showRate: let showRate):
            view?.show(.needShowRate(showRate: showRate))
        case .showNoConnectionView:
            view?.show(.showNoConnectionView)
        case .scrollsToBottom(animated: let animated):
            view?.show(.scrollsToBottom(animated: animated))
        case .endLoading:
            view?.show(.endLoading)
        case .dataIsShown:
            view?.show(.dataIsShown)
        case .drawTableWithData:
            view?.show(.drawTableWithData)
        case .updateTableMatrix(matrix: let matrix):
            view?.show(.updateTableMatrix(matrix: matrix))
        case .addRow(index: let index, lastIndexPath: let lastIndexPath, insertSections: let insertSections, scrollsToBottom: let scrollsToBottom):
            view?.show(.addRow(index: index, lastIndexPath: lastIndexPath, insertSections: insertSections, scrollsToBottom: scrollsToBottom))
        case .addNewRow:
            view?.show(.addNewRow)
        case .redrawCell(indexPath: let indexPath, message: let message):
            view?.show(.redrawCell(indexPath: indexPath, message: message))
        case .insertSections(sections: let sections):
            view?.show(.insertSections(sections: sections))
        case .deleteSections(sections: let sections):
            view?.show(.deleteSections(sections: sections))
        case .moveRow(movedIndexPath: let movedIndexPath, newIndexPath: let newIndexPath):
            view?.show(.moveRow(movedIndexPath: movedIndexPath, newIndexPath: newIndexPath))
        case .deleteRows(indexPaths: let indexPaths, section: let section):
            view?.show(.deleteRows(indexPaths: indexPaths, section: section))
        case .showKeyBoard:
            view?.show(.showKeyBoard)
        case .reloadAll:
            view?.show(.reloadAll)
        case .updateTitle(connectionError: let connectionError):
            view?.show(.updateTitle(connectionError: connectionError))
        case .reloadTitle:
            view?.show(.reloadTitle)
        case .updateBadge(messagesCount: let messagesCount):
            view?.show(.updateBadge(messagesCount: messagesCount))
        }
    }
}
