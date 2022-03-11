
import UIKit

let DEFAULT_SUPPORT_ICON = UIImage.PSDImage(name: "SupportIcon")

protocol PSDSupportImageSetterDelegate: class {
    func reloadCells(with owner:PSDUser)
}
struct PSDSupportImageSetter {
    static func defaultSupportImage() -> UIImage? {
        let color: UIColor = defaultBackAvatarColor().isDarkColor ? .white : .black
        let image: UIImage?
        if let customAvatar = PyrusServiceDesk.mainController?.customization?.avatarForSupport {
            image = customAvatar
        } else{
            image = DEFAULT_SUPPORT_ICON?.imageWith(color: color) ?? DEFAULT_SUPPORT_ICON
        }
        return image
    }
    private static func defaultBackAvatarColor()->UIColor{
        if PyrusServiceDesk.mainController?.customization?.avatarForSupport != nil{
            return UIColor.psdBackground
        }
        return UIColor.appColor
    }
    static func clean(){
        loadingUsers.removeAll()
    }
    ///The array with loading images for users' avatars.
    ///For each user contain Bool flag, if it is true - for this user image is loading now, if false - already loaded or did not loading yet.
    private static var loadingUsers: [PSDUser:Bool] = [PSDUser:Bool]()
    ///Return image for support user. Or start load it if need
    static func setImage(for user: PSDUser, in imageView:UIImageView, delagate: PSDSupportImageSetterDelegate?) {
        if user as? PSDPlaceholderUser != nil {
            imageView.image = nil
            imageView.backgroundColor = UIColor.psdLightGray
            return
        }
        imageView.backgroundColor = defaultBackAvatarColor()
        imageView.image = DEFAULT_SUPPORT_IMAGE
        if (user.image) != nil{
            imageView.image = user.image!
        }
        else if user.imagePath != nil && (user.imagePath?.count)!>0 {
            if !(loadingUsers[user] ?? false) {
                loadingUsers[user] = true
                DispatchQueue.global().async {
                    [weak user, weak delagate] in
                    loadImage(for: user!, size: CGSize(width: AVATAR_SIZE, height: AVATAR_SIZE)){
                        (image : UIImage?) in
                        DispatchQueue.main.async {
                            if image != nil{
                                user?.image = image
                            }
                            if user != nil{
                                loadingUsers[user!]=false
                                delagate?.reloadCells(with: user!)
                            }
                            
                        }
                    }
                }
            }
            
        }
    }
    /**
     Load image from server .
     - parameter user: PSDUser who image need to be loaded
     - parameter size: Size of image that need to load
     - parameter image: Return image if it was loaded or nil if not
     */
    private static func loadImage(for user: PSDUser, size: CGSize,completion: @escaping (_ image: UIImage?) -> Void){
        if user.imagePath != nil && (user.imagePath?.count)!>0 {
            let url = PyrusServiceDeskAPI.PSDURL(type:.avatar, avatarId:user.imagePath!, size:size)
           PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
                guard let data = data, error == nil else{
                    completion(nil)
                    return
                }
                if data.count != 0{
                    let image = UIImage(data: data)
                    completion(image)
                }
                else{
                    completion(nil)
                }
            }.resume()
        }
    }
}
