//https://stackoverflow.com/questions/26162616/upload-image-with-parameters-in-swift/26163136#26163136
import Foundation
import MobileCoreServices

let contenttype = "application/json; charset=utf-8"
private let authTokenKey = "Authorization"
private let requestTimeout: TimeInterval = 60
extension URLRequest {
    /**
     Create URLRequest with RequestType that don't need any id.
     - Parameter parameters: [String: Any] is an additional parameters to "AppId" and "UserId". If no need in additional parameters send nil.
     */
    static func createRequest(type: urlType, parameters:[String: Any]) -> URLRequest? {
        if(type == .upload){
            fatalError("Bad type in this method")
        }
        let url = PyrusServiceDeskAPI.PSDURL(type:type)
        return createRequest(url:url, json:parameters)
    }
    
    /**
     Create URLRequest with a Codable body.
     
     Тело кодируется как есть, без добавления статических ключей
     (`addStaticKeys`): по спеке HelpySync корневые `app_id`, `user_id`,
     `last_note_id` и `security_key` в запрос не передаются, а `instance_id`,
     `locale` и `version` модель запроса несёт сама.
     */
    static func createRequest<Body: Encodable>(
        type: urlType,
        body: Body,
        encoder: JSONEncoder = JSONEncoder()
    ) -> URLRequest? {
        if type == .upload {
            fatalError("Bad type in this method")
        }
        guard PyrusServiceDesk.clientId != nil || PyrusServiceDesk.multichats else {
            EventsLogger.logEvent(.emptyClientId)
            return nil
        }
        let jsonData: Data
        do {
            jsonData = try encoder.encode(body)
        } catch {
            PyrusLogger.shared.logEvent("Failed to encode request body for \(type.rawValue): \(error)")
            return nil
        }
        let url = PyrusServiceDeskAPI.PSDURL(type: type)
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: requestTimeout)
        request.httpMethod = "POST"
        request.httpBody = jsonData
        request.addValue(contenttype, forHTTPHeaderField: "content-type")
        request.addValue("\(jsonData.count)", forHTTPHeaderField: "Content-Length")
        request.addCustomHeaders()
        request.addUserAgent()
        return request
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
        request.addCustomHeaders()
        request.httpMethod = "POST"
        request.setValue("Keep-Alive", forHTTPHeaderField: "Connection")
        return request
    }
    
    private static func createRequest(url: URL, json:[String: Any]) -> URLRequest? {
        guard let body = addStaticKeys(to: json) else {
            return nil
        }
        guard let jsonData = try? JSONSerialization.data(withJSONObject: body) else {
            return nil
        }
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: requestTimeout)
        request.httpMethod = "POST"
        request.httpBody = jsonData
        request.addValue(contenttype, forHTTPHeaderField: "content-type")
        request.addValue("\(jsonData.count)", forHTTPHeaderField: "Content-Length")
        request.addCustomHeaders()
        request.addUserAgent()
        return request
    }
    
    private static func addStaticKeys(to JSON: [String: Any]) -> [String: Any]? {
        guard PyrusServiceDesk.clientId != nil || PyrusServiceDesk.multichats else {
            EventsLogger.logEvent(.emptyClientId)
            return nil
        }
        var fullJSON = JSON
        fullJSON["locale"] = Locale.current.languageCode ?? "en"
        fullJSON["instance_id"] = PyrusServiceDesk.userId
        fullJSON["version"] = 2

        guard !PyrusServiceDesk.multichats else { return fullJSON }

        fullJSON["app_id"] = PyrusServiceDesk.clientId
        if let customUserId = PyrusServiceDesk.customUserId {
            fullJSON["user_id"] = customUserId
            if let securityKey = PyrusServiceDesk.securityKey {
                fullJSON["security_key"] = securityKey
            }
        }
        return fullJSON
    }
    
    mutating func addCustomHeaders() {
        guard let auth = PyrusServiceDesk.authorizationToken else {
            return
        }
        addValue(auth, forHTTPHeaderField: authTokenKey)
    }
    
    mutating func addUserAgent() {
        let sdkVersion = PyrusServiceDesk.PSD_VERSION
        let appId = (String(PyrusServiceDesk.clientId?.prefix(10) ?? ""))
        let systemVersion = UIDevice.current.systemVersion
        let isMultichats = PyrusServiceDesk.multichats ? "1" : "0"
        let userAgent = "ServiceDesk/ios/\(sdkVersion)/\(appId)/\(systemVersion)/\(isMultichats)"
        addValue(userAgent, forHTTPHeaderField: "User-Agent")
    }
}
