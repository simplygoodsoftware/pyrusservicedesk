import UIKit

final class PSDChatsCellConfigurator: TableViewCellConfiguratorProtocol {

    private let psdChatInfoCellIdentifier = ReuseIdentifier<PSDChatInfoTableViewCell>(identifier:  PSDChatInfoTableViewCell.identifier)
    private let psdClosedTicketsCellIdentifier = ReuseIdentifier<ClosedTicketsCell>(identifier:  ClosedTicketsCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(PSDChatInfoTableViewCell.self, forCellReuseIdentifier: PSDChatInfoTableViewCell.identifier)
        tableView.register(ClosedTicketsCell.self, forCellReuseIdentifier: ClosedTicketsCell.identifier)

    }

    func getCell(model: PSDChatsViewModelProtocol, indexPath: IndexPath) -> UITableViewCell {
        switch model.type {
        case .chat:
            if let chatModel = model as? ChatViewModel {
                let cell = getCell(reuseIdentifier: psdChatInfoCellIdentifier, indexPath: indexPath)
                cell.configure(with: chatModel)
                return cell
            }
        case .header:
            if let headerModel = model as? ClosedTicketsCellModel {
                let cell = getCell(reuseIdentifier: psdClosedTicketsCellIdentifier, indexPath: indexPath)
                cell.configure(with: headerModel)
                cell.selectionStyle = .none
                return cell
            }
        }
        return UITableViewCell()
    }
}
