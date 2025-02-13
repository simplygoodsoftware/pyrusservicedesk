
import Foundation
import UIKit

enum ImageType {
    case image, clientIcon
}

protocol ImageRepositoryProtocol {
    func saveImage(_ image: UIImage, name: String, type: ImageType)
    func loadImage(name: String, type: ImageType) -> UIImage? 
}

class ImageRepository: ImageRepositoryProtocol {
    private let fileManager = FileManager.default
    private let directoryURL: URL
    private let clientsDirectoryURL: URL
    private let maxImages = 20

    init?() {
        guard let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        directoryURL = documentsDirectory.appendingPathComponent("ImageRepository")
        clientsDirectoryURL = documentsDirectory.appendingPathComponent("ClientsImageRepository")
        
        if !fileManager.fileExists(atPath: directoryURL.path) {
            do {
                try fileManager.createDirectory(at: directoryURL, withIntermediateDirectories: true, attributes: nil)
            } catch {
                print("Ошибка при создании папки: \(error)")
                return nil
            }
        }
        
        if !fileManager.fileExists(atPath: clientsDirectoryURL.path) {
            do {
                try fileManager.createDirectory(at: clientsDirectoryURL, withIntermediateDirectories: true, attributes: nil)
            } catch {
                print("Ошибка при создании папки: \(error)")
                return nil
            }
        }
    }

    func saveImage(_ image: UIImage, name: String, type: ImageType) {
        let url = type == .image ? directoryURL : clientsDirectoryURL
        saveImage(image, name: name, directoryURL: url)
    }
    
    func loadImage(name: String, type: ImageType) -> UIImage? {
        let url = type == .image ? directoryURL : clientsDirectoryURL
        return loadImage(named: name, directoryURL: url)
    }
    
    func saveImage(_ image: UIImage, name: String, directoryURL: URL) {
        DispatchQueue.global().async { [weak self] in
            guard let self else { return }
            guard let imageData = image.jpegData(compressionQuality: 0.9) else { return }
            
            let timestamp = Date().timeIntervalSince1970
            let fileName = name
            let fileURL = directoryURL.appendingPathComponent(fileName)
            
            do {
                try imageData.write(to: fileURL)
                cleanupIfNeeded()
            } catch {
                print("Ошибка при сохранении изображения: \(error)")
            }
        }
    }
    
    func loadImage(named fileName: String, directoryURL: URL) -> UIImage? {
        let fileURL = directoryURL.appendingPathComponent(fileName)
        guard let imageData = try? Data(contentsOf: fileURL) else { return nil }
        return UIImage(data: imageData)
    }

    func getAllImageFiles() -> [URL] {
        do {
            let files = try fileManager.contentsOfDirectory(at: directoryURL, includingPropertiesForKeys: [.creationDateKey], options: .skipsHiddenFiles)
            return files.sorted { (url1, url2) -> Bool in
                let date1 = (try? url1.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                let date2 = (try? url2.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                return date1 < date2
            }
        } catch {
            print("Ошибка при получении списка файлов: \(error)")
            return []
        }
    }
    
    private func cleanupIfNeeded() {
        let allFiles = getAllImageFiles()
        if allFiles.count > maxImages {
            let filesToDelete = allFiles.prefix(allFiles.count - maxImages)
            for fileURL in filesToDelete {
                do {
                    try fileManager.removeItem(at: fileURL)
                } catch {
                    print("Ошибка при удалении файла \(fileURL): \(error)")
                }
            }
        }
    }
}
