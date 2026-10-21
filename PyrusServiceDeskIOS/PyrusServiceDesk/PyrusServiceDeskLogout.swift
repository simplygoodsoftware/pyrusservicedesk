import Foundation

extension PyrusServiceDesk {

    /// Разлогин: ставит команды setPushToken(token = nil) для всех user_id,
    /// отправляет их и затем чистит кэш
    ///
    /// Если отправка не удалась, команды переживают очистку кэша
    /// (см. `deleteCommandsForCacheClean`) и уйдут в следующей сессии.
    /// После успешной отправки команды тоже не удаляются локально:
    /// их удалит обычный синк по `commandsResult`, а до того повторная
    /// отправка дедуплицируется бэком по command_id.
    ///
    /// Completion вызывается на main после попытки отправки и запуска
    /// очистки кэша.
    @objc public static func logoutAllUsers(completion: (() -> Void)? = nil) {
        guard let clientId, !clientId.isEmpty else {
            EventsLogger.logEvent(.emptyClientId)
            DispatchQueue.main.async { completion?() }
            return
        }
        guard multichats else {
            PyrusLogger.shared.logEvent("logoutAllUsers is HelpySync-only, cache clean skipped")
            DispatchQueue.main.async { completion?() }
            return
        }

        repository.add(commands: makeLogoutCommands(clientId: clientId)) {
            sendLogoutCommands(completion: completion)
        }
    }
}

private extension PyrusServiceDesk {

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

    /// Отправка напрямую через PSDHelpySync, минуя SyncManager:
    /// его guard по isStarted не должен блокировать разлогин,
    /// а ответ (чаты старого пользователя) здесь не применяется.
    static func sendLogoutCommands(completion: (() -> Void)?) {
        PSDHelpySync.get(commands: repository.getCommands()) { response in
            if !response.complete {
                PyrusLogger.shared.logEvent(
                    "Logout sync failed, push token commands postponed to next session"
                )
            }
            cleanCache()
            DispatchQueue.main.async { completion?() }
        }
    }
}
