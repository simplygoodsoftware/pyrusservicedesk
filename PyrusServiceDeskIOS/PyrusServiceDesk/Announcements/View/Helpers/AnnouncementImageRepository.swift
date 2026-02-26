import Foundation

/// Репозиторий для загрузки и кеширования бинарных данных вложений (Data) по id.
/// - Хранит максимум 100 объектов (в памяти и на диске).
/// - Потокобезопасен (actor).
/// - Коалесцирует параллельные запросы к одному и тому же id.
actor AnnouncementAttachmentsRepository {

    // MARK: - Singleton
    static let shared = AnnouncementAttachmentsRepository()

    // MARK: - Конфиг
    private let maxStoredObjects = 100
    private let baseURLString = "https://files.pyrus.com/services/me/helpy/chats/attachments"
    private let contentTypeHeaderValue = contenttype // используйте ваш существующий contenttype

    // MARK: - Кеши/состояние
    private let memoryCache = NSCache<NSString, NSData>()
    private var inflightTasks = [String: Task<Data, Error>]()

    private init() {
        memoryCache.countLimit = maxStoredObjects
    }

    // MARK: - Публичный API

    /// Возвращает данные вложения по его id и authorId.
    /// - Сначала ищет в памяти, затем на диске. Если нет — скачивает, сохраняет и возвращает.
    func data(for attachmentId: String, authorId: String) async throws -> Data {
        let key = attachmentId as NSString

        if let cached = memoryCache.object(forKey: key) {
            await updateDiskAccessDateIfExists(for: attachmentId)
            return Data(referencing: cached)
        }

        if let diskData = try readFromDisk(for: attachmentId) {
            memoryCache.setObject(diskData as NSData, forKey: key)
            await updateDiskAccessDateIfExists(for: attachmentId)
            return diskData
        }

        if let task = inflightTasks[attachmentId] {
            return try await task.value
        }

        let task = Task<Data, Error> {
            let data = try await downloadAttachment(authorId: authorId, attachmentId: attachmentId)
            self.memoryCache.setObject(data as NSData, forKey: key)
            try await self.writeToDisk(data: data, for: attachmentId)
            await self.enforceDiskLimit()
            return data
        }
        inflightTasks[attachmentId] = task

        do {
            let data = try await task.value
            inflightTasks[attachmentId] = nil
            return data
        } catch {
            inflightTasks[attachmentId] = nil
            throw error
        }
    }

    /// Есть ли данные для id в кеше (память или диск).
    func hasData(for attachmentId: String) -> Bool {
        if memoryCache.object(forKey: attachmentId as NSString) != nil { return true }
        let url = fileURL(for: attachmentId)
        return FileManager.default.fileExists(atPath: url.path)
    }

    /// Удалить данные для конкретного id из памяти и диска.
    func remove(for attachmentId: String) {
        memoryCache.removeObject(forKey: attachmentId as NSString)
        try? FileManager.default.removeItem(at: fileURL(for: attachmentId))
    }

    /// Очистить все кеши.
    func removeAll() {
        memoryCache.removeAllObjects()
        let dir = diskDirectoryURL
        if FileManager.default.fileExists(atPath: dir.path) {
            if let items = try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil) {
                for url in items {
                    try? FileManager.default.removeItem(at: url)
                }
            }
        }
    }

    // MARK: - Сеть

    private func downloadAttachment(authorId: String, attachmentId: String) async throws -> Data {
        var components = URLComponents(string: baseURLString)!
        components.queryItems = [
            URLQueryItem(name: "id", value: attachmentId),
            URLQueryItem(name: "authorId", value: authorId)
        ]
        guard let url = components.url else { throw URLError(.badURL) }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.addCustomHeaders()
        request.addUserAgent()
        // Если нужна авторизация:
        // request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return data
    }

    // MARK: - Диск (LRU по modificationDate)

    private var diskDirectoryURL: URL {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        return base.appendingPathComponent("AnnouncementAttachments", isDirectory: true)
    }

    private func ensureDiskDirectory() {
        let url = diskDirectoryURL
        if !FileManager.default.fileExists(atPath: url.path) {
            try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        }
    }

    private func fileURL(for key: String) -> URL {
        ensureDiskDirectory()
        // Если id может содержать недопустимые символы, можно добавить расширение или хеш,
        // но большинство строковые id подходят как имя файла.
        return diskDirectoryURL.appendingPathComponent(key)
    }

    private func readFromDisk(for key: String) throws -> Data? {
        let url = fileURL(for: key)
        if FileManager.default.fileExists(atPath: url.path) {
            let data = try Data(contentsOf: url)
            return data
        }
        return nil
    }

    private func writeToDisk(data: Data, for key: String) async throws {
        let url = fileURL(for: key)
        try data.write(to: url, options: .atomic)
        try FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: url.path)
    }

    private func updateDiskAccessDateIfExists(for key: String) async {
        let url = fileURL(for: key)
        if FileManager.default.fileExists(atPath: url.path) {
            try? FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: url.path)
        }
    }

    private func enforceDiskLimit() async {
        ensureDiskDirectory()
        let dir = diskDirectoryURL
        guard let items = try? FileManager.default.contentsOfDirectory(
            at: dir,
            includingPropertiesForKeys: [.contentModificationDateKey],
            options: [.skipsHiddenFiles]
        ) else { return }

        guard items.count > maxStoredObjects else { return }

        let sortedByAge = items.sorted { lhs, rhs in
            let lDate = (try? lhs.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
            let rDate = (try? rhs.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
            return lDate < rDate
        }

        let overflow = sortedByAge.count - maxStoredObjects
        guard overflow > 0 else { return }

        for i in 0..<overflow {
            try? FileManager.default.removeItem(at: sortedByAge[i])
        }
    }
}

// MARK: - Пример использования
/*
Task {
    do {
        let data = try await AnnouncementImageRepository.shared.data(
            for: attachmentId,
            authorId: PyrusServiceDesk.authorId ?? ""
        )
        print("Размер файла:", data.count)
    } catch {
        print("Ошибка:", error)
    }
}
*/
