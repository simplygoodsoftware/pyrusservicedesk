enum PSDAnnouncementCellType {
    case announcement
    case announcementsRead
}

struct AnnouncementsViewModel: Hashable {
    static func == (lhs: AnnouncementsViewModel, rhs: AnnouncementsViewModel) -> Bool {
        return lhs.type == rhs.type && lhs.data == rhs.data
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(type)
        hasher.combine(data)
    }
    let data: AnyHashable
    let type: PSDAnnouncementCellType
}
