import UIKit

@available(iOS 13.0, *)
final class SearchChatCellConfigurator: TableViewCellConfiguratorProtocol {

    private let searchChatInfoCellIdentifier = ReuseIdentifier<SearchChatCell>(identifier:  SearchChatCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(SearchChatCell.self, forCellReuseIdentifier: SearchChatCell.identifier)
    }

    func getCell(model: SearchChatViewModel, indexPath: IndexPath) -> UITableViewCell {
        let cell = getCell(reuseIdentifier: searchChatInfoCellIdentifier, indexPath: indexPath)
        cell.configure(with: model)
        return cell
    }
}

