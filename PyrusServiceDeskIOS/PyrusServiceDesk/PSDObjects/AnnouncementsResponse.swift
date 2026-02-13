import Foundation

// MARK: - Root

struct AnnouncementsResponse: Codable {
    let inboxItem: InboxItem
    let newAnnouncements: [Announcement]?
    let announcementChanges: [AnnouncementChange]?

    enum CodingKeys: String, CodingKey {
        case inboxItem = "inbox_item"
        case newAnnouncements = "new_announcements"
        case announcementChanges = "announcement_changes"
    }
}

// MARK: - Inbox

struct InboxItem: Codable {
    let lastMessageDatetimeUTC: String?//Date?
    let lastReadMessageId: String?
    let unreadCount: Int

    enum CodingKeys: String, CodingKey {
        case lastMessageDatetimeUTC = "last_message_datetime_utc"
        case lastReadMessageId = "last_read_message_id"
        case unreadCount = "unread_count"
    }
}

// MARK: - Announcement

struct Announcement: Codable {
    let id: String
    let type: AnnouncementType
    let createdAt: Date
    let editedAt: Date?
    let content: Content

    enum CodingKeys: String, CodingKey {
        case id, type, content
        case createdAt = "created_at"
        case editedAt = "edited_at"
    }
}

enum AnnouncementType: String, Codable {
    case message = "Message"
}

// MARK: - Changes

struct AnnouncementChange: Codable {
    let type: ChangeType
    let messageId: String
    let performedAt: Date
    let content: Content? // есть только у Edited

    enum CodingKeys: String, CodingKey {
        case type, content
        case messageId = "message_id"
        case performedAt = "performed_at"
    }
}

enum ChangeType: String, Codable {
    case edited = "Edited"
    case deleted = "Deleted"
}

// MARK: - Content

struct Content: Codable {
    let attachments: [Attachment]?
    let richTextDocument: RichTextDocument

    enum CodingKeys: String, CodingKey {
        case attachments
        case richTextDocument = "rich_text_document"
    }
}

// MARK: - Attachment

struct Attachment: Codable {
    let id: String
    let name: String?
    let size: Int
    let width: Int
    let height: Int
    let media: Bool
}

enum MediaType: Int, Codable {
    case image = 1
}

// MARK: - Rich Text

struct RichTextDocument: Codable {
    let version: Int
    let richTextBlocks: [RichTextBlock]

    enum CodingKeys: String, CodingKey {
        case version
        case richTextBlocks = "rich_text_blocks"
    }
}

struct RichTextBlock: Codable {
    let type: BlockType
    let richTextInlines: [RichTextInline]

    enum CodingKeys: String, CodingKey {
        case type
        case richTextInlines = "rich_text_inlines"
    }
}

enum BlockType: String, Codable {
    case paragraph = "Paragraph"
}

struct RichTextInline: Codable {
    let type: InlineType
    let string: String
    let marks: String?
}

enum InlineType: String, Codable {
    case text = "Text"
}

// пока заглушка
struct Mark: Codable {}
