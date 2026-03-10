
class AnnouncementsPresenter: NSObject {
    private let chatsPrepareQueue = DispatchQueue.init(label: "prepareChats")

    weak var view: AnnouncementsViewProtocol?
    private var isClosedTicketsOpened: Bool = false
    private var announcements = [PSDAnnouncement]()
}

extension AnnouncementsPresenter: AnnouncementsPresenterProtocol {
    func doWork(_ action: AnnouncementsPresenterCommand) {
        switch action {
        case .updateAnnouncements(announcements: let announcements, lastReadId: let lastReadId):
            let startTime = DispatchTime.now()
            self.announcements = announcements
            updateAnnouncements(lastReadId: lastReadId)
//            let endTime = DispatchTime.now()
//            let nanoTime = endTime.uptimeNanoseconds - startTime.uptimeNanoseconds
//            let timeInterval = Double(nanoTime) / 1_000_000
//            print("⏱ updateChats выполнена за \(timeInterval) мс (обработано \(chats.count) чатов)")
        case .endRefresh:
            view?.show(.endRefresh)
        case .updateTitle(title: let title):
            view?.show(.updateTitle(title: title ?? "Announcements".localizedPSD()))
        case .updateTitles(titles: let titles, selectedIndex: let selectedIndex):
            let titles = titles.map({ TitleWithBadge(title: $0) })
            view?.show(.updateTitle(title: "Announcements".localizedPSD()))
            view?.show(.updateTitles(titles: titles, selectedIndex: selectedIndex))
        case .updateSelected(index: let index):
            view?.show(.updateSelected(index: index))
        case .updateIcon(image: let image):
            view?.show(.updateIcon(image: image ?? UIImage.PSDImage(name: "iiko")))
        case .deleteSegmentControl:
            view?.show(.deleteSegmentControl)
        case .startRefresh:
            view?.show(.startRefresh)
        case .connectionError:
            view?.show(.connectionError)
        }
    }
}

private extension AnnouncementsPresenter {
    func updateAnnouncements(lastReadId: String?) {

        var isRead = false
        if lastReadId?.count ?? 0 == 0 {
           isRead = true
        }
        var anns = [AnnouncementsViewModel]()
        for (i, announcement) in announcements.enumerated() {
            if announcement.id == lastReadId {
                isRead = true
                if i > 0 {
                    anns.append(AnnouncementsViewModel(data: AnnouncementsReadModel(id: 0), type: .announcementsRead))
                }
            }
            
            let ann = PSDAnnouncement(
                id: announcement.id,
                text: announcement.text,
                date: announcement.date,
                isRead: isRead,
                attachments: announcement.attachments,
                appId: announcement.appId
            )
            anns.append(AnnouncementsViewModel(data: ann, type: .announcement))
        }
        
        view?.show(.updateAnnouncements(announcements: [anns]))
    }
}

private extension UIFont {
    static let timeLabel = CustomizationHelper.systemFont(ofSize: 13.0)
    static let messageLabel = CustomizationHelper.systemBoldFont(ofSize: 17)
    static let notificationButton = CustomizationHelper.systemFont(ofSize: 12.0)
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}

private extension UIColor {
    static let timeLabel = UIColor(hex: "#9199A1") ?? .systemGray
    static let secondColor = UIColor(hex: "#FFB049")
    
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}
