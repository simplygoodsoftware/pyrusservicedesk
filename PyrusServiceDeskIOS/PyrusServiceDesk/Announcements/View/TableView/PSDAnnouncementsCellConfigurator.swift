import UIKit

final class PSDAnnouncementsCellConfigurator: TableViewCellConfiguratorProtocol {

    private let psdAnnouncementCellIdentifier = ReuseIdentifier<PSDAnnouncementCell>(identifier:  PSDAnnouncementCell.identifier)
    private let psdAnnouncementsReadedCellIdentifier = ReuseIdentifier<AnnouncementsReadCell>(identifier:  AnnouncementsReadCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(PSDAnnouncementCell.self, forCellReuseIdentifier: PSDAnnouncementCell.identifier)
        tableView.register(AnnouncementsReadCell.self, forCellReuseIdentifier: AnnouncementsReadCell.identifier)
    }

    func getCell(model: AnnouncementsViewModel, indexPath: IndexPath, delegate: AnnouncementsAttachmentsDelegate) -> UITableViewCell {
        switch model.type {
        case .announcement:
            if let announcementModel = model.data as? PSDAnnouncement {
                let cell = getCell(reuseIdentifier: psdAnnouncementCellIdentifier, indexPath: indexPath)
                cell.configure(with: announcementModel, delegate: delegate)
                return cell
            }
        case .announcementsRead:
            if model.data is AnnouncementsReadModel {
                let cell = getCell(reuseIdentifier: psdAnnouncementsReadedCellIdentifier, indexPath: indexPath)
                return cell
            }
        }
        return UITableViewCell()
    }
}
