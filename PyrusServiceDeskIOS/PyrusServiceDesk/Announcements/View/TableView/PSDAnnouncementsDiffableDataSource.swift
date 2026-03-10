import UIKit

class PSDAnnouncementsDiffableDataSource: UITableViewDiffableDataSource<AnnouncementsSectionModel, AnnouncementsViewModel> {
    private weak var cellProvider: UITableViewDataSource?
    
    static func createDataSource(for table: UITableView, cellCreator: UITableViewDataSource) -> UITableViewDiffableDataSource<AnnouncementsSectionModel, AnnouncementsViewModel> {
        let data = PSDAnnouncementsDiffableDataSource(
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
