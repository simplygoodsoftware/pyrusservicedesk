import Foundation

enum PSDAnnouncementCellType {
    case announcement
    case announcementsRead
}

/// Элемент diffable-снапшота.
/// Идентичность (==/hash) — только стабильный id, чтобы изменение контента
/// не превращалось в delete+insert с миганием и прыжками скролла.
/// Изменения контента доставляются через reconfigureItems (см. hasSameContent).
struct AnnouncementsViewModel: Hashable {
    let data: AnyHashable
    let type: PSDAnnouncementCellType

    var itemIdentity: String {
        switch type {
        case .announcement:
            let id = (data as? PSDAnnouncementCellModel)?.announcement.id ?? ""
            return "announcement_" + id
        case .announcementsRead:
            return "read_marker"
        }
    }

    static func == (lhs: AnnouncementsViewModel, rhs: AnnouncementsViewModel) -> Bool {
        lhs.itemIdentity == rhs.itemIdentity
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(itemIdentity)
    }

    /// Полное сравнение контента — для вычисления айтемов, требующих reconfigure.
    func hasSameContent(as other: AnnouncementsViewModel) -> Bool {
        type == other.type && data == other.data
    }
}
