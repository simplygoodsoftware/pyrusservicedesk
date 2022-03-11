import UIKit

enum userType : String{
    case user
    case support
}
class PSDUsers: NSObject {
    static var supportUsers : [PSDUser] = []
    static var user : PSDUser!
    static func add(user:PSDUser)
    {
        if user.personId == PyrusServiceDesk.userId{
            PSDUsers.user = user
        }else{
            PSDUsers.supportUsers.append(user)
        }
    }
    ///Chek if PSDUsers contain user with same data, if true - return it,if not - create new, add it to exist and return.
    static func supportUsersContain(name:String, imagePath: String)->PSDUser {
        for support in supportUsers{
            if support.name == name && support.imagePath == imagePath{
                return support
            }
        }
        let newUser = PSDUser(personId: "0", name: name, type: .support, imagePath: imagePath)
        add(user: newUser)
        return newUser
    }
}
///
let DEFAULT_SUPPORT_IMAGE = PSDSupportImageSetter.defaultSupportImage()
class PSDUser: NSObject {
    //user saved in PyrusServiceDeskCreator is not denited
    ///Has only not support user
    var personId: String?
    
    var name: String?
    var type: userType?
    
    ///Has only support users
    var imagePath: String?
    ///Has only support users
    var image: UIImage?
    
    init(personId: String, name:String, type:userType, imagePath:String)  {
        self.personId = personId
        self.name = name
        self.type = type
        self.imagePath = imagePath
    }
    func equalTo(user:PSDUser?)->Bool{
        guard let user = user else{
            return false
        }
        if PyrusServiceDesk.userId == user.personId && user.personId == self.personId {
            return true
        }
        if self.name == user.name && self.imagePath == user.imagePath{
            return true
        }
        
        return false
    }
}
