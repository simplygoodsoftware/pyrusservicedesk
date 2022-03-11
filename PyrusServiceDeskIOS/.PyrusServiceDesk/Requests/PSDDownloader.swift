import Foundation
///Delegate for PSDDownloader. Pass messages to change progress, and about completion result.
protocol PSDDownloaderDelegate: class {
    /// pass the progress of download
    func changeProgress(to newValue:CGFloat)
    ///The downloading complete because of no internet connection
    func showNoConnection()
    ///The downloading is complete
    ///- parameter data : The data received from server or nil, if end with some error(canceled, timeout or server error)
    func ended(with data: Data?)
}
///Load data from server ("get"). Call remove to free.
class PSDDownloader: NSObject{
    static let timeout : Double = 60
    weak var delegate: PSDDownloaderDelegate?
    private var buffer: Data = Data()
    private var downloadsSession: URLSession?
    private var currentTask : URLSessionDataTask?
    var expectedSize : Int = 0
    private func  createSession() -> URLSession{
        let configuration = URLSessionConfiguration.default
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.urlCache = nil
        return URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue.main)
    }
    
    ///Load data from server
    ///- parameter url: URL to load file
    func load(from url: URL)
    {
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval:PSDDownloader.timeout)
        request.httpMethod = "GET"
        if downloadsSession == nil {
            downloadsSession = createSession()
        }
        currentTask = downloadsSession!.dataTask(with: request)
        currentTask?.resume()
    }
   
    func cancel(){
        buffer = Data()//clear data
        currentTask?.cancel()
    }
    func remove(){
        buffer = Data()//clear data
        downloadsSession?.invalidateAndCancel()
        downloadsSession = nil
    }
    
   
    
}
extension PSDDownloader :URLSessionTaskDelegate, URLSessionDelegate, URLSessionDataDelegate{
    //MARK: deleagte methods
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        
        buffer.append(data)
        let percentageDownloaded = Float(buffer.count) / Float(expectedSize)
        self.delegate?.changeProgress(to: CGFloat(percentageDownloaded))
    }
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        self.delegate?.changeProgress(to: 1.0)
        if(error == nil){
            self.delegate?.ended(with: buffer)
        }
        else{
            if let error = error as NSError?, error.domain == NSURLErrorDomain && error.code == NSURLErrorNotConnectedToInternet {
                self.delegate?.showNoConnection()
            }
            else{
                self.delegate?.ended(with: nil)
            }
            
        }
    }
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, willCacheResponse proposedResponse: CachedURLResponse, completionHandler: @escaping (CachedURLResponse?) -> Void) {
        completionHandler(nil)
    }
}
