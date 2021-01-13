import Foundation
struct PSDFilesManager {
    private static let PSD_FILE_PATH = "PyrusServiceDesk"
    
    ///Save attachment to document directory
    ///Returns true on succes
    static func saveAttchment(_ attachment: PSDAttachment, forMessageWith localId: String) -> Bool {
        let url  = attachment.data.dataToFile(fileName: attachment.name, messageLocalId: localId)
        return url != nil
    }
    static func removeAttachment(_ attachment: PSDAttachment, forMessageWith localId: String){
        removeLocalFile(fileName: attachment.name, messageLocalId: localId)
    }
    static func getAtttachment(_ fileName: String, messageLocalId: String) -> PSDAttachment? {
        guard fileName.count > 0, messageLocalId.count > 0 else {
            return nil
        }
        let url = localURL(fileName: fileName,messageLocalId: messageLocalId)
        guard let data = dataFrom(url: url) else{
            return nil
        }
        return PSDAttachment(localPath: url.path, data: data, serverIdentifer: nil)
    }
    ///Return local url by its name
    static func localURL (fileName: String, messageLocalId: String) -> URL{
        let messagePath = getDocumentsDirectory()
        let filePath = messagePath.appendingPathComponent(messageLocalId.lowercased() + "_" + fileName, isDirectory: false)
        return filePath
    }
    ///Return local document path
    static func getDocumentsDirectory() -> URL {
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        let documentsDirectory = URL(fileURLWithPath: paths.last ?? "")
        return documentsDirectory.appendingPathComponent(PSD_FILE_PATH, isDirectory: true)
    }
    ///Return data from file placed local path by its name
    private static func dataFromLocalURL(fileName: String, messageLocalId: String)->Data?{
        let url = localURL(fileName: fileName,messageLocalId: messageLocalId)
        return dataFrom(url: url)
    }
    ///Return data from file placed in url
    private static func dataFrom(url: URL) -> Data? {
        do {
            let data: Data = try Data(contentsOf: url)
            return data
        } catch {
        }
        return nil
    }
    ///Removing local file by its name
    static func removeLocalFile(fileName: String,messageLocalId: String) {
        let url = localURL(fileName:fileName,messageLocalId: messageLocalId)
        removeFile(url:url)
    }
    ///Removing file in url
    static func removeFile(url: URL) {
        DispatchQueue.global().async {
            do {
                try FileManager.default.removeItem(at: url)
            } catch {
                PyrusLogger.shared.logEvent("Error remove file = \(("Error removing file \(error)"))")
            }
        }
       
    }
}
