
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
        do {
            try data.write(to: localURL(fileName: fileName, messageLocalId: messageLocalId))
            return localURL(fileName: fileName, messageLocalId: messageLocalId) as NSURL
            
        } catch {
            //print("Error writing the file: \(error.localizedDescription)")
        }
        return nil
    }
}
///Return local url by its name
func localURL (fileName: String, messageLocalId: String) -> URL{
    let createdFileName : String = messageLocalId  + "_" + fileName 
    let filePath = getDocumentsDirectory().appendingPathComponent(createdFileName)
    return URL(fileURLWithPath: filePath)
}
///Return local document path
private func getDocumentsDirectory() -> NSString {
    let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
    let documentsDirectory = paths[0]
    return documentsDirectory as NSString
}
///Return data from file placed local path by its name
func dataFromLocalURL(fileName: String, messageLocalId: String)->Data?{
    let url = localURL(fileName:fileName,messageLocalId: messageLocalId)
    return dataFrom(url:url)
}
///Return data from file placed in url
private func dataFrom(url:URL)->Data?{
    do {
        let data : Data = try Data(contentsOf: url)
        return data
    } catch {
    }
    return nil
}
///Removing local file by its name
func removeLocalFile(fileName:String,messageLocalId: String){
    let url = localURL(fileName:fileName,messageLocalId: messageLocalId)
    removeFile(url:url)
}
///Removing file in url
func removeFile(url:URL){
    DispatchQueue.global().async {
        do {
            try FileManager.default.removeItem(at: url)
        } catch _ as NSError {
            //print("Error removing file")
        }
    }
   
}
