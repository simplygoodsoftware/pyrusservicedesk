
struct PSDAnnouncement: Hashable {
    let id: String
    let text: String?
    let date: Date
    var isRead: Bool
    var attachments: [PSDAnnouncementAttachment]
    let appId: String
}

struct PSDAnnouncementAttachment: Hashable {
    let id: String
    let name: String?
    let size: Int
    let width: Int
    let height: Int
    let media: Bool
}
