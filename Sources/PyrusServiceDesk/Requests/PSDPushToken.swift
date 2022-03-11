import Foundation
struct PSDPushToken {
    /**
     Send device token to server.
     - parameter token:String with devise Id
     - parameter success: Bool. true if token was send
     */
    static func send(_ token:String?, completion: @escaping(Error?) -> Void)
    {
        
        var  request : URLRequest
        var parameters = [String: Any]()
        if let token = token  {
            parameters["token"] = token
        } else {
            parameters["token"] = NSNull()
        }
        parameters["type"] = "ios"
        request = URLRequest.createRequest(type:.token, parameters: parameters)
        
        let task = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                // check for fundamental networking error
                completion(PSDError.init(description: "No internet connection"))
                return
            }
            
            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != 200 {           // check for http errors
                completion(PSDError.init(description: "Server error"))
                
            }
            else{
                do{
                    let resp = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String : Any] ?? [String: Any]()
                    if let error = resp["error"]{
                        completion(PSDError.init(description: "\(error)"))
                    }
                    else{
                        completion(nil)
                    }
                }catch{
                    completion(nil)
                }
            }
        }
        task.resume()
    }
}
