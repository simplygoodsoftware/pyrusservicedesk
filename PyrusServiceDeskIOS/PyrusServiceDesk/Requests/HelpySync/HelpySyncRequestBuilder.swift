import Foundation

/// Собирает `HelpySyncRequest` из текущего состояния SDK.
///
/// Ожидает вызова с главного потока (синк запускается с main через
/// `ThrottleController`): состояние пользователей и кэш чатов
/// мутируются на main.
enum HelpySyncRequestBuilder {

    private enum Constants {
        /// Значение поля `version` — то же, что и в остальных запросах SDK
        /// (см. `URLRequest.addStaticKeys`).
        static let protocolVersion = 2
        static let defaultLocale = "en"
    }

    private enum LogConstants {
        /// Сколько пар ticket_id:last_note_id максимум печатать в лог.
        static let maxLoggedEntries = 30
        /// Максимальная длина base64 блоба в логе.
        static let maxLoggedBlobLength = 200
    }

    /// - Parameters:
    ///   - commands: несинхронизированные команды из репозитория.
    ///   - cachedChats: снапшот кэша чатов; из него собирается блоб `tickets`.
    ///     Тот же снапшот следует передать в `HelpySyncChatsMapper`, чтобы
    ///     состояние, о котором мы сообщили серверу, совпадало с тем,
    ///     относительно которого применяется дельта.
    static func makeRequest(
        commands: [TicketCommand],
        cachedChats: [PSDChat],
        maxClosedTicketsCount: Int = 50
    ) -> HelpySyncRequest {
        let request = HelpySyncRequest(
            instanceId: PyrusServiceDesk.userId,
            deviceId: PSDDeviceIdentifier.deviceId,
            version: Constants.protocolVersion,
            locale: Locale.current.languageCode ?? Constants.defaultLocale,
            apiSign: PyrusServiceDesk.apiSign(),
            authorId: PyrusServiceDesk.authorId,
            authorName: PyrusServiceDesk.authorName,
            users: makeUsers(),
            tickets: makeTicketsBlob(from: cachedChats),
            commands: commands.isEmpty ? nil : commands,
            maxClosedTicketsCount: maxClosedTicketsCount,
            announcementCheckpoints: makeAnnouncementCheckpoints()
        )
        logRequestSummary(request)
        return request
    }
}

private extension HelpySyncRequestBuilder {

    // MARK: Users

    /// Собирает массив users: основной пользователь, дополнительные
    /// и анонимные клиенты. Корневые app_id/user_id по спеке из запроса убраны.
    static func makeUsers() -> [HelpySyncUserData] {
        var users = [HelpySyncUserData]()

        if let clientId = PyrusServiceDesk.clientId, !clientId.isEmpty {
            users.append(makeUserData(appId: clientId, userId: PyrusServiceDesk.customUserId))
        }

        for user in PyrusServiceDesk.additionalUsers {
            users.append(makeUserData(appId: user.clientId, userId: user.userId))
        }

        // Анонимные клиенты: только app_id, user_id = nil
        // (раньше отправлялись в additional_users с last_note_id = 0).
        for clientId in PyrusServiceDesk.anonimClients {
            users.append(HelpySyncUserData(appId: clientId, userId: nil))
        }

        return deduplicated(users)
    }

    static func makeUserData(appId: String, userId: String?) -> HelpySyncUserData {
        return HelpySyncUserData(appId: appId, userId: parseUserId(userId))
    }

    /// По спеке user_id — long?. Внутри SDK идентификаторы хранятся строками,
    /// поэтому конвертируем; нечисловое непустое значение — нарушение контракта,
    /// логируем и трактуем как анонимную авторизацию.
    static func parseUserId(_ userId: String?) -> Int64? {
        guard let userId, !userId.isEmpty else {
            return nil
        }
        guard let numericId = Int64(userId) else {
            PyrusLogger.shared.logEvent("HelpySync: non-numeric user_id, sending as anonymous")
            return nil
        }
        return numericId
    }

    static func deduplicated(_ users: [HelpySyncUserData]) -> [HelpySyncUserData] {
        var seen = Set<String>()
        return users.filter { user in
            let userIdPart = user.userId.map { String($0) } ?? ""
            let key = "\(user.appId)|\(userIdPart)"
            return seen.insert(key).inserted
        }
    }

    // MARK: Tickets blob

    /// Состояние кэша тикетов: для каждого серверного тикета — id последнего
    /// комментария в кэше. Локальные тикеты (отрицательные id) не отправляются.
    /// Тикеты нового пользователя из extra_users в кэше отсутствуют, поэтому
    /// в блоб не попадают — сервер вернёт по ним все комментарии
    /// (эквивалент прежнего lastNoteId = 0 из спеки).
    static func makeTicketsBlob(from cachedChats: [PSDChat]) -> String? {
        // Прежнее поведение needShowLoading (last_note_id = 0):
        // не сообщаем серверу о кэше, чтобы получить всё заново.
        guard !PyrusServiceDesk.needShowLoading else {
            PyrusLogger.shared.logEvent("HelpySync request: needShowLoading, tickets blob skipped")
            return nil
        }

        var entriesByTicketId = [Int64: Int64]()
        for chat in cachedChats {
            guard let chatId = chat.chatId, chatId > 0 else {
                continue
            }
            let ticketId = Int64(chatId)
            let lastNoteId = lastCachedNoteId(of: chat)
            // На случай дублей в кэше берём максимальный известный id.
            entriesByTicketId[ticketId] = max(entriesByTicketId[ticketId] ?? 0, lastNoteId)
        }

        let entries = entriesByTicketId
            .map { TicketCacheEntry(ticketId: $0.key, lastNoteId: $0.value) }
            .sorted { $0.ticketId < $1.ticketId }

        logBlobEntries(entries)
        let blob = TicketsCacheBlobEncoder.encode(entries)
        logBlob(blob)
        return blob
    }

    // MARK: Diagnostics

    /// Диагностика: какие пары ticket_id → last_note_id уходят в блоб.
    /// Пустой блоб на первом синке (пустой кэш) — норма: сервер вернёт всё,
    /// дельта начинает работать со второго синка.
    static func logBlobEntries(_ entries: [TicketCacheEntry]) {
        guard !entries.isEmpty else {
            print("HelpySync request: tickets blob empty (full history will be returned)")
            return
        }
        let zeroCount = entries.filter { $0.lastNoteId == 0 }.count
        let pairs = entries
            .prefix(LogConstants.maxLoggedEntries)
            .map { "\($0.ticketId):\($0.lastNoteId)" }
            .joined(separator: ", ")
        let suffix = entries.count > LogConstants.maxLoggedEntries ? ", …" : ""
        print(
            "HelpySync request: tickets blob \(entries.count) entries"
            + " (with lastNoteId=0: \(zeroCount)) [\(pairs)\(suffix)]"
        )
    }

    /// Base64 блоба (усечённый) — для сверки с телом запроса в снифере.
    static func logBlob(_ blob: String?) {
        guard let blob else {
            return
        }
        let truncated = blob.count > LogConstants.maxLoggedBlobLength
        let prefix = blob.prefix(LogConstants.maxLoggedBlobLength)
        print(
            "HelpySync request: tickets base64 \(blob.count) chars: \(prefix)\(truncated ? "…" : "")"
        )
    }

    /// Сводка по составу запроса: пользователи, команды, чекпоинты.
    static func logRequestSummary(_ request: HelpySyncRequest) {
        let userIds = request.users
            .prefix(LogConstants.maxLoggedEntries)
            .map { user in user.userId.map { String($0) } ?? "anonymous" }
            .joined(separator: ", ")
        print(
            "HelpySync request: users: \(request.users.count) [\(userIds)],"
            + " commands: \(request.commands?.count ?? 0),"
            + " announcement checkpoints: \(request.announcementCheckpoints?.count ?? 0),"
            + " author_id set: \(request.authorId?.isEmpty == false)"
        )
    }

    /// Максимальный числовой id комментария тикета в кэше.
    /// Локальные (ещё не отправленные) сообщения имеют нечисловые id
    /// и в подсчёт не попадают.
    static func lastCachedNoteId(of chat: PSDChat) -> Int64 {
        var lastNoteId: Int64 = 0
        for message in chat.messages {
            if let messageId = Int64(message.messageId) {
                lastNoteId = max(lastNoteId, messageId)
            }
        }
        if let lastCommentId = chat.lastComment.flatMap({ Int64($0.messageId) }) {
            lastNoteId = max(lastNoteId, lastCommentId)
        }
        return lastNoteId
    }

    // MARK: Announcements checkpoints

    /// Чекпоинты объявлений — логика повторяет прежнюю из PSDGetChats:
    /// по чекпоинту на каждого известного клиента, а до первого ответа
    /// сервера (клиентов ещё нет) — пустой чекпоинт по основному app_id.
    static func makeAnnouncementCheckpoints() -> [HelpySyncAnnouncementCheckpoint]? {
        if !PyrusServiceDesk.clients.isEmpty {
            return PyrusServiceDesk.clients.map { client in
                HelpySyncAnnouncementCheckpoint(
                    appId: client.clientId,
                    lastAnnouncementId: client.lasAnnoncementId,
                    lastAnnouncementChangeDatetime: client.lasAnnouncementUpdateDate
                )
            }
        }

        guard let clientId = PyrusServiceDesk.clientId else {
            return nil
        }
        return [
            HelpySyncAnnouncementCheckpoint(
                appId: clientId,
                lastAnnouncementId: nil,
                lastAnnouncementChangeDatetime: nil
            )
        ]
    }
}
