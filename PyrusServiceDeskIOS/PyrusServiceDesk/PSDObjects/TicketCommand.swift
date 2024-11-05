import Foundation

enum TicketCommandType: String {
    case createComment
    case readTicket
}

protocol TicketCommandParamsProtocol: Codable {
    var ticketId: Int { get }
    var authorId: String? { get }
}

struct MarkTicketAsReadTicketCommand: TicketCommandParamsProtocol {
    var ticketId: Int
    var authorId: String?
    
    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case authorId = "author_id"
    }
}

struct CreateCommentTicketCommand: TicketCommandParamsProtocol {
    var ticketId: Int
    var authorId: String?
    var requestNewTicket: Bool?
    var userId: String?
    var message: String
    var attachments: [AttachmentData]?
    var authorName: String?
    
    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case authorId = "author_id"
        case requestNewTicket = "request_new_ticket"
        case userId = "user_id"
        case message = "message"
        case attachments = "attachments"
        case authorName = "author_name"
    }
}

struct AttachmentData: Codable {
    let type: Int
    let name: String
    let guid: String
}

enum TicketCommandParams: Codable {
    case markAsRead(MarkTicketAsReadTicketCommand)
    case createComment(CreateCommentTicketCommand)

    enum CodingKeys: String, CodingKey {
        case type
        case data
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        switch self {
        case .markAsRead(let params):
            try container.encode(TicketCommandType.readTicket.rawValue, forKey: .type)
            try container.encode(params, forKey: .data)
        case .createComment(let params):
            try container.encode(TicketCommandType.createComment.rawValue, forKey: .type)
            try container.encode(params, forKey: .data)
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let type = try container.decode(String.self, forKey: .type)
        
        switch type {
        case TicketCommandType.readTicket.rawValue:
            let params = try container.decode(MarkTicketAsReadTicketCommand.self, forKey: .data)
            self = .markAsRead(params)
        case TicketCommandType.createComment.rawValue:
            let params = try container.decode(CreateCommentTicketCommand.self, forKey: .data)
            self = .createComment(params)
        default:
            throw DecodingError.dataCorruptedError(forKey: .type, in: container, debugDescription: "Unknown type")
        }
    }
}

class TicketCommand: NSObject, Codable {
    let commandId: String
    let type: String
    let params: TicketCommandParams

    init(type: TicketCommandType, params: TicketCommandParams) {
        self.commandId = UUID().uuidString
        self.type = type.rawValue
        self.params = params
    }
}
