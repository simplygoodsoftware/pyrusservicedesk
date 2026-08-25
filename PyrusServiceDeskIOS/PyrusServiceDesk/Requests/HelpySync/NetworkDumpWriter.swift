import Foundation

/// Пишет тела сетевых запросов и ответов в JSON-файлы.
///
/// Запрос и ответ сохраняются в отдельные файлы в разных подпапках
/// (`Requests` / `Responses`) и связываются общим именем:
/// `<label>_<время>_<id>_request.json` и `..._response.json`.
///
/// Запрос пишется в момент отправки — он останется на диске,
/// даже если ответ не пришёл (таймаут, обрыв, краш).
struct NetworkDumpWriter {

    private enum Constants {
        static let rootDirectoryName = "PyrusServiceDeskDumps"
        static let requestsDirectoryName = "Requests"
        static let responsesDirectoryName = "Responses"
        static let requestSuffix = "request"
        static let responseSuffix = "response"
        static let fileExtension = "json"
        static let fileNameDateFormat = "yyyy-MM-dd_HH-mm-ss-SSS"
        static let timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS Z"
        static let posixLocaleIdentifier = "en_US_POSIX"
        static let queueLabel = "com.pyrus.servicedesk.networkDump"
        static let identifierLength = 6
        /// Сколько последних файлов хранить в каждой папке.
        static let maxStoredFiles = 200

        enum Key {
            static let label = "label"
            static let identifier = "identifier"
            static let date = "date"
            static let kind = "kind"
            static let url = "url"
            static let method = "method"
            static let headers = "headers"
            static let statusCode = "statusCode"
            static let error = "error"
            static let durationSeconds = "durationSeconds"
            static let body = "body"
            static let bodySize = "bodySizeBytes"
        }
    }

    /// Связка запроса и ответа: возвращается при записи запроса
    /// и передаётся при записи ответа.
    struct DumpToken {
        let label: String
        let identifier: String
        let fileNameBase: String
    }

    /// Включает запись дампов. Отключается из кода приложения,
    /// если нужно перестать писать на диск.
    static var isEnabled = true

    private static let queue = DispatchQueue(label: Constants.queueLabel, qos: .utility)

    private static let fileNameFormatter = makeFormatter(format: Constants.fileNameDateFormat)
    private static let timestampFormatter = makeFormatter(format: Constants.timestampFormat)

    /// Корневая папка с дампами.
    static var dumpsDirectoryURL: URL? {
        FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask)
            .first?
            .appendingPathComponent(Constants.rootDirectoryName, isDirectory: true)
    }

    /// Сохраняет тело запроса целиком.
    /// - Returns: токен для последующей записи ответа, либо `nil`, если запись выключена.
    @discardableResult
    static func saveRequest(
        label: String,
        request: URLRequest,
        fallbackBody: Data? = nil
    ) -> DumpToken? {
        guard isEnabled else { return nil }

        let date = Date()
        let identifier = makeIdentifier()
        let fileNameBase = "\(label)_\(fileNameFormatter.string(from: date))_\(identifier)"
        let token = DumpToken(label: label, identifier: identifier, fileNameBase: fileNameBase)
        let body = request.httpBody ?? fallbackBody

        var payload: [String: Any] = [
            Constants.Key.kind: Constants.requestSuffix,
            Constants.Key.url: request.url?.absoluteString ?? "",
            Constants.Key.method: request.httpMethod ?? ""
        ]
        if let headers = request.allHTTPHeaderFields, !headers.isEmpty {
            payload[Constants.Key.headers] = headers
        }

        write(
            payload: payload,
            token: token,
            date: date,
            body: body,
            directoryName: Constants.requestsDirectoryName,
            suffix: Constants.requestSuffix
        )
        return token
    }

    /// Сохраняет тело ответа целиком (до любых починок и преобразований).
    static func saveResponse(
        for token: DumpToken?,
        body: Data?,
        statusCode: Int?,
        error: Error?,
        duration: TimeInterval?
    ) {
        guard isEnabled, let token else { return }

        var payload: [String: Any] = [Constants.Key.kind: Constants.responseSuffix]
        if let statusCode {
            payload[Constants.Key.statusCode] = statusCode
        }
        if let error {
            payload[Constants.Key.error] = error.localizedDescription
        }
        if let duration {
            payload[Constants.Key.durationSeconds] = (duration * 1000).rounded() / 1000
        }

        write(
            payload: payload,
            token: token,
            date: Date(),
            body: body,
            directoryName: Constants.responsesDirectoryName,
            suffix: Constants.responseSuffix
        )
    }
}

private extension NetworkDumpWriter {

    static func makeFormatter(format: String) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: Constants.posixLocaleIdentifier)
        formatter.dateFormat = format
        return formatter
    }

    static func makeIdentifier() -> String {
        String(UUID().uuidString.replacingOccurrences(of: "-", with: "")
            .prefix(Constants.identifierLength))
            .lowercased()
    }

    static var writingOptions: JSONSerialization.WritingOptions {
        var options: JSONSerialization.WritingOptions = [.prettyPrinted, .sortedKeys]
        if #available(iOS 13.0, *) {
            options.insert(.withoutEscapingSlashes)
        }
        return options
    }

    static func write(
        payload: [String: Any],
        token: DumpToken,
        date: Date,
        body: Data?,
        directoryName: String,
        suffix: String
    ) {
        queue.async {
            guard let root = dumpsDirectoryURL else { return }
            let directory = root.appendingPathComponent(directoryName, isDirectory: true)

            var dump = payload
            dump[Constants.Key.label] = token.label
            dump[Constants.Key.identifier] = token.identifier
            dump[Constants.Key.date] = timestampFormatter.string(from: date)
            dump[Constants.Key.bodySize] = body?.count ?? 0
            dump[Constants.Key.body] = jsonValue(from: body)

            guard
                JSONSerialization.isValidJSONObject(dump),
                let fileData = try? JSONSerialization.data(withJSONObject: dump, options: writingOptions)
            else {
                print("Network dump serialization failed: \(token.fileNameBase)_\(suffix)")
                return
            }

            let fileURL = directory.appendingPathComponent(
                "\(token.fileNameBase)_\(suffix).\(Constants.fileExtension)",
                isDirectory: false
            )

            do {
                try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
                try fileData.write(to: fileURL, options: .atomic)
                print("Network dump saved: \(fileURL.path)")
                removeOutdatedFiles(in: directory)
            } catch {
                print("Network dump write failed: \(error)")
            }
        }
    }

    /// Тело кладём разобранным деревом — так файл читается и ищется как JSON.
    /// Если тело не парсится (обрыв, HTML от прокси) — сохраняем как строку,
    /// чтобы не потерять содержимое.
    static func jsonValue(from data: Data?) -> Any {
        guard let data, !data.isEmpty else { return NSNull() }
        if let object = try? JSONSerialization.jsonObject(with: data, options: [.allowFragments]) {
            return object
        }
        return String(decoding: data, as: UTF8.self)
    }

    static func removeOutdatedFiles(in directory: URL) {
        let fileManager = FileManager.default
        guard
            let files = try? fileManager.contentsOfDirectory(
                at: directory,
                includingPropertiesForKeys: [.creationDateKey],
                options: [.skipsHiddenFiles]
            ),
            files.count > Constants.maxStoredFiles
        else {
            return
        }

        let sorted = files.sorted { lhs, rhs in
            let lhsDate = (try? lhs.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? .distantPast
            let rhsDate = (try? rhs.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? .distantPast
            return lhsDate > rhsDate
        }
        for file in sorted.dropFirst(Constants.maxStoredFiles) {
            try? fileManager.removeItem(at: file)
        }
    }
}
