
import Foundation
let PSDImage_Size_Max : CGFloat = 1600 //pt
let IMAGE_COMPRESSION_QUALITY : CGFloat = 0.7
extension UIImage {
    
    //copress Data
    func compressImage() -> Data? {
        let data = self.imageResize(to: PSDImage_Size_Max).jpegData(compressionQuality:IMAGE_COMPRESSION_QUALITY)
        return data
    }
    
    ///Resize image
    ///- parameter newSize: the CGFloat value - maximum size of image. If image has less size return original image.
    func imageResize(to changeSize: CGFloat)->UIImage
    {
        if(self.size.width > changeSize || self.size.height > changeSize){
            var newSize :CGSize = self.size
            let p : CGFloat = min(changeSize/self.size.height, changeSize/self.size.width)
            newSize.width = self.size.width*p
            newSize.height = self.size.height*p
            UIGraphicsBeginImageContextWithOptions(newSize,false,1.0)
            self.draw(in: CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height))
            let newImage : UIImage = UIGraphicsGetImageFromCurrentImageContext() ?? self
            UIGraphicsEndImageContext()
            return newImage
        }
        return self
    }
}
