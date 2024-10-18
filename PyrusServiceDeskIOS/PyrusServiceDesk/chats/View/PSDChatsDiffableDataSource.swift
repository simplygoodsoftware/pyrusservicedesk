import UIKit

@available(iOS 13.0, *)
class PSDChatsDiffableDataSource: UITableViewDiffableDataSource<PSDChatsSectionModel, AnyHashable> {
    private weak var cellProvider: UITableViewDataSource?
    
    static func createDataSource(for table: UITableView, cellCreator: UITableViewDataSource) -> UITableViewDiffableDataSource<PSDChatsSectionModel, AnyHashable> {
        let data = PSDChatsDiffableDataSource(
            tableView: table,
            cellProvider: {[weak cellCreator]  table, indexPath, _ in
                return cellCreator?.tableView(table, cellForRowAt: indexPath)
            }
        )
        data.cellProvider = cellCreator
        data.defaultRowAnimation = .fade
        return data
    }
}
