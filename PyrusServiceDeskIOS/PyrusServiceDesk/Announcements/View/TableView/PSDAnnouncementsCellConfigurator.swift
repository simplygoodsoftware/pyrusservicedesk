import UIKit

final class PSDAnnouncementsCellConfigurator: TableViewCellConfiguratorProtocol {

    private let psdAnnouncementCellIdentifier = ReuseIdentifier<PSDAnnouncementCell>(identifier:  PSDAnnouncementCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(PSDAnnouncementCell.self, forCellReuseIdentifier: PSDAnnouncementCell.identifier)
    }

    func getCell(model: AnnouncementsViewModel, indexPath: IndexPath) -> UITableViewCell {
        switch model.type {
        case .announcement:
            if let announcementModel = model.data as? PSDAnnouncement {
                let cell = getCell(reuseIdentifier: psdAnnouncementCellIdentifier, indexPath: indexPath)
                cell.configure(with: announcementModel)
                return cell
            }
        }
        return UITableViewCell()
    }
}
