
import Foundation
extension Data {
    
    /// Append string to Data
    /// - parameter string:  The string to be added to the `Data`.
    mutating func append(_ string: String, using encoding: String.Encoding = .utf8){
        if let data = string.data(using: encoding) {
            append(data)
        }
    }
    ///Write data to file
    func dataToFile(fileName: String, messageLocalId: String) -> NSURL? {
        let data = self
        let url = PSDFilesManager.localURL(fileName: fileName, messageLocalId: messageLocalId)
        let docUrl = PSDFilesManager.getDocumentsDirectory()
        if !FileManager.default.fileExists(atPath: docUrl.path){
            do {
                try FileManager.default.createDirectory(at: docUrl, withIntermediateDirectories: true, attributes: nil)
            }catch {
                print("Error create folder: \(error.localizedDescription)")
            }
        }
        do {
            try data.write(to: url, options: .atomic)
            return url as NSURL
            
        } catch {
            print("Error writing the file: \(error.localizedDescription), path = \(url.absoluteString), \(error.self)")
        }
        return nil
    }
}
