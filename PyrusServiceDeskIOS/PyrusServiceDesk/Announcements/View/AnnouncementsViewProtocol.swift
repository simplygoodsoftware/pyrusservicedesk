import UIKit

enum AnnouncementsViewCommand {
    case updateAnnouncements(announcements: [[AnnouncementsViewModel]])
    case endRefresh
    case updateTitle(title: String)
    case updateTitles(titles: [TitleWithBadge], selectedIndex: Int)
    case updateSelected(index: Int)
    case updateIcon(image: UIImage?)
    case deleteSegmentControl
    case startRefresh
    case connectionError
}

protocol AnnouncementsViewProtocol: NSObjectProtocol {
    func show(_ action: AnnouncementsViewCommand)
}
