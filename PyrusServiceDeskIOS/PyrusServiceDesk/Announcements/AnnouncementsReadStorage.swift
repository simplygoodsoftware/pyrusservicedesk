import Foundation

/// Локальное состояние прочитанности объявлений.
///
/// Источники правды:
/// - **Серверная прочитанность** — флаги `PSDAnnouncement.isRead`.
///   Они парсятся из фида, переживают дельта-синки и персистятся
///   в Core Data. `client.lasAnnoncementReadId` НЕ используется:
///   дельта-синк без изменений затирает его nil'ом
///   (`generateClients`: `announcementsInfo?.inboxItem.lastReadMessageId`).
/// - **Локальная прочитанность** — указатель в UserDefaults, двигается
///   при открытии экрана объявлений. Нужен, потому что серверные флаги
///   обновятся только после того, как сервер обработает команду
///   `.readAnnouncemnts`, а плашка должна погаснуть сразу.
///
/// Непрочитанным считается объявление с `isRead == false`, которое
/// новее локального указателя. Локальный указатель только уменьшает
/// количество непрочитанного и двигается только вперёд.
///
/// Все методы вызывать только на main thread. Чтение не имеет
/// побочных эффектов; записи — только `markRead` и `clear`.
final class AnnouncementsReadStorage {

    static let shared = AnnouncementsReadStorage()

    // MARK: - Constants

    private enum Constants {
        static let storageKey = "PSDAnnouncementsLocalReadIds"
    }

    // MARK: - State

    private let defaults: UserDefaults

    /// Локально прочитанный lastReadId по каждому клиенту.
    private var localReadIds: [String: String]

    // MARK: - Init

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        localReadIds = defaults.dictionary(forKey: Constants.storageKey) as? [String: String] ?? [:]
    }

    // MARK: - Запись

    /// Помечает объявления клиента прочитанными до указанного id включительно.
    /// Указатель двигается только вперёд.
    func markRead(clientId: String, lastReadId: String) {
        dispatchPrecondition(condition: .onQueue(.main))
        guard orderIndex(of: lastReadId, clientId: clientId)
                >= orderIndex(of: localReadIds[clientId], clientId: clientId)
        else { return }
        localReadIds[clientId] = lastReadId
        defaults.set(localReadIds, forKey: Constants.storageKey)
    }

    /// Сбрасывает локальные пометки.
    /// Вызывать там же, где чистится кэш объявлений (DBAnnouncement),
    /// иначе указатели будут ссылаться на чужие/удалённые объявления.
    func clear() {
        dispatchPrecondition(condition: .onQueue(.main))
        localReadIds = [:]
        defaults.removeObject(forKey: Constants.storageKey)
    }

    // MARK: - Чтение

    /// Граница «прочитано/непрочитано» для разделителя на экране
    /// объявлений: последнее прочитанное по серверу (isRead) или
    /// локальный указатель — что дальше по ленте.
    func effectiveLastReadId(for clientId: String) -> String? {
        dispatchPrecondition(condition: .onQueue(.main))

        let serverId = serverLastReadId(for: clientId)
        let localId = localReadIds[clientId]

        return orderIndex(of: localId, clientId: clientId)
             >= orderIndex(of: serverId, clientId: clientId)
            ? localId ?? serverId
            : serverId
    }

    /// Количество непрочитанных объявлений клиента:
    /// `isRead == false` и новее локального указателя.
    /// `client.announcementsUnreadCount` из эха синка не используется.
    func unreadCount(for clientId: String) -> Int {
        dispatchPrecondition(condition: .onQueue(.main))

        let localIndex = orderIndex(of: localReadIds[clientId], clientId: clientId)
        return PyrusServiceDesk.announcements
            .filter { $0.appId == clientId && !$0.isRead && $0.orderIndex > localIndex }
            .count
    }

    /// Есть ли непрочитанные объявления хотя бы у одного клиента.
    func hasUnread() -> Bool {
        PyrusServiceDesk.clients.contains { unreadCount(for: $0.clientId) > 0 }
    }

    // MARK: - Private

    /// Серверный указатель прочитанности: самое свежее объявление
    /// клиента с isRead == true.
    private func serverLastReadId(for clientId: String) -> String? {
        PyrusServiceDesk.announcements
            .filter { $0.appId == clientId && $0.isRead }
            .max { $0.orderIndex < $1.orderIndex }?
            .id
    }

    /// orderIndex объявления; -1 для nil и неизвестных id,
    /// чтобы сравнения «дальше/ближе» работали без опционалов.
    private func orderIndex(of announcementId: String?, clientId: String) -> Int {
        guard let announcementId else { return -1 }
        return PyrusServiceDesk.announcements
            .first(where: { $0.appId == clientId && $0.id == announcementId })?
            .orderIndex ?? -1
    }
}
