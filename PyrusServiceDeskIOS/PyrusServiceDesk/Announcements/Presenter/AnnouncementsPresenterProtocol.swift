
enum AnnouncementsPresenterCommand {
    case updateAnnouncements(announcements: [PSDAnnouncement], lastReadId: String?)
    case endRefresh
    case updateTitle(title: String?)
    case updateTitles(titles: [String], selectedIndex: Int)
    case updateSelected(index: Int)
    case updateIcon(image: UIImage?)
    case deleteSegmentControl
    case startRefresh
    case connectionError
}

protocol AnnouncementsPresenterProtocol {
    func doWork(_ action: AnnouncementsPresenterCommand)
}
