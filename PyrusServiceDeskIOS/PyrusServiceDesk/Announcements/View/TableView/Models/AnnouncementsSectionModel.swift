import Foundation

/// Секции таблицы объявлений. Пока секция одна,
/// enum даёт стабильную идентичность без UUID-хаков.
enum AnnouncementsSection: Hashable {
    case main
}
