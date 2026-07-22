import UIKit

final class PSDAnnouncementsDiffableDataSource: UITableViewDiffableDataSource<AnnouncementsSection, AnnouncementsViewModel> {

    /// Ячейка конфигурируется из item identifier'а снапшота, а не из внешнего массива
    /// по indexPath — во время анимированного apply массив и снапшот могут разъехаться.
    static func createDataSource(
        for table: UITableView,
        cellConfigurator: PSDAnnouncementsCellConfigurator,
        attachmentsDelegate: AnnouncementsAttachmentsDelegate
    ) -> PSDAnnouncementsDiffableDataSource {
        let dataSource = PSDAnnouncementsDiffableDataSource(
            tableView: table,
            cellProvider: { [weak cellConfigurator, weak attachmentsDelegate] _, indexPath, item in
                cellConfigurator?.getCell(model: item, indexPath: indexPath, delegate: attachmentsDelegate)
            }
        )
        dataSource.defaultRowAnimation = .fade
        return dataSource
    }
}
