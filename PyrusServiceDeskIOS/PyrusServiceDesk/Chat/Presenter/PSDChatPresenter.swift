import Foundation

class PSDChatPresenter {
    weak var view: PSDChatViewProtocol?
}

extension PSDChatPresenter: PSDChatPresenterProtocol {
    func doWork(_ action: PSDChatPresenterCommand) {
        switch action {
        case .updateButtons(buttons: let buttons):
            view?.show(.updateButtons(buttons: buttons))
        case .updateRows:
            view?.show(.updateRows)
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
            view?.show(.updateTableMatrix(matrix: matrix.reversed().map { $0.reversed() }))
        case .addRow(scrollsToBottom: let scrollsToBottom):
            view?.show(.addRow(scrollsToBottom: scrollsToBottom))
        case .addNewRow:
            view?.show(.addNewRow)
        case .redrawCell(indexPath: let indexPath, message: let message):
            view?.show(.redrawCell(indexPath: indexPath, message: message))
        case .showKeyBoard:
            view?.show(.showKeyBoard)
        case .reloadAll(animated: let animated):
            view?.show(.reloadAll(animated: animated))
        case .updateTitle(connectionError: let connectionError):
            view?.show(.updateTitle(connectionError: connectionError))
        case .reloadTitle:
            view?.show(.reloadTitle)
        case .updateBadge(messagesCount: let messagesCount):
            view?.show(.updateBadge(messagesCount: messagesCount))
        case .scrollToRow(indexPath: let indexPath):
            view?.show(.scrollToRow(indexPath: indexPath))
        case .updateActive(isActive: let isActive):
            view?.show(.updateActive(isActive: isActive))
        }
    }
}
