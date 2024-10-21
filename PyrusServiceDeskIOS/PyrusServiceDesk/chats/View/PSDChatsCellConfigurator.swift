import UIKit

final class PSDChatsCellConfigurator: TableViewCellConfiguratorProtocol {

    private let psdChatInfoCellIdentifier = ReuseIdentifier<PSDChatInfoTableViewCell>(identifier:  PSDChatInfoTableViewCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(PSDChatInfoTableViewCell.self, forCellReuseIdentifier: PSDChatInfoTableViewCell.identifier)
    }

    func getCell(model: ChatViewModel, indexPath: IndexPath) -> PSDChatInfoTableViewCell {
        let cell = getCell(reuseIdentifier: psdChatInfoCellIdentifier, indexPath: indexPath)
        cell.configure(with: model)
        return cell
    }
}
