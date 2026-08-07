import Foundation

extension PyrusServiceDesk {

    /// Разлогин пользователя.
    ///
    /// По спеке HelpySync («Доработки на мобилках», п. 7) при разлогине
    /// для всех user_id необходимо отправить команду setPushToken
    /// с token = nil — бэкенд удалит пуш-токены этих пользователей.
    ///
    /// Вызывать до `cleanCache()`, чтобы команды успели встать в очередь
    /// до очистки хранилища.
    @objc public static func logoutAllUsers() {
        guard let clientId, !clientId.isEmpty else {
            EventsLogger.logEvent(.emptyClientId)
            return
        }

        var logoutTargets: [(appId: String, userId: String?)] = [(clientId, customUserId)]
        for user in additionalUsers {
            logoutTargets.append((user.clientId, user.userId))
        }

        for target in logoutTargets {
            let params = TicketCommandParams(
                ticketId: nil,
                appId: target.appId,
                userId: target.userId,
                token: nil,
                type: DeviceType.ios.legacyName
            )
            let command = TicketCommand(
                commandId: UUID().uuidString,
                type: .setPushToken,
                appId: target.appId,
                userId: target.userId,
                params: params
            )
            // Синк не триггерим на каждую команду — один общий ниже.
            repository.add(command: command, needSync: false)
        }

        DispatchQueue.main.async {
            syncManager.syncGetTickets()
        }
    }
}
