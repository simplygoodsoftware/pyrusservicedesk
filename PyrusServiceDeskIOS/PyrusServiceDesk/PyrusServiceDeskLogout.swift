import Foundation

extension PyrusServiceDesk {

    /// Разлогин: ставит команды setPushToken(token = nil) для всех user_id,
    /// запускает их отправку fire-and-forget и чистит кэш. Кэш чистится
    /// всегда и сразу, не дожидаясь сети, — отдельный `cleanCache()`
    /// хосту не нужен.
    ///
    /// По спеке HelpySync («Доработки на мобилках», п. 7) бэкенд по этим
    /// командам удаляет пуш-токены пользователей.
    ///
    /// Доставка не гарантируется в этой сессии: команды переживают очистку
    /// кэша (см. `deleteCommandsForCacheClean`) и уйдут в следующей —
    /// бэк дедуплицирует по command_id. Удалит их обычный синк
    /// по `commandsResult`.
    ///
    /// Completion вызывается на main после постановки команд и очистки
    /// кэша; повторный вход следует начинать после него.
    @objc public static func logoutAllUsers(completion: (() -> Void)? = nil) {
        guard let clientId, !clientId.isEmpty else {
            EventsLogger.logEvent(.emptyClientId)
            finishLogout(completion: completion)
            return
        }
        guard multichats else {
            finishLogout(completion: completion)
            return
        }

        repository.add(commands: makeLogoutCommands(clientId: clientId)) {
            // Отправка до cleanCache: запрос собирается синхронно
            // из ещё нетронутого состояния SDK.
            PSDHelpySync.sendDetached(commands: repository.getCommands())
            finishLogout(completion: completion)
        }
    }
}

private extension PyrusServiceDesk {

    static func finishLogout(completion: (() -> Void)?) {
        cleanCache()
        DispatchQueue.main.async { completion?() }
    }

    static func makeLogoutCommands(clientId: String) -> [TicketCommand] {
        var targets: [(appId: String, userId: String?)] = [(clientId, customUserId)]
        for user in additionalUsers {
            targets.append((user.clientId, user.userId))
        }
        // Основной пользователь может дублироваться в additionalUsers.
        var seenPairs = Set<String>()
        let uniqueTargets = targets.filter {
            seenPairs.insert("\($0.appId)|\($0.userId ?? "")").inserted
        }

        return uniqueTargets.map { target in
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
    }
}
