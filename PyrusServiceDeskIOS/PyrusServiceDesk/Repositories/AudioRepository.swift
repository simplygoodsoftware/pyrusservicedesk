import Foundation
import AVFoundation

protocol AudioRepositoryProtocol {
    func saveAudio(_ audioData: Data, name: String, id: String?) throws
    func audioExists(name: String, id: String?) -> Bool
    func loadAudio(name: String, id: String?) -> Data?
    func getAudioURL(name: String, id: String?) -> URL?
    func deleteAudio(name: String, id: String?) throws
    func clearRepository() throws
}

class AudioRepository: AudioRepositoryProtocol {
    private let fileManager = FileManager.default
    private let directoryURL: URL
    private let maxAudioFiles = 100
    
    init?() {
        guard let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        directoryURL = documentsDirectory.appendingPathComponent("AudioRepository")
        createDirectoryIfNeeded()
    }
    
    private func createDirectoryIfNeeded() {
        if !fileManager.fileExists(atPath: directoryURL.path) {
            do {
                try fileManager.createDirectory(at: directoryURL, withIntermediateDirectories: true, attributes: nil)
            } catch {
                print("Error creating directory: \(error)")
            }
        }
    }
    
    func audioExists(name: String, id: String?) -> Bool {
        guard let fileURL = getAudioFileURL(name: name, id: id) else {
            return false
        }
        return fileManager.fileExists(atPath: fileURL.path)
    }
    
    func saveAudio(_ audioData: Data, name: String, id: String?) throws {
        guard !audioExists(name: name, id: id) else { return }
        let folderURL = directoryURL.appendingPathComponent(id ?? "global")
        let fileURL = folderURL.appendingPathComponent(name)
        
        try createDirectoryIfNeeded(at: folderURL)
        try audioData.write(to: fileURL)
        
        try cleanupIfNeeded()
    }
    
    func loadAudio(name: String, id: String?) -> Data? {
        guard let fileURL = getAudioFileURL(name: name, id: id) else {
            return nil
        }
        return try? Data(contentsOf: fileURL)
    }
    
    func getAudioURL(name: String, id: String?) -> URL? {
        return getAudioFileURL(name: name, id: id)
    }
    
    func deleteAudio(name: String, id: String?) throws {
        guard let fileURL = getAudioFileURL(name: name, id: id) else {
            throw NSError(domain: "AudioRepository", code: 404, userInfo: [NSLocalizedDescriptionKey: "File not found"])
        }
        try fileManager.removeItem(at: fileURL)
    }
    
    private func getAudioFileURL(name: String, id: String?) -> URL? {
        let folderURL = directoryURL.appendingPathComponent(id ?? "global")
        return folderURL.appendingPathComponent(name)
    }
    
    private func createDirectoryIfNeeded(at url: URL) throws {
        if !fileManager.fileExists(atPath: url.path) {
            try fileManager.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)
        }
    }
    
    private func getAllAudioFiles() throws -> [URL] {
        let contents = try fileManager.contentsOfDirectory(at: directoryURL, includingPropertiesForKeys: [.isDirectoryKey], options: .skipsHiddenFiles)
        
        var audioFiles: [URL] = []
        
        for url in contents {
            if try url.resourceValues(forKeys: [.isDirectoryKey]).isDirectory == true {
                let folderContents = try fileManager.contentsOfDirectory(at: url, includingPropertiesForKeys: [.creationDateKey], options: .skipsHiddenFiles)
                audioFiles.append(contentsOf: folderContents)
            } else {
                audioFiles.append(url)
            }
        }
        
        return audioFiles.sorted {
            let date1 = try? $0.resourceValues(forKeys: [.creationDateKey]).creationDate ?? Date.distantPast
            let date2 = try? $1.resourceValues(forKeys: [.creationDateKey]).creationDate ?? Date.distantPast
            return date1! < date2!
        }
    }
    
    private func cleanupIfNeeded() throws {
        let allFiles = try getAllAudioFiles()
        if allFiles.count > maxAudioFiles {
            let filesToDelete = allFiles.prefix(allFiles.count - maxAudioFiles)
            for fileURL in filesToDelete {
                try fileManager.removeItem(at: fileURL)
            }
        }
    }
    
    func clearRepository() {        
        do {
            let contents = try fileManager.contentsOfDirectory(at: directoryURL, includingPropertiesForKeys: nil)
            for url in contents {
                try fileManager.removeItem(at: url)
            }
        } catch {
            print("failed cleaar audio repository: \(error)")
        }
    }
}
