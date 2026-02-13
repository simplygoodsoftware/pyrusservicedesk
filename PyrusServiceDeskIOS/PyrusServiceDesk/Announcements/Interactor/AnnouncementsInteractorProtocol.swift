
enum AnnouncementsInteractorCommand {
    case viewDidload
    case viewWillAppear
    case reloadAnnouncements
    case updateSelected(index: Int)
}

protocol AnnouncementsInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: AnnouncementsInteractorCommand)
}
