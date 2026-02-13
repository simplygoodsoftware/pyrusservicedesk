
struct NewAnnouncementsModel: Hashable {
    let logos: [UIImage]
    let title: String
    let subtitle: String
    
    static func == (lhs: NewAnnouncementsModel, rhs: NewAnnouncementsModel) -> Bool {
        return lhs.title == rhs.title && lhs.subtitle == rhs.subtitle
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(title)
        hasher.combine(subtitle)
    }
}
