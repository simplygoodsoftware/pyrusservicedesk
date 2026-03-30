final class PSDLogHelper {
    
    static func createRequestNewTicketLog(chat: PSDChat?, newTicket: Bool) -> Logs {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yy HH:mm:ss zzz"
        struct TicketInfo: Codable {
            let ticketId: String
            let isActive: Bool
            let date: String
            let lastReadedCommentId: Int?
        }
        let tickets = PyrusServiceDesk.chats.map {
            let ticketId = $0.chatId == nil ? "nil" : "\($0.chatId ?? 0)"
            let date = $0.date == nil ? "nil" : "\(formatter.string(from: $0.date ?? Date()))"
            return TicketInfo(ticketId: ticketId, isActive: $0.isActive, date: date, lastReadedCommentId: $0.lastReadedCommentId ?? 0)
        }
        var ticketsInfo = ""
        do {
            let data = try JSONEncoder().encode(tickets)
            ticketsInfo = String(data: data, encoding: .utf8) ?? ""
        } catch { }
        
        let createMessages = PSDMessagesStorage.getNewCreateTicketMessages(PyrusServiceDesk.customUserId)
        
        let message = """
            \(formatter.string(from: Date())) Create new ticket: appId: \(PyrusServiceDesk.clientId ?? "nil"), userId: \(PyrusServiceDesk.customUserId ?? "nil"), ticketId: \(chat?.chatId == nil ? "nil" : "\(chat?.chatId ?? 0)"), isActive: \(chat?.isActive ?? false), newTicketFlag: \(newTicket), chatsCount: \(PyrusServiceDesk.chats.count), chats: \(ticketsInfo), localMessagesCount = \(createMessages.count)
            """
        return Logs(exceptions: [LogInfo(message: message, stack: "", serial: 1)])
    }
}
