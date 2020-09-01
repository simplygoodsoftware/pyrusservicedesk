
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
private enum VideoExtension : String,CaseIterable {
    case mov = ".mov"
    case gt = ".gt"
    case avi = ".avi"
    case mp4 = ".mp4"
    case m4p = ".m4p"
    case m4v = ".m4v"
    case gpp = ".3gp"
    case gpp2 = ".3g2"
}
extension String {
    func isSupportedFileFormat() -> Bool {
        for i in FilesSupportedExtension.allCases{
            if self.lowercased().hasSuffix(i.rawValue.lowercased()){
                return true
            }
        }
        return isVideoFormat()
    }
    func isImageFileFormat() -> Bool {
      if self.lowercased().hasSuffix(FilesSupportedExtension.jpeg.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.png.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.jpg.rawValue.lowercased()) || self.lowercased().hasSuffix(FilesSupportedExtension.heic.rawValue.lowercased()){
            return true
        }
        return false
    }
    func isVideoFormat() -> Bool {
      for i in VideoExtension.allCases{
          if self.lowercased().hasSuffix(i.rawValue.lowercased()){
              return true
          }
      }
      return false
    }
}
