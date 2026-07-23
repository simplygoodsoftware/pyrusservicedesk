import Foundation
import UIKit
import ImageIO
import AVFoundation

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
        guard var components = URLComponents(string: baseURLString) else {
            throw URLError(.badURL)
        }
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
        // Caches-директория существует всегда, но force unwrap здесь не нужен:
        // fallback на temporaryDirectory безопасен для кеша.
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
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

// MARK: - Декодер превью


/// Даунсэмплинг картинок для превью в гриде объявлений.
/// Полноразмерный декод через UIImage(data:) происходит лениво на главном
/// потоке при первом рендере — для длинных лент это гарантированные фризы.
enum AnnouncementImageDecoder {

    /// Максимальный размер миниатюры для сетки объявлений (в поинтах).
    static let gridThumbnailMaxDimension: CGFloat = 500

    /// Возвращает картинку, ужатую до `maxDimension` поинтов по большей стороне.
    /// Декод выполняется на фоновой очереди.
    /// - Parameter scale: масштаб экрана вызывающей стороны. Передаётся снаружи
    ///   намеренно: чтение UIScreen.main.scale здесь добавляло прыжок на main actor
    ///   на каждую загрузку и подёргивало скролл при пачке одновременных декодов.
    static func downsampledImage(from data: Data, maxDimension: CGFloat, scale: CGFloat) async -> UIImage? {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                continuation.resume(returning: downsample(data: data, maxDimension: maxDimension, scale: scale))
            }
        }
    }

    private static func downsample(data: Data, maxDimension: CGFloat, scale: CGFloat) -> UIImage? {
        let sourceOptions: [CFString: Any] = [kCGImageSourceShouldCache: false]
        guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions as CFDictionary) else {
            return UIImage(data: data)
        }

        let thumbnailOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true, // декод здесь, а не при рендере
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxDimension * scale
        ]
        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, thumbnailOptions as CFDictionary) else {
            return UIImage(data: data)
        }
        return UIImage(cgImage: cgImage, scale: scale, orientation: .up)
    }
}

// MARK: - Кеш миниатюр

/// Кеш готовых (декодированных) миниатюр по id вложения — фото и кадры видео.
/// Повторный показ идёт синхронно из памяти: без задач и без моргания плейсхолдером.
final class AnnouncementThumbnailCache {
    static let shared = AnnouncementThumbnailCache()

    private enum Constants {
        static let countLimit = 300
    }

    private let cache = NSCache<NSString, UIImage>()

    private init() {
        cache.countLimit = Constants.countLimit
    }

    func image(for attachmentId: String) -> UIImage? {
        cache.object(forKey: attachmentId as NSString)
    }

    func set(_ image: UIImage, for attachmentId: String) {
        cache.setObject(image, forKey: attachmentId as NSString)
    }
}

// MARK: - Кадр из видео


extension AnnouncementImageDecoder {

    /// Генерирует миниатюру (кадр) видео. Выполняется на фоновой очереди.
    /// AVFoundation читает только из файла, поэтому данные пишутся во временный
    /// файл и удаляются сразу после генерации.
    /// - Parameter scale: масштаб экрана вызывающей стороны (см. downsampledImage).
    static func videoThumbnail(from data: Data, fileName: String?, maxDimension: CGFloat, scale: CGFloat) async -> UIImage? {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                continuation.resume(
                    returning: makeVideoThumbnail(data: data, fileName: fileName, maxDimension: maxDimension, scale: scale)
                )
            }
        }
    }

    private static func makeVideoThumbnail(data: Data, fileName: String?, maxDimension: CGFloat, scale: CGFloat) -> UIImage? {
        let fileExtension = ((fileName as NSString?)?.pathExtension)
            .flatMap { $0.isEmpty ? nil : $0 } ?? "mp4"
        let temporaryURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(fileExtension)

        do {
            try data.write(to: temporaryURL)
        } catch {
            return nil
        }
        defer { try? FileManager.default.removeItem(at: temporaryURL) }

        let asset = AVURLAsset(url: temporaryURL)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: maxDimension * scale, height: maxDimension * scale)

        // Первый кадр часто чёрный — берём чуть дальше нуля,
        // с фолбэком на нулевой для совсем коротких роликов.
        let preferredTime = CMTime(seconds: 0.1, preferredTimescale: 600)
        for time in [preferredTime, .zero] {
            if let cgImage = try? generator.copyCGImage(at: time, actualTime: nil) {
                return UIImage(cgImage: cgImage, scale: scale, orientation: .up)
            }
        }
        return nil
    }
}

// MARK: - Префетчинг миниатюр

/// Прогрев миниатюр для рядов, которые вот-вот появятся на экране.
/// Загруженная и декодированная заранее миниатюра попадает в
/// AnnouncementThumbnailCache — ячейка покажет её мгновенно, без задачи
/// в момент скролла. Это снимает основную нагрузку «появления картинок»
/// с кадров прокрутки.
enum AnnouncementThumbnailPrefetcher {

    static func warm(attachments: [PSDAnnouncementAttachment], scale: CGFloat) async {
        for attachment in attachments {
            guard !Task.isCancelled else { return }
            guard AnnouncementThumbnailCache.shared.image(for: attachment.id) == nil else { continue }

            guard let data = try? await AnnouncementAttachmentsRepository.shared.data(
                for: attachment.id,
                authorId: PyrusServiceDesk.authorId ?? ""
            ) else { continue }
            guard !Task.isCancelled else { return }

            let image: UIImage?
            if attachment.isVideo {
                image = await AnnouncementImageDecoder.videoThumbnail(
                    from: data,
                    fileName: attachment.name,
                    maxDimension: AnnouncementImageDecoder.gridThumbnailMaxDimension,
                    scale: scale
                )
            } else {
                image = await AnnouncementImageDecoder.downsampledImage(
                    from: data,
                    maxDimension: AnnouncementImageDecoder.gridThumbnailMaxDimension,
                    scale: scale
                )
            }
            if let image {
                AnnouncementThumbnailCache.shared.set(image, for: attachment.id)
            }
        }
    }
}
