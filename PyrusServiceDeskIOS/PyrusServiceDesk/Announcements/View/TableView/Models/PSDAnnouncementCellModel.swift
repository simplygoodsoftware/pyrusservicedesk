import Foundation

struct PSDAnnouncementCellModel: Hashable {
    let announcement: PSDAnnouncement
    let client: PSDClientInfo?
    /// Заранее построенная (на фоне, в презентере) атрибутированная строка объявления.
    /// Не участвует в сравнении — она детерминированно выводится из `announcement.content`.
    let attributedText: NSAttributedString?

    static func == (lhs: PSDAnnouncementCellModel, rhs: PSDAnnouncementCellModel) -> Bool {
        lhs.announcement == rhs.announcement
            && lhs.client?.clientId == rhs.client?.clientId
            && lhs.client?.clientName == rhs.client?.clientName
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(announcement.id)
    }
}
