import UIKit
import Foundation
import ImageIO
extension UIImage {
    ///Take image from Pyrus Service Desk framework bundle. Return image, if no image - fatalError
    class func PSDImage(name: String) -> UIImage? {
        return UIImage(named: name, in: PSD_BUNDLE, compatibleWith: nil)
    }
    ///Take gif image from Pyrus Service Desk framework bundle. Return array with images, if no gif image - fatalError
    class func PSDGifImage(name: String) -> [UIImage]{
        var fullName = name
        if UIScreen.main.scale>1 {
            fullName = fullName+"@\(Int(UIScreen.main.scale))"+"x"
        }
        let url = PSD_BUNDLE.url(forResource: fullName, withExtension: "gif")
        guard let gifData = try? Data(contentsOf: url!),
            let source =  CGImageSourceCreateWithData(gifData as CFData, nil) else {
                fatalError("Missing MyImage...")
        }
        var images = [UIImage]()
        let imageCount = CGImageSourceGetCount(source)
        for i in 0 ..< imageCount {
            if let image = CGImageSourceCreateImageAtIndex(source, i, nil) {
                images.append(UIImage(cgImage: image))
            }
        }        
        return images
    }
}
