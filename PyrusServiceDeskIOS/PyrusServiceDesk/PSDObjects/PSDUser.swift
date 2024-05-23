import UIKit

enum userType: String {
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
class PSDUser: NSObject, Codable {
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
    
    private enum CodingKeys: String, CodingKey {
        case personId, name, type, imagePath
    }
    
    required public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        personId = try values.decode(String.self, forKey: .personId)
        name = try values.decode(String.self, forKey: .name)
        imagePath = try values.decode(String.self, forKey: .imagePath)
        let typeString = try values.decode(String.self, forKey: .type)
        type = userType(rawValue: typeString)
    }
    func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(personId, forKey: .personId)
        try values.encode(name, forKey: .name)
        try values.encode(imagePath, forKey: .imagePath)
        try values.encode(type?.rawValue, forKey: .type)
    }
}
