import Foundation

/// Маппит тикеты из ответа HelpySync в доменные `PSDChat`.
///
/// Отличия от прежнего разбора GetTickets (`PSDGetChats.generateChats`),
/// продиктованные спекой:
/// - сервер больше не присылает `last_comment` — последний комментарий
///   определяется на клиенте по дельте и кэшу;
/// - сервер больше не присылает `is_read` — прочитанность вычисляется
///   на клиенте из кэша и `last_read_comment_id`;
/// - `user_id` приходит как long? (nil у анонимных тикетов), для анонимных
///   тикетов дополнительно приходит `app_id`;
/// - `is_inbound` теперь означает «отправлен из приложения»
///   (false — комментарий от поддержки).
enum HelpySyncChatsMapper {

    /// - Parameter cachedChats: тот же снапшот кэша, из которого собирался
    ///   блоб `tickets` запроса, — дельта применяется ровно к нему.
    static func makeChats(
        from tickets: [HelpySyncTicket],
        cachedChats: [PSDChat]
    ) -> [PSDChat] {
        let cachedChatsById = makeCachedChatsIndex(from: cachedChats)
        let currentUserId = PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        let usersById: [String: PSDUserInfo] = Dictionary(
            PyrusServiceDesk.additionalUsers.map { ($0.userId ?? "", $0) },
            uniquingKeysWith: { first, _ in first }
        )

        var chats = [PSDChat]()
        chats.reserveCapacity(tickets.count)

        for ticket in tickets {
            let cachedChat = cachedChatsById[ticket.ticketId]
            let messages = makeMessages(from: ticket.comments ?? [])
            let ticketCreatedAt = parseDate(ticket.createdAt)

            let lastMessage = messages.last ?? cachedChat?.lastComment
            let chatDate = lastMessage?.date ?? cachedChat?.date ?? ticketCreatedAt ?? Date()

            updateLastNoteIdIfNeeded(
                lastMessage: lastMessage,
                userId: ticket.userIdString,
                currentUserId: currentUserId,
                usersById: usersById
            )

            let chat = PSDChat(chatId: ticket.ticketId, date: chatDate, messages: messages)
            chat.subject = ticket.subject
            chat.userId = ticket.userIdString
            chat.appId = ticket.appId
            chat.createdAt = ticketCreatedAt
            chat.lastComment = lastMessage
            chat.lastReadedCommentId = ticket.lastReadCommentId
            chat.isRead = isTicketRead(
                lastReadCommentId: ticket.lastReadCommentId,
                deltaMessages: messages,
                cachedChat: cachedChat
            )

            chat.showRating = ticket.showRating ?? false
            if isMoreThan24Hours(from: lastMessage?.date ?? Date()) {
                chat.showRating = false
            }
            chat.showRatingText = ticket.showRatingText

            // Сохраняем существующую особенность: активность не перетирается,
            // пока в мультичатах показан запрос оценки.
            if !chat.showRating || !PyrusServiceDesk.multichats {
                chat.isActive = ticket.isActive ?? true
            }

            chats.append(chat)
        }

        // Сортировка тикетов по спеке полностью на клиенте.
        return PSDGetChats.sortByLastMessage(chats)
    }
}

private extension HelpySyncChatsMapper {

    private enum Constants {
        static let ratingVisibilityInterval: TimeInterval = 24 * 60 * 60
    }

    static func makeCachedChatsIndex(from cachedChats: [PSDChat]) -> [Int: PSDChat] {
        return Dictionary(
            cachedChats.compactMap { chat in
                chat.chatId.map { ($0, chat) }
            },
            uniquingKeysWith: { first, _ in first }
        )
    }

    // MARK: Messages

    static func makeMessages(from comments: [HelpySyncComment]) -> [PSDMessage] {
        var messages = [PSDMessage]()
        messages.reserveCapacity(comments.count)

        for comment in comments {
            guard let message = makeMessage(from: comment) else {
                continue
            }
            messages.append(message)
        }
        return messages
    }

    static func makeMessage(from comment: HelpySyncComment) -> PSDMessage? {
        let text = normalizedText(from: comment.body)
        let rating = normalizedRating(from: comment.rating)
        let attachments = makeAttachments(from: comment.attachments)

        // Как и раньше: пустые комментарии (без текста, вложений и оценки)
        // в ленту не попадают.
        guard text != nil || rating != nil || attachments != nil else {
            return nil
        }

        let isOutgoing = comment.author?.authorId == PyrusServiceDesk.authorId
        let owner: PSDUser? = isOutgoing ? PSDUsers.user : makeSupportUser(from: comment.author)

        let message = PSDMessage(
            text: text,
            attachments: attachments,
            messageId: String(comment.commentId),
            owner: owner,
            date: parseDate(comment.createdAt) ?? Date()
        )
        message.rating = rating
        message.isOutgoing = isOutgoing
        message.isSupportMessage = isFromSupport(comment)
        message.isSystemMessage = comment.isSystem ?? false
        if let clientId = comment.clientId, !clientId.isEmpty {
            message.clientId = clientId
        }
        return message
    }

    /// По спеке is_inbound == false означает комментарий от поддержки.
    /// Если поле не пришло, определяем по автору: author_id равен nil,
    /// когда комментарий создан не на стороне приложения.
    static func isFromSupport(_ comment: HelpySyncComment) -> Bool {
        if let isInbound = comment.isInbound {
            return !isInbound
        }
        return comment.author?.authorId == nil
    }

    static func normalizedText(from body: String?) -> String? {
        guard let body, !body.isEmpty else {
            return nil
        }
        return body
    }

    static func normalizedRating(from rating: Int?) -> Int? {
        guard let rating, rating != 0 else {
            return nil
        }
        return rating
    }

    static func makeAttachments(from attachments: [HelpySyncCommentAttachment]?) -> [PSDAttachment]? {
        guard let attachments, !attachments.isEmpty else {
            return nil
        }
        return attachments.map { attachmentData in
            let attachment = PSDAttachment(
                localPath: "",
                data: nil,
                serverIdentifer: String(attachmentData.id)
            )
            attachment.name = attachmentData.name ?? ""
            attachment.size = attachmentData.size ?? 0
            return attachment
        }
    }

    static func makeSupportUser(from author: HelpySyncCommentAuthor?) -> PSDUser {
        guard let author else {
            return PSDUser(
                personId: "0",
                name: "Support_Default_Name".localizedPSD(),
                type: .support,
                imagePath: ""
            )
        }
        return PSDUsers.supportUsersContain(
            name: author.name ?? "",
            imagePath: author.avatarId ?? "",
            authorId: author.authorId
        )
    }

    // MARK: Read state

    /// Тикет не прочитан, если среди известных клиенту комментариев
    /// (дельта + кэш) есть чужой комментарий с id больше last_read_comment_id.
    static func isTicketRead(
        lastReadCommentId: Int?,
        deltaMessages: [PSDMessage],
        cachedChat: PSDChat?
    ) -> Bool {
        let lastReadId = lastReadCommentId ?? 0
        let knownMessages = deltaMessages + (cachedChat?.messages ?? [])

        for message in knownMessages {
            guard
                let messageId = Int(message.messageId),
                messageId > lastReadId,
                message.owner?.authorId != PyrusServiceDesk.authorId
            else {
                continue
            }
            return false
        }
        return true
    }

    // MARK: Dates

    /// Даты приходят строками. Сначала пробуем `fastParseISODate` —
    /// проверенный парсер, которым пользовался старый разбор GetTickets, —
    /// затем толерантный парсер фабрики (в т.ч. .NET-формат с 7 знаками
    /// дробных секунд). nil никогда не валит разбор — фолбэки на месте вызова.
    static func parseDate(_ string: String?) -> Date? {
        guard let string, !string.isEmpty else {
            return nil
        }
        return string.fastParseISODate() ?? PSDJSONDecoderFactory.date(fromServerString: string)
    }

    // MARK: Last note id

    /// Сохраняем прежнее поведение GetTickets: глобальные lastNoteId
    /// продолжают обновляться — на них завязана остальная (нетронутая) логика.
    static func updateLastNoteIdIfNeeded(
        lastMessage: PSDMessage?,
        userId: String,
        currentUserId: String,
        usersById: [String: PSDUserInfo]
    ) {
        guard let messageId = Int(lastMessage?.messageId ?? "0"), messageId > 0 else {
            return
        }
        if userId == currentUserId {
            if (PyrusServiceDesk.lastNoteId ?? 0) < messageId {
                PyrusServiceDesk.lastNoteId = messageId
            }
        } else if let user = usersById[userId], (user.lastNoteId ?? 0) < messageId {
            user.lastNoteId = messageId
        }
    }

    static func isMoreThan24Hours(from date: Date) -> Bool {
        return Date().timeIntervalSince(date) > Constants.ratingVisibilityInterval
    }
}

extension HelpySyncTicket {
    /// Внутри SDK идентификаторы пользователей хранятся строками;
    /// пустая строка — прежнее значение для анонимных тикетов.
    var userIdString: String {
        guard let userId else {
            return ""
        }
        return String(userId)
    }
}
