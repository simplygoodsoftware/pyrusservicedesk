import Foundation

/// Фабрика JSONDecoder и парсинг серверных дат.
///
/// Сервер (.NET) может прислать дату в нескольких вариантах:
/// - ISO8601 с миллисекундами: `2026-08-06T12:34:56.789Z`
/// - ISO8601 без дробной части: `2026-08-06T12:34:56Z`
/// - с 7 знаками дробных секунд (.NET DateTime): `2026-08-06T12:34:56.7890123Z`
/// - без указания таймзоны (трактуем как UTC): `2026-08-06T12:34:56.7890123`
/// - зона без двоеточия: `+0300`
/// `ISO8601DateFormatter` строгий и принимает только первые два варианта,
/// поэтому остальные приводятся к каноничному виду нормализацией.
enum PSDJSONDecoderFactory {

    /// Декодер с толерантной стратегией ISO8601-дат.
    static func makeServerResponseDecoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let string = try container.decode(String.self)
            if let date = date(fromServerString: string) {
                return date
            }
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unsupported ISO8601 date: \(string)"
            )
        }
        return decoder
    }

    /// Парсит дату из строки серверного формата (см. описание типа).
    static func date(fromServerString string: String) -> Date? {
        if let date = date(fromISO8601: string) {
            return date
        }
        guard let normalized = normalized(string) else {
            return nil
        }
        return date(fromISO8601: normalized)
    }

    /// Парсит строго стандартную ISO8601-строку (с миллисекундами или без).
    static func date(fromISO8601 string: String) -> Date? {
        return fractionalFormatter.date(from: string) ?? plainFormatter.date(from: string)
    }
}

private extension PSDJSONDecoderFactory {

    enum Constants {
        /// yyyy-MM-ddTHH:mm:ss (.дробная часть)? (зона)?
        static let serverDatePattern =
            #"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$"#
        static let utcSuffix = "Z"
        static let fractionDigits = 3
    }

    // ISO8601DateFormatter потокобезопасен, поэтому форматтеры можно
    // держать статически и не создавать на каждый декод.
    static let fractionalFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let plainFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    static let serverDateRegex = try? NSRegularExpression(pattern: Constants.serverDatePattern)

    /// Приводит нестандартные варианты к парсибельному ISO8601:
    /// - дробная часть секунд усечённая/дополненная до миллисекунд,
    /// - отсутствие зоны трактуется как UTC,
    /// - зона без двоеточия получает двоеточие.
    static func normalized(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespaces)
        guard
            let regex = serverDateRegex,
            let match = regex.firstMatch(
                in: trimmed,
                range: NSRange(trimmed.startIndex..., in: trimmed)
            )
        else {
            return nil
        }

        guard let base = substring(of: trimmed, at: match.range(at: 1)) else {
            return nil
        }

        var result = base

        if let fraction = substring(of: trimmed, at: match.range(at: 2)) {
            result += "." + normalizedFraction(fraction)
        }

        if let timezone = substring(of: trimmed, at: match.range(at: 3)) {
            result += normalizedTimezone(timezone)
        } else {
            result += Constants.utcSuffix
        }

        return result
    }

    /// Ровно 3 знака дробной части: лишние отбрасываем, недостающие добиваем нулями.
    static func normalizedFraction(_ fraction: String) -> String {
        return String(fraction.prefix(Constants.fractionDigits))
            .padding(toLength: Constants.fractionDigits, withPad: "0", startingAt: 0)
    }

    /// `+0300` → `+03:00`; `Z` и `+03:00` остаются как есть.
    static func normalizedTimezone(_ timezone: String) -> String {
        guard timezone != Constants.utcSuffix, !timezone.contains(":") else {
            return timezone
        }
        let sign = String(timezone.prefix(1))
        let digits = String(timezone.dropFirst())
        guard digits.count == 4 else {
            return timezone
        }
        return sign + digits.prefix(2) + ":" + digits.suffix(2)
    }

    static func substring(of string: String, at nsRange: NSRange) -> String? {
        guard nsRange.location != NSNotFound, let range = Range(nsRange, in: string) else {
            return nil
        }
        return String(string[range])
    }
}
