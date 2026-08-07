import Foundation

// MARK: - Request

/// Тело запроса HelpySync (замена GetTickets для приложения Помощник).
///
/// По спеке из корня запроса убраны `app_id`, `user_id`, `last_note_id`
/// и `security_key` — данные о пользователях передаются массивом `users`.
/// `sort_type` сознательно не отправляется: сортировка тикетов полностью
/// перенесена на клиент. `max_closed_tickets_count` не отправляется —
/// используется серверный дефолт (см. `HelpySyncDefaults.maxClosedTicketsCount`).
struct HelpySyncRequest: Encodable {
    /// id установки приложения (меняется при переустановке). Максимум 36 байт.
    let instanceId: String
    /// id устройства (не меняется при переустановке).
    let deviceId: String
    let version: Int
    let locale: String
    let apiSign: String
    /// Хэш номера телефона — определяет автора комментариев
    /// и прочитанность конкретного пользователя в тикете.
    let authorId: String?
    let authorName: String?
    /// Пользователи, по которым необходимо получить тикеты.
    let users: [HelpySyncUserData]
    /// base64-упаковка состояния кэша тикетов
    /// (см. `TicketsCacheBlobEncoder`). nil — кэш пуст, сервер вернёт всё.
    let tickets: String?
    let commands: [TicketCommand]?
    let maxClosedTicketsCount: Int?
    let announcementCheckpoints: [HelpySyncAnnouncementCheckpoint]?

    enum CodingKeys: String, CodingKey {
        case instanceId = "instance_id"
        case deviceId = "device_id"
        case version
        case locale
        case apiSign = "api_sign"
        case authorId = "author_id"
        case authorName = "author_name"
        case users
        case tickets
        case commands
        case maxClosedTicketsCount = "max_closed_tickets_count"
        case announcementCheckpoints = "helpy_announcement_feed_checkpoints"
    }
}

/// Значения, которые сервер использует по умолчанию.
enum HelpySyncDefaults {
    /// Максимальное число возвращаемых закрытых тикетов на одного user_id,
    /// если клиент не прислал своё значение.
    static let maxClosedTicketsCount = 50
}

/// Данные о пользователе, по которому нужно получить тикеты.
struct HelpySyncUserData: Encodable {
    /// Идентификатор расширения (привязка к конкретной форме в Pyrus).
    let appId: String
    /// id пользователя (ресторана). По спеке — long?, nil при анонимной авторизации.
    let userId: Int64?

    enum CodingKeys: String, CodingKey {
        case appId = "app_id"
        case userId = "user_id"
    }
}

/// Состояние кэша объявлений по конкретному app_id (для вычисления дифа).
struct HelpySyncAnnouncementCheckpoint: Encodable {
    let appId: String
    /// id последнего объявления на клиенте.
    let lastAnnouncementId: String?
    /// Дата последнего полученного изменения на клиенте.
    let lastAnnouncementChangeDatetime: String?

    enum CodingKeys: String, CodingKey {
        case appId = "app_id"
        case lastAnnouncementId = "last_helpy_announcement_id"
        case lastAnnouncementChangeDatetime = "last_helpy_announcement_change_datetime"
    }
}

// MARK: - Decoding helpers

/// Обёртка для поэлементного (lossy) декодирования массивов:
/// битый элемент даёт nil и пропускается, а не валит весь ответ.
struct FailableDecodable<Value: Decodable>: Decodable {
    let value: Value?

    init(from decoder: Decoder) throws {
        value = try? Value(from: decoder)
    }
}

/// Гибкое декодирование числовых полей: контракт объявляет long,
/// но сервер исторически мог присылать числа строками.
enum FlexibleDecoding {

    static func intIfPresent<Key: CodingKey>(
        _ container: KeyedDecodingContainer<Key>,
        forKey key: Key
    ) -> Int? {
        if let value = try? container.decode(Int.self, forKey: key) {
            return value
        }
        if let string = try? container.decode(String.self, forKey: key) {
            return Int(string)
        }
        return nil
    }

    static func int64IfPresent<Key: CodingKey>(
        _ container: KeyedDecodingContainer<Key>,
        forKey key: Key
    ) -> Int64? {
        if let value = try? container.decode(Int64.self, forKey: key) {
            return value
        }
        if let string = try? container.decode(String.self, forKey: key) {
            return Int64(string)
        }
        return nil
    }

    /// Строковое поле, которое сервер может прислать и числом
    /// (реальный пример из ответа: `"avatar_id": 119728`).
    /// Повторяет поведение старого `stringOfKey`, принимавшего любой тип.
    static func stringIfPresent<Key: CodingKey>(
        _ container: KeyedDecodingContainer<Key>,
        forKey key: Key
    ) -> String? {
        if let value = try? container.decode(String.self, forKey: key) {
            return value
        }
        if let value = try? container.decode(Int64.self, forKey: key) {
            return String(value)
        }
        return nil
    }

    /// Декодирует lossy-массив и логирует количество отброшенных элементов.
    static func lossyArrayIfPresent<Key: CodingKey, Element: Decodable>(
        _ container: KeyedDecodingContainer<Key>,
        forKey key: Key,
        elementName: String
    ) -> [Element]? {
        guard
            let failableItems = try? container.decodeIfPresent(
                [FailableDecodable<Element>].self,
                forKey: key
            )
        else {
            return nil
        }
        let items = failableItems.compactMap { $0.value }
        let droppedCount = failableItems.count - items.count
        if droppedCount > 0 {
            PyrusLogger.shared.logEvent(
                "HelpySync: dropped \(droppedCount) undecodable \(elementName)"
            )
        }
        return items
    }
}

// MARK: - Response

/// Часть ответа HelpySync, которая изменилась относительно GetTickets
/// и декодируется через Codable.
///
/// Блок `applications` схемы не менял и продолжает разбираться существующей
/// логикой (`PSDGetChats.generateClients` / `generateAnnouncements`),
/// чтобы не дублировать и не ломать проверенный код.
struct HelpySyncResponse: Decodable {
    let tickets: [HelpySyncTicket]?
    let commandsResult: [TicketCommandResult]?
    let authorAccessDenied: [String]?
    /// true, если бэкенд вернул не все закрытые тикеты
    /// (сработало ограничение maxClosedTicketsCount).
    let hasMoreClosedTickets: Bool?

    enum CodingKeys: String, CodingKey {
        case tickets
        case commandsResult = "commands_result"
        case authorAccessDenied = "author_access_denied"
        case hasMoreClosedTickets = "has_more_closed_tickets"
        // В спеке поле указано в camelCase, хотя остальной контракт — snake_case.
        // Декодируем оба варианта, чтобы не зависеть от финального решения бэка.
        case hasMoreClosedTicketsCamel = "hasMoreClosedTickets"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        tickets = FlexibleDecoding.lossyArrayIfPresent(
            container,
            forKey: .tickets,
            elementName: "tickets"
        )
        commandsResult = FlexibleDecoding.lossyArrayIfPresent(
            container,
            forKey: .commandsResult,
            elementName: "commands_result"
        )
        authorAccessDenied = try? container.decodeIfPresent([String].self, forKey: .authorAccessDenied)
        let snakeCaseValue = try? container.decodeIfPresent(Bool.self, forKey: .hasMoreClosedTickets)
        let camelCaseValue = try? container.decodeIfPresent(Bool.self, forKey: .hasMoreClosedTicketsCamel)
        hasMoreClosedTickets = snakeCaseValue ?? camelCaseValue
    }
}

/// Заголовок тикета из ответа HelpySync.
///
/// В отличие от GetTickets сервер больше не присылает `last_comment`
/// и `is_read` — и то и другое вычисляется на клиенте
/// (см. `HelpySyncChatsMapper`).
///
/// Декодирование намеренно максимально живучее: обязателен только
/// `ticket_id`; проблема в любом другом поле не валит ни тикет, ни ответ.
/// Даты приходят строками и парсятся в маппере — как в старом разборе
/// GetTickets, который никогда не падал на формате даты.
struct HelpySyncTicket: Decodable {
    let ticketId: Int
    let subject: String?
    /// Сырая строка created_at; парсится в маппере с фолбэком.
    let createdAt: String?
    let isActive: Bool?
    /// Только комментарии с id больше переданного в запросе lastNoteId
    /// (или все — если тикета не было в кэше клиента).
    let comments: [HelpySyncComment]?
    let showRating: Bool?
    let showRatingText: String?
    /// id последнего прочитанного комментария для текущего пользователя.
    let lastReadCommentId: Int?
    /// Всегда берётся сервером из поля формы; nil при анонимной авторизации.
    let userId: Int64?
    /// Приходит только для анонимных тикетов, не привязанных ни к какому user_id.
    let appId: String?

    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case subject
        case createdAt = "created_at"
        case isActive = "is_active"
        case comments
        case showRating = "show_rating"
        case showRatingText = "show_rating_text"
        case lastReadCommentId = "last_read_comment_id"
        case userId = "user_id"
        case appId = "app_id"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        guard let ticketId = FlexibleDecoding.intIfPresent(container, forKey: .ticketId) else {
            throw DecodingError.keyNotFound(
                CodingKeys.ticketId,
                DecodingError.Context(
                    codingPath: container.codingPath,
                    debugDescription: "ticket_id is missing or not numeric"
                )
            )
        }
        self.ticketId = ticketId
        subject = try? container.decodeIfPresent(String.self, forKey: .subject)
        createdAt = try? container.decodeIfPresent(String.self, forKey: .createdAt)
        isActive = try? container.decodeIfPresent(Bool.self, forKey: .isActive)
        comments = FlexibleDecoding.lossyArrayIfPresent(
            container,
            forKey: .comments,
            elementName: "comments"
        )
        showRating = try? container.decodeIfPresent(Bool.self, forKey: .showRating)
        showRatingText = try? container.decodeIfPresent(String.self, forKey: .showRatingText)
        lastReadCommentId = FlexibleDecoding.intIfPresent(container, forKey: .lastReadCommentId)
        userId = FlexibleDecoding.int64IfPresent(container, forKey: .userId)
        appId = try? container.decodeIfPresent(String.self, forKey: .appId)
    }
}

/// Комментарий тикета из ответа HelpySync (TicketCommentData из спеки).
/// Обязателен только `comment_id` — остальные поля best-effort.
struct HelpySyncComment: Decodable {
    let commentId: Int
    let body: String?
    let attachments: [HelpySyncCommentAttachment]?
    /// Сырая строка created_at; парсится в маппере с фолбэком.
    let createdAt: String?
    /// По спеке: false, если комментарий от поддержки (отправлен не из приложения).
    let isInbound: Bool?
    /// id команды на создание комментария (guid, создаётся на клиенте).
    let clientId: String?
    let author: HelpySyncCommentAuthor?
    let isSystem: Bool?
    /// Поля нет в спеке TicketCommentData, но старый контракт его присылал
    /// для сообщений с оценкой — декодируем опционально, чтобы не потерять
    /// существующее поведение отрисовки рейтинга.
    let rating: Int?

    enum CodingKeys: String, CodingKey {
        case commentId = "comment_id"
        case body
        case attachments
        case createdAt = "created_at"
        case isInbound = "is_inbound"
        case clientId = "client_id"
        case author
        case isSystem = "is_system"
        case rating
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        guard let commentId = FlexibleDecoding.intIfPresent(container, forKey: .commentId) else {
            throw DecodingError.keyNotFound(
                CodingKeys.commentId,
                DecodingError.Context(
                    codingPath: container.codingPath,
                    debugDescription: "comment_id is missing or not numeric"
                )
            )
        }
        self.commentId = commentId
        body = try? container.decodeIfPresent(String.self, forKey: .body)
        attachments = FlexibleDecoding.lossyArrayIfPresent(
            container,
            forKey: .attachments,
            elementName: "attachments"
        )
        createdAt = try? container.decodeIfPresent(String.self, forKey: .createdAt)
        isInbound = try? container.decodeIfPresent(Bool.self, forKey: .isInbound)
        clientId = try? container.decodeIfPresent(String.self, forKey: .clientId)
        author = try? container.decodeIfPresent(HelpySyncCommentAuthor.self, forKey: .author)
        isSystem = try? container.decodeIfPresent(Bool.self, forKey: .isSystem)
        rating = FlexibleDecoding.intIfPresent(container, forKey: .rating)
    }
}

/// Вложение комментария (TicketCommentAttachment из спеки).
struct HelpySyncCommentAttachment: Decodable {
    let id: Int
    let name: String?
    let size: Int?

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case size
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        guard let id = FlexibleDecoding.intIfPresent(container, forKey: .id) else {
            throw DecodingError.keyNotFound(
                CodingKeys.id,
                DecodingError.Context(
                    codingPath: container.codingPath,
                    debugDescription: "attachment id is missing or not numeric"
                )
            )
        }
        self.id = id
        name = try? container.decodeIfPresent(String.self, forKey: .name)
        size = FlexibleDecoding.intIfPresent(container, forKey: .size)
    }
}

/// Автор комментария (AuthorData из спеки).
struct HelpySyncCommentAuthor: Decodable {
    /// nil, если комментарий создан не на стороне приложения.
    let authorId: String?
    let name: String?
    /// В спеке AuthorData поля нет, но сервер присылает avatar_id
    /// для операторов поддержки — причём ЧИСЛОМ (например, 119728),
    /// поэтому декодируем гибко и приводим к строке
    /// (дальше он используется как строковый imagePath).
    let avatarId: String?

    enum CodingKeys: String, CodingKey {
        case authorId = "author_id"
        case name
        case avatarId = "avatar_id"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        authorId = FlexibleDecoding.stringIfPresent(container, forKey: .authorId)
        name = FlexibleDecoding.stringIfPresent(container, forKey: .name)
        avatarId = FlexibleDecoding.stringIfPresent(container, forKey: .avatarId)
    }
}
