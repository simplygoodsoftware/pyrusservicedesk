import Foundation

extension PyrusServiceDesk {

    /// Разлогин пользователя.
    ///
    /// По спеке HelpySync («Доработки на мобилках», п. 7) при разлогине
    /// для всех user_id необходимо отправить команду setPushToken
    /// с token = nil — бэкенд удалит пуш-токены этих пользователей.
    ///
    /// Синк запускается сразу после постановки команд в очередь, без
    /// троттлинга. Команды setPushToken переживают `cleanCache()`
    /// (см. `PSDChatsDataService.deleteAllObjects`), поэтому при обрыве
    /// доставятся в следующей сессии — бэк дедуплицирует по command_id.
    @objc public static func logoutAllUsers() {
        guard let clientId, !clientId.isEmpty else {
            EventsLogger.logEvent(.emptyClientId)
            return
        }

        var logoutTargets: [(appId: String, userId: String?)] = [(clientId, customUserId)]
        for user in additionalUsers {
            logoutTargets.append((user.clientId, user.userId))
        }

        let commands = logoutTargets.map { target in
            TicketCommand(
                commandId: UUID().uuidString,
                type: .setPushToken,
                appId: target.appId,
                userId: target.userId,
                params: TicketCommandParams(
                    ticketId: nil,
                    appId: target.appId,
                    userId: target.userId,
                    token: nil,
                    type: DeviceType.ios.legacyName
                )
            )
        }

        repository.add(commands: commands) {
            syncManager.syncImmediately()
        }
    }
}
