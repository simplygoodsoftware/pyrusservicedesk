import Foundation

///Type of url to automatic url generation
enum urlType : String{
    ///Add gettickets to baseURLString. Use PSDURL(type:urlType).
    case chats = "gettickets"
    ///Add getticket/{ticketid} to baseURLString. Use PSDURL(type:urlType,ticketId:String).
    case chat = "getticket"
    ///Add GetTicketFeed to baseURLString. Use PSDURL(type:urlType).
    case chatFeed = "GetTicketFeed"
    ///Add CreateTicket to baseURLString. Use PSDURL(type:urlType).
    case createNew = "CreateTicket"
    ///Add UpdateTicket/{ticketid} to baseURLString. Use PSDURL(type:urlType,ticketId:String).
    case update = "UpdateTicket"
    ///Add UpdateTicketFeed to baseURLString. Use PSDURL(type:urlType).
    case updateFeed = "UpdateTicketFeed"
    ///Add UploadFile to baseURLString. Use PSDURL(type:urlType).
    case upload = "UploadFile"
    ///Add DownloadFile/{file_id} to baseURLString. Use PSDURL(type:urlType,ticketId:String).
    case download = "DownloadFile"
    ///Add avatar/{avatar_id}/{size} to baseURLString, pass any size, it will be converted to server-supported. Use PSDURL(type:urlType,avatarId:String, size:CGSize).
    case avatar = "avatar"
    ///Add SetPushToken to baseURLString. Use PSDURL(type:urlType).
    case token = "SetPushToken"
    ///Add DownloadFilePriview/{file_id} to baseURLString. Use PSDURL(type:urlType,ticketId:String).
    case downloadPreview = "DownloadFilePriview"
}

struct PyrusServiceDeskAPI {
    private static let baseURLString = "https://pyrus.com/servicedeskapi/v1/"//dev.
    ///Create URL for urlType in [.chats, .createNew, .upload, .chatFeed, .updateFeed, .token]
    static func PSDURL(type:urlType)->URL{
        let validTypes : [urlType] = [.chats, .createNew, .upload, .chatFeed, .updateFeed, .token]
        if !(validTypes.contains(type)){
            fatalError("Bad urlType for this function, type = \(urlType.RawValue())")
        }
        return PSDURL(type:type,ticketId:"0")
    }
    ///Create URL all urlType exept avatar
    static func PSDURL(type:urlType,ticketId:String)->URL{
        if type == .avatar{
            fatalError("Bad urlType for this function")
        }
        var urlString : String = "\(baseURLString)" + type.rawValue

        if type == .download || type == .downloadPreview{
            let userIdEncodeed : String = PyrusServiceDesk.userId.addingPercentEncoding(withAllowedCharacters: NSCharacterSet.urlHostAllowed) ?? PyrusServiceDesk.userId
            let appIdEncodeed : String = PyrusServiceDesk.clientId?.addingPercentEncoding(withAllowedCharacters: CharacterSet.rfc3986Unreserved) ?? PyrusServiceDesk.clientId ?? ""
            urlString = urlString + "/" + ticketId + "?user_id=" + userIdEncodeed + "&app_id=" + appIdEncodeed
            
        }
        else{
             urlString = ticketId != "0" ? "\(urlString)/\(ticketId)/":urlString
        }
        let components = NSURLComponents(string: urlString)
        return (components?.url)!
    }
    ///Create URL for urlType in [.avatar]
    static func PSDURL(type:urlType,avatarId:String, size:CGSize)->URL{
        if type != .avatar{
            fatalError("Bad urlType for this function")
        }
        var urlString : String = "\(baseURLString)" + type.rawValue
        urlString = "\(urlString)/\(avatarId)/\(validSize(from: size))"
        let components = NSURLComponents(string: urlString)
        return (components?.url)!
    }
    private static func validSize(from size: CGSize)->String{
        let validSizes : [Int] = [16, 30, 32, 40, 48, 60, 64, 96, 90, 120, 128, 180, 240, 512]
        let maxValue : Int = (Int(max(size.width, size.height)))*2
        var i=0
        for currentSize in validSizes{
            let previousSize = i-1>0 ? validSizes[i-1] : 0
            i=i+1
            if maxValue>previousSize && maxValue<=currentSize{
                return "\(currentSize)"
            }
        }
        return "512"
        
    }
    
}
