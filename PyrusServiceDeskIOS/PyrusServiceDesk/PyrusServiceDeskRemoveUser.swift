import Foundation

extension PyrusServiceDesk {

    /// Выход из пользователя по инициативе клиента (свайп на «Мои команды»).
    ///
    /// Чистит состояние SDK (пользователи, клиенты, чаты, объявления и их кэш),
    /// ставит отложенный разлогин пушей — setPushToken с token = nil,
    /// как в `logoutAllUsers()` — и дергает `deniedAccessCallback`,
    /// чтобы приложение удалило пользователя у себя готовой цепочкой
    /// denied access. Последнего пользователя не удаляет: для этого
    /// есть полный разлогин. Ожидает вызова с главного потока.
    @objc public static func removeUser(userId: String) {
        guard !userId.isEmpty else { return }

        let appId: String
        if userId == customUserId {
            guard let clientId, !clientId.isEmpty, !additionalUsers.isEmpty else { return }
            appId = clientId
        } else if let user = additionalUsers.first(where: { $0.userId == userId }) {
            appId = user.clientId
        } else {
            return
        }

        let command = TicketCommand(
            commandId: UUID().uuidString,
            type: .setPushToken,
            appId: appId,
            userId: userId,
            params: TicketCommandParams(
                ticketId: nil,
                appId: appId,
                userId: userId,
                token: nil,
                type: DeviceType.ios.legacyName
            )
        )

        if userId == customUserId {
            // Повторяет повышение пользователя из denyAccesses и PersonInfoRepository.deleteUsers.
            let nextUser = additionalUsers.removeLast()
            customUserId = nextUser.userId
            clientId = nextUser.clientId
            clientName = nextUser.clientName
            userName = nextUser.userName
        } else {
            additionalUsers.removeAll(where: { $0.userId == userId })
        }
        if currentUserId == userId {
            currentUserId = customUserId
        }

        removeOrphanedClients()
        removeChats(of: userId)

        repository.add(commands: [command]) {
            syncManager.syncImmediately()
        }

        deniedAccessCallback?.deleteUsers(userIds: [userId])
        NotificationCenter.default.post(name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
        NotificationCenter.default.post(name: .createMenuNotification, object: nil)
    }
}

private extension PyrusServiceDesk {

    /// Убирает клиентов, у которых не осталось ни одного пользователя
    /// (критерии — как в SyncManager.checkAccesses), вместе с их объявлениями.
    static func removeOrphanedClients() {
        var removedIds = [String]()
        clients.removeAll { client in
            if client.clientId == clientId { return false }
            if anonimClients.contains(client.clientId) { return false }
            if additionalUsers.contains(where: { $0.clientId == client.clientId }) { return false }
            removedIds.append(client.clientId)
            return true
        }
        guard !removedIds.isEmpty else { return }
        if let currentClientId, removedIds.contains(currentClientId) {
            PyrusServiceDesk.currentClientId = nil
        }
        syncManager.chatsDataService.saveClientModels(with: clients)
        removeAnnouncements(of: removedIds)
    }

    static func removeChats(of userId: String) {
        guard chats.contains(where: { $0.userId == userId }) else { return }
        chats.removeAll(where: { $0.userId == userId })
        // saveChatModels удаляет из CoreData всё, чего нет в переданном списке.
        syncManager.chatsDataService.saveChatModels(with: chats, completion: nil)
    }

    static func removeAnnouncements(of appIds: [String]) {
        let removedAnnouncementIds = announcements
            .filter { appIds.contains($0.appId) }
            .compactMap { $0.id }
        AnnouncementsReadStorage.shared.clear(clientIds: appIds)
        guard !removedAnnouncementIds.isEmpty else { return }
        announcements.removeAll(where: { appIds.contains($0.appId) })
        syncManager.chatsDataService.deleteAnnouncements(ids: removedAnnouncementIds)
    }
}
