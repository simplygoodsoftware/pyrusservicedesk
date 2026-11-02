import Foundation

extension Notification.Name {
    /// Пользователь удалён через PyrusServiceDesk.removeUser —
    /// экран чатов должен сбросить фильтр.
    static let userRemovedNotification = Notification.Name("PSDUserRemovedNotification")
}

extension PyrusServiceDesk {

    /// Выход из пользователя по инициативе клиента (свайп на «Мои команды»).
    ///
    /// Чистит состояние SDK (пользователи, клиенты, чаты, объявления и их кэш),
    /// ставит отложенный разлогин пушей — setPushToken с token = nil,
    /// как в `logoutAllUsers()` — и дергает `deniedAccessCallback`,
    /// чтобы приложение удалило пользователя у себя готовой цепочкой
    /// denied access. Последнего пользователя не удаляет: для этого
    /// есть полный разлогин. Можно вызывать с любого потока.
    @objc public static func removeUser(userId: String) {
        DispatchQueue.main.async {
            removeUserOnMain(userId: userId)
        }
    }

    /// Выбытие пользователя из состояния SDK; для основного — повышение
    /// следующего, как в PersonInfoRepository.deleteUsers(userIds:).
    /// Копии основного в additionalUsers (допустимы, см. logoutAllUsers)
    /// убираются перед повышением. Общий код ручного выхода и denied access
    /// (ChatsInteractor.denyAccesses). Ожидает вызова с главного потока.
    static func removeUserState(userId: String) {
        if userId == customUserId {
            additionalUsers.removeAll(where: { $0.userId == userId })
            guard let nextUser = additionalUsers.popLast() else { return }
            customUserId = nextUser.userId
            clientId = nextUser.clientId
            clientName = nextUser.clientName
            userName = nextUser.userName
        } else {
            additionalUsers.removeAll(where: { $0.userId == userId })
        }
        // currentUserId — фильтр списка чатов, nil = без фильтра.
        if currentUserId == userId {
            currentUserId = nil
        }
    }

    /// Чистка после выбытия пользователей: осиротевшие клиенты,
    /// их объявления (память, CoreData, указатели прочитанности
    /// в UserDefaults) и чаты выбывших пользователей.
    /// Вызывать после `removeUserState`. Ожидает вызова с главного потока.
    static func cleanupAfterUsersRemoval(removedUserIds: [String]) {
        guard !removedUserIds.isEmpty else { return }
        removeOrphanedClients()
        removeChats(of: removedUserIds)
    }
}

private extension PyrusServiceDesk {

    static func removeUserOnMain(userId: String) {
        guard !userId.isEmpty else { return }

        let appId: String
        if userId == customUserId {
            guard let clientId, !clientId.isEmpty,
                  additionalUsers.contains(where: { $0.userId != userId }) else { return }
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

        // Ответ синка, стартовавшего до удаления, содержит тикеты и клиента
        // этого пользователя — игнорируем его, как делает cleanCache.
        syncManager.invalidateInFlightSync()

        removeUserState(userId: userId)
        cleanupAfterUsersRemoval(removedUserIds: [userId])

        repository.add(commands: [command]) {
            syncManager.syncImmediately()
        }

        deniedAccessCallback?.deleteUsers(userIds: [userId])
        NotificationCenter.default.post(
            name: PyrusServiceDesk.chatsUpdateNotification,
            object: nil,
            userInfo: ["isFilter": false]
        )
        NotificationCenter.default.post(name: .userRemovedNotification, object: nil)
        NotificationCenter.default.post(name: .createMenuNotification, object: nil)
    }

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

    static func removeChats(of userIds: [String]) {
        guard chats.contains(where: { userIds.contains($0.userId ?? "") }) else { return }
        chats.removeAll(where: { userIds.contains($0.userId ?? "") })
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
