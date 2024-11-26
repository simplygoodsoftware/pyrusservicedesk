import Foundation

enum TicketCommandType: Int {
    case createComment
    case readTicket
}

struct AttachmentData: Codable {
    let type: Int
    let name: String
    let guid: String?
}

class TicketCommandParams: Codable {
    let ticketId: Int?
    let appId: String?
    let requestNewTicket: Bool?
    let userId: String?
    let message: String?
    let attachments: [AttachmentData]?
    
    init(ticketId: Int?, appId: String?, requestNewTicket: Bool? = nil, userId: String?, message: String? = nil, attachments: [AttachmentData]? = nil, authorId: String? = nil) {
        self.ticketId = ticketId
        self.appId = appId
        self.requestNewTicket = requestNewTicket
        self.userId = userId
        self.message = message
        self.attachments = attachments
    }
    
    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case appId = "app_id"
        case requestNewTicket = "request_new_ticket"
        case userId = "user_id"
        case message = "comment"
        case attachments = "attachments"
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
    
    enum CodingKeys: String, CodingKey {
        case commandId = "command_id"
        case commentId = "comment_id"
        case ticketId = "ticket_id"
        case error = "error"
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
