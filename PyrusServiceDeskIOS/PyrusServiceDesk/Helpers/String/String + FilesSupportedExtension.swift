
import Foundation
enum FilesSupportedExtension : String,CaseIterable {
    case doc = ".doc"
    case docx = ".docx"
    case gif = ".gif"
    case jpeg = ".jpeg"
    case jpg = ".jpg"
    case pdf = ".pdf"
    case png = ".png"
    case tif = ".tif"
    case txt = ".txt"
    case xls = ".xls"
    case xlsx = ".xlsx"
    case heic = ".heic"
}
extension String {
    func isSupportedFileFormat() -> Bool {
        for i in FilesSupportedExtension.allCases{
            if self.lowercased().hasSuffix(i.rawValue.lowercased()){
                return true
            }
        }
        return false
    }
    func isImageFileFormat() -> Bool {
      if self.lowercased().hasSuffix(FilesSupportedExtension.jpeg.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.png.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.jpg.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.heic.rawValue.lowercased()){
            return true
        }
        return false
    }
}
