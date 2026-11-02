import Foundation

/// Определяет, каких пользователей из `extra_users` ответа можно добавлять.
///
/// Extra user с user_id = B добавляется только при переносе тикета:
/// тикет есть в кэше с user_id = A, а в этом же ответе тот же ticket_id
/// пришёл с непустым user_id = B ≠ A. Пустой user_id в кэше (анонимный
/// тикет) тоже считается переносом — это сценарий привязки анонимного
/// клиента к пользователю.
enum HelpySyncExtraUsersFilter {

    /// - Parameter cachedChats: тот же снапшот кэша, из которого собирался
    ///   блоб `tickets` запроса (см. `PSDHelpySync`).
    static func reassignedUserIds(
        tickets: [HelpySyncTicket],
        cachedChats: [PSDChat]
    ) -> Set<String> {
        let cachedUserIdsByTicketId = makeCachedUserIdsIndex(from: cachedChats)

        var userIds = Set<String>()
        for ticket in tickets {
            let newUserId = ticket.userIdString
            guard !newUserId.isEmpty,
                  let cachedUserId = cachedUserIdsByTicketId[ticket.ticketId],
                  cachedUserId != newUserId else {
                continue
            }
            userIds.insert(newUserId)
        }
        return userIds
    }
}

private extension HelpySyncExtraUsersFilter {

    static func makeCachedUserIdsIndex(from cachedChats: [PSDChat]) -> [Int: String] {
        var index = [Int: String]()
        for chat in cachedChats {
            // Локальные тикеты (отрицательные id) пропускаем.
            guard let chatId = chat.chatId, chatId > 0 else {
                continue
            }
            index[chatId] = chat.userId ?? ""
        }
        return index
    }
}
