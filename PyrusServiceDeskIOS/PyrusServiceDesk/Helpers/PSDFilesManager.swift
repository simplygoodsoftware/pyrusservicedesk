import Foundation
struct PSDFilesManager {
    static func saveAttchment(_ attachment: PSDAttachment, forMessageWith localId: String){
        guard let data = attachment.data else {
            return
        }
        data.dataToFile(attachment.name, messageLocalId: localId)
    }
    static func removeAttachment(_ attachment: PSDAttachment, forMessageWith localId: String){
        removeLocalFile(fileName: attachment.name, messageLocalId: localId)
    }
    static func getAtttachment(_ name: String: messageLocalId: String) -> PSDAttachment? {
        guard name.count > 0, messageLocalId.count > 0{
            return nil
        }
        let url = localURL(fileName:fileName,messageLocalId: messageLocalId)
        guard let data = dataFrom(url: url) else{
            return nil
        }
        return PSDAttachment(localPath: url.path, data: data, serverIdentifer: nil)
    }
}
