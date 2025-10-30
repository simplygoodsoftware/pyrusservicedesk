import Foundation

@available(iOS 13.0, *)
class KBDiffableDataSource: UITableViewDiffableDataSource<Int, PSDRowMessage> {
    private weak var cellProvider: UITableViewDataSource?
    
    static func createDataSource(for table: UITableView, cellCreator: UITableViewDataSource) -> UITableViewDiffableDataSource<Int, PSDRowMessage> {
        let data = KBDiffableDataSource(
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
