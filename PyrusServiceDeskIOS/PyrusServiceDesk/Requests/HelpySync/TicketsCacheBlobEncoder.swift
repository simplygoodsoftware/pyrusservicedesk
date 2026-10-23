import Foundation

/// Запись о состоянии одного тикета в кэше клиента.
struct TicketCacheEntry {
    let ticketId: Int64
    /// id последнего комментария тикета в кэше клиента.
    /// 0 — у клиента нет комментариев тикета, сервер вернёт всю историю.
    let lastNoteId: Int64
}

/// Упаковывает состояние кэша тикетов в base64-блоб для поля `tickets`
/// запроса HelpySync.
///
/// Формат строго по спеке — подряд идущие записи по 16 байт:
///   байты 0–7  — ticket_id    (Int64, little-endian),
///   байты 8–15 — last_note_id (Int64, little-endian).
///
/// Порядок байт — little-endian (младший байт первым), как в бинарных
/// форматах синка остальных приложений Pyrus.
enum TicketsCacheBlobEncoder {

    enum Constants {
        static let bytesPerField = MemoryLayout<Int64>.size
        static let bytesPerEntry = bytesPerField * 2
    }

    /// Возвращает base64-строку блоба или nil, если записей нет
    /// (пустой кэш — поле в запрос не передаётся, сервер вернёт всё).
    static func encode(_ entries: [TicketCacheEntry]) -> String? {
        guard !entries.isEmpty else {
            return nil
        }

        var data = Data(capacity: entries.count * Constants.bytesPerEntry)
        for entry in entries {
            append(entry.ticketId, to: &data)
            append(entry.lastNoteId, to: &data)
        }
        return data.base64EncodedString()
    }

    /// Обратная распаковка блоба — для логов и самопроверки формата.
    static func decode(_ base64: String) -> [TicketCacheEntry]? {
        guard
            let data = Data(base64Encoded: base64),
            data.count % Constants.bytesPerEntry == 0
        else {
            return nil
        }

        var entries = [TicketCacheEntry]()
        entries.reserveCapacity(data.count / Constants.bytesPerEntry)
        var offset = data.startIndex
        while offset < data.endIndex {
            let ticketId = readInt64(from: data, at: offset)
            let lastNoteId = readInt64(from: data, at: offset + Constants.bytesPerField)
            entries.append(TicketCacheEntry(ticketId: ticketId, lastNoteId: lastNoteId))
            offset += Constants.bytesPerEntry
        }
        return entries
    }
}

private extension TicketsCacheBlobEncoder {

    static func append(_ value: Int64, to data: inout Data) {
        withUnsafeBytes(of: value.littleEndian) { buffer in
            data.append(contentsOf: buffer)
        }
    }

    static func readInt64(from data: Data, at offset: Data.Index) -> Int64 {
        var raw: Int64 = 0
        withUnsafeMutableBytes(of: &raw) { buffer in
            data.copyBytes(to: buffer, from: offset..<(offset + Constants.bytesPerField))
        }
        return Int64(littleEndian: raw)
    }
}
