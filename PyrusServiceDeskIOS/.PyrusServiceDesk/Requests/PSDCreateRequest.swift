//https://stackoverflow.com/questions/26162616/upload-image-with-parameters-in-swift/26163136#26163136
import Foundation
import MobileCoreServices

let contenttype = "application/json; charset=utf-8"
extension URLRequest {
    /**
     Create URLRequest with RequestType that don't need any id.
     - Parameter parameters: [String: Any] is an additional parameters to "AppId" and "UserId". If no need in additional parameters send nil.
     */
    static func createRequest(type: urlType, parameters:[String: Any]) -> URLRequest{
        if(type == .upload){
            fatalError("Bad type in this method")
        }
        let url = PyrusServiceDeskAPI.PSDURL(type:type)
        return createRequest(url:url, json:parameters)
    }
    
    /**
     Create URLRequest with RequestType .upload
     */
    static func createUploadRequest() -> URLRequest{
        let url = PyrusServiceDeskAPI.PSDURL(type:.upload)
        return createUploadRequest(url:url)
    }
    
    private static func createUploadRequest(url: URL) -> URLRequest {
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: PSDDownloader.timeout)
        request.httpMethod = "POST"
        request.setValue("Keep-Alive", forHTTPHeaderField: "Connection")
        return request
    }
    
    private static func createRequest(url: URL, json:[String: Any]) -> URLRequest {
        let body = addStaticKeys(to: json)
        let jsonData = try? JSONSerialization.data(withJSONObject: body)
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 60)
        request.httpMethod = "POST"
        request.httpBody = jsonData
        request.addValue(contenttype, forHTTPHeaderField: "content-type")
        request.addValue("\(jsonData!.count)", forHTTPHeaderField: "Content-Length")       
        return request
    }
    private static func addStaticKeys(to JSON:[String: Any]) -> [String: Any]
    {
        var fullJSOn = JSON
        fullJSOn["app_id"] = PyrusServiceDesk.clientId
        if let securityKey = PyrusServiceDesk.securityKey, let customUserId = PyrusServiceDesk.customUserId {
            fullJSOn["user_id"] = customUserId
            fullJSOn["security_key"] = securityKey
            fullJSOn["instance_id"] = PyrusServiceDesk.userId
            fullJSOn["version"] = 2
        } else {
            fullJSOn["user_id"] = PyrusServiceDesk.userId
        }
        if((PyrusServiceDesk.clientId) == nil){
            fatalError("no client Id")
        }
        return fullJSOn
    }
}

