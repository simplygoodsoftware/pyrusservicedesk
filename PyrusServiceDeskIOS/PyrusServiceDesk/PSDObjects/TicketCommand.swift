import Foundation

enum TicketCommandType: Int {
    case createComment
    case readTicket
    case setPushToken
    case updateAccess
    case calcOperatorTime = 4
    case readAnnouncemnts = 5
}

/// Тип устройства для команды setPushToken.
/// По спеке HelpySync передаётся числом; исторически SDK хранит
/// и передаёт строку ("ios"/"android") — маппинг между форматами здесь.
enum DeviceType: Int {
    case ios = 0
    case android = 1

    init?(legacyName: String) {
        switch legacyName.lowercased() {
        case DeviceType.ios.legacyName:
            self = .ios
        case DeviceType.android.legacyName:
            self = .android
        default:
            return nil
        }
    }

    var legacyName: String {
        switch self {
        case .ios:
            return "ios"
        case .android:
            return "android"
        }
    }
}

/// Признак кодирования команд в формат запроса HelpySync.
///
/// Нужен, потому что один и тот же `TicketCommand` кодируется в трёх местах:
/// - персистентность (файл/кэш) — формат менять нельзя;
/// - легаси-запрос GetTickets — формат менять нельзя;
/// - новый запрос HelpySync — тип устройства по спеке передаётся как Int.
enum HelpySyncWireFormat {
    static let userInfoKey = CodingUserInfoKey(rawValue: "psd.helpySyncWireFormat")

    /// Энкодер для тела запроса HelpySync.
    static func makeEncoder() -> JSONEncoder {
        let encoder = JSONEncoder()
        if let userInfoKey {
            encoder.userInfo[userInfoKey] = true
        }
        return encoder
    }

    static func isEnabled(in encoder: Encoder) -> Bool {
        guard let userInfoKey else {
            return false
        }
        return encoder.userInfo[userInfoKey] as? Bool ?? false
    }
}

struct AttachmentData: Codable {
    let type: Int
    let name: String
    let guid: String?
}

class TicketCommandParams: Codable {
    var ticketId: Int?
    let appId: String?
    let requestNewTicket: Bool?
    let userId: String?
    var message: String?
    let attachments: [AttachmentData]?
    let token: String?
    let type: String?
    let messageId: Int?
    let rating: Int?
    let authorId: String?
    let hasAccess: Bool?
    let hasAdminAccess: Bool?
    let ratingComment: String?
    var date: Date? = nil
    var messageClientId: String? = nil
    let extraFields: [String: String]?
    let lastReadAnnouncementId: String?
    
    init(ticketId: Int? = nil, appId: String? = nil, requestNewTicket: Bool? = nil, userId: String? = nil, message: String? = nil, attachments: [AttachmentData]? = nil, authorId: String? = nil, token: String? = nil, type: String? = nil, messageId: Int? = nil, rating: Int? = nil, ratingComment: String? = nil, date: Date? = nil, messageClientId: String? = nil, hasAccess: Bool? = nil, hasAdminAccess: Bool? = nil, extraFields: [String: String]? = nil, lastReadAnnouncementId: String? = nil) {

        self.ticketId = ticketId
        self.appId = appId
        self.requestNewTicket = requestNewTicket
        self.userId = userId
        self.message = message
        self.attachments = attachments
        self.token = token
        self.type = type
        self.messageId = messageId
        self.rating = rating
        self.date = date ?? Date()
        self.messageClientId = messageClientId
        self.authorId = authorId
        self.hasAccess = hasAccess
        self.hasAdminAccess = hasAdminAccess
        self.ratingComment = ratingComment
        self.extraFields = extraFields
        self.lastReadAnnouncementId = lastReadAnnouncementId
    }
    
    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case appId = "app_id"
        case requestNewTicket = "request_new_ticket"
        case userId = "user_id"
        case message = "comment"
        case attachments = "attachments"
        case token = "token"
        case type = "type"
        case messageId = "comment_id"
        case rating = "rating"
        case authorId = "author_id"
        case hasAccess = "has_access"
        case hasAdminAccess = "has_admin_access"
        case ratingComment = "rating_comment"
        case extraFields = "extra_fields"
        case lastReadAnnouncementId = "last_read_announcement_id"
    }

    // MARK: Codable
    //
    // Кастомная реализация вместо синтезированной ради двух отличий wire-формата:
    // 1) поле `type` (тип устройства) по спеке HelpySync кодируется числом
    //    (DeviceType), в остальных местах — легаси-строкой;
    // 2) для setPushToken с token == nil кодируется явный null —
    //    по спеке null-токен означает удаление токена на бэке
    //    (так же делал старый PSDPushToken через NSNull).
    // `date` и `messageClientId` в кодирование не входят — как и раньше.

    required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        ticketId = try container.decodeIfPresent(Int.self, forKey: .ticketId)
        appId = try container.decodeIfPresent(String.self, forKey: .appId)
        requestNewTicket = try container.decodeIfPresent(Bool.self, forKey: .requestNewTicket)
        userId = try container.decodeIfPresent(String.self, forKey: .userId)
        message = try container.decodeIfPresent(String.self, forKey: .message)
        attachments = try container.decodeIfPresent([AttachmentData].self, forKey: .attachments)
        token = try container.decodeIfPresent(String.self, forKey: .token)
        // Тип устройства: принимаем и новый числовой формат,
        // и легаси-строку из ранее сохранённых команд.
        if let deviceTypeRawValue = try? container.decode(Int.self, forKey: .type) {
            type = DeviceType(rawValue: deviceTypeRawValue)?.legacyName
        } else {
            type = try container.decodeIfPresent(String.self, forKey: .type)
        }
        messageId = try container.decodeIfPresent(Int.self, forKey: .messageId)
        rating = try container.decodeIfPresent(Int.self, forKey: .rating)
        authorId = try container.decodeIfPresent(String.self, forKey: .authorId)
        hasAccess = try container.decodeIfPresent(Bool.self, forKey: .hasAccess)
        hasAdminAccess = try container.decodeIfPresent(Bool.self, forKey: .hasAdminAccess)
        ratingComment = try container.decodeIfPresent(String.self, forKey: .ratingComment)
        extraFields = try container.decodeIfPresent([String: String].self, forKey: .extraFields)
        lastReadAnnouncementId = try container.decodeIfPresent(String.self, forKey: .lastReadAnnouncementId)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(ticketId, forKey: .ticketId)
        try container.encodeIfPresent(appId, forKey: .appId)
        try container.encodeIfPresent(requestNewTicket, forKey: .requestNewTicket)
        try container.encodeIfPresent(userId, forKey: .userId)
        try container.encodeIfPresent(message, forKey: .message)
        try container.encodeIfPresent(attachments, forKey: .attachments)
        try encodeToken(to: &container)
        try encodeDeviceType(to: &container, encoder: encoder)
        try container.encodeIfPresent(messageId, forKey: .messageId)
        try container.encodeIfPresent(rating, forKey: .rating)
        try container.encodeIfPresent(authorId, forKey: .authorId)
        try container.encodeIfPresent(hasAccess, forKey: .hasAccess)
        try container.encodeIfPresent(hasAdminAccess, forKey: .hasAdminAccess)
        try container.encodeIfPresent(ratingComment, forKey: .ratingComment)
        try container.encodeIfPresent(extraFields, forKey: .extraFields)
        try container.encodeIfPresent(lastReadAnnouncementId, forKey: .lastReadAnnouncementId)
    }

    private func encodeToken(to container: inout KeyedEncodingContainer<CodingKeys>) throws {
        if let token {
            try container.encode(token, forKey: .token)
            return
        }
        // Наличие типа устройства означает команду setPushToken:
        // nil-токен для неё кодируем явным null (удаление токена на бэке).
        if type != nil {
            try container.encodeNil(forKey: .token)
        }
    }

    private func encodeDeviceType(
        to container: inout KeyedEncodingContainer<CodingKeys>,
        encoder: Encoder
    ) throws {
        guard let type else {
            return
        }
        if HelpySyncWireFormat.isEnabled(in: encoder), let deviceType = DeviceType(legacyName: type) {
            try container.encode(deviceType.rawValue, forKey: .type)
        } else {
            try container.encode(type, forKey: .type)
        }
    }
}

class TicketCommand: NSObject, Codable {
    let commandId: String
    let type: Int
    let appId: String?
    let userId: String?
    let params: TicketCommandParams

    init(commandId: String, type: TicketCommandType, appId: String?, userId: String?, params: TicketCommandParams) {
        self.commandId = commandId
        self.type = type.rawValue
        self.params = params
        self.appId = appId
        self.userId = userId
    }
    
    enum CodingKeys: String, CodingKey {
        case commandId = "command_id"
        case type = "type"
        case appId = "app_id"
        case userId = "user_id"
        case params = "params"
    }
}

class TicketCommandResult: Codable {
    let commandId: String
    let commentId: Int?
    let ticketId: Int?
    let error: ServiceError?
    let operatorResponseTimeMessage: String?
    
    enum CodingKeys: String, CodingKey {
        case commandId = "command_id"
        case commentId = "comment_id"
        case ticketId = "ticket_id"
        case error = "error"
        case operatorResponseTimeMessage = "operator_response_time_message"
    }
}

struct ServiceError: Codable {
    let text: String?
    let code: Int?
}


extension Encodable {
    func toDictionary() -> [String: Any?]? {
        do {
            let data = try JSONEncoder().encode(self)
            
            if let jsonObject = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                return jsonObject
            } else {
                return nil
            }
        } catch {
            return nil
        }
    }
}
