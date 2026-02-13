
struct GetTicketsResponse {
    let complete: Bool
    let chats: [PSDChat]?
    let clients: [PSDClientInfo]?
    let commandsResult: [TicketCommandResult]?
    let authorAccessDenied: [String]?
    let announcementsResult: AnnouncementsResult?
    
    init(complete: Bool,
         chats: [PSDChat]? = nil,
         clients: [PSDClientInfo]? = nil,
         commandsResult: [TicketCommandResult]? = nil,
         authorAccessDenied: [String]? = nil,
         announcementsResult: AnnouncementsResult? = nil
    ) {
        self.complete = complete
        self.chats = chats
        self.clients = clients
        self.commandsResult = commandsResult
        self.authorAccessDenied = authorAccessDenied
        self.announcementsResult = announcementsResult
    }
}

struct ClientsResult {
    let clients: [PSDClientInfo]
    let serverAnnouncements: [String: AnnouncementsResponse]
}

struct AnnouncementsResult {
    let newAnnouncements: [PSDAnnouncement]?
    let deletedAnnouncementsIds: Set<String>?
}
