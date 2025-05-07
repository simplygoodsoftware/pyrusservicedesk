
import Foundation
protocol PSDPreviewSetterDelegate : class{
    func reloadCells(with attachmentId: String)
}
struct PSDPreviewSetter {
    ///The download state of preview
    private enum downloadState{
        case loading//file is download now
        case loaded//file was download
        case needLoad // file is not downloaded and need to download
    }
    
    private static let imageRepository = ImageRepository()
    private static let audioRepository = AudioRepository()
    ///Load preview of PSDAttachment and set it to PSDAttachmentView
    ///- parameter attachment: PSDAttachment which preview need to be loaded
    ///- parameter localId: the local id of message
    ///- parameter attachmentView: view to set loaded image if it is.
    static func setPreview(of attachment:PSDAttachment?, in attachmentView: PSDAttachmentView?, delegate: PSDPreviewSetterDelegate?, animated: Bool){
        setPreview(of: attachment, in: attachmentView, delegate: delegate, animated: animated, startAfter: 0)
    }
    private static func setPreview(of attachment:PSDAttachment?, in attachmentView: PSDAttachmentView?, delegate: PSDPreviewSetterDelegate?, animated: Bool, startAfter:Int){
         DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(startAfter), execute: { [weak imageRepository] in
            guard let attachment = attachment, let attachmentView = attachmentView else{
                return
            }
             
             if attachment.isAudio {
                 (attachmentView as? PSDAudioAttachmentView)?.changeLoadingProggress(0.5)
                 if !(audioRepository?.audioExists(name: attachment.name, id: attachment.serverIdentifer) ?? false) {
                     if Int(attachment.serverIdentifer ?? "") == nil {
                         (attachmentView as? PSDAudioAttachmentView)?.presenter?.update(
                            fileUrl: URL(fileURLWithPath: attachment.localPath ?? ""),
                            attachmentId: attachment.serverIdentifer ?? ""
                         )
                         (attachmentView as? PSDAudioAttachmentView)?.changeLoadingProggress(1)
                             (attachmentView as? PSDAudioAttachmentView)?.changeState(.stopped)
                     }
                     
                     if !attachment.emptyId(){
                         let url = PyrusServiceDeskAPI.PSDURL(type: .download, ticketId: attachment.serverIdentifer!)
                         PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
                             if let data, data.count != 0 {
                                 do {
                                     try audioRepository?.saveAudio(data, name: attachment.name, id: attachment.serverIdentifer!)
                                     (attachmentView as? PSDAudioAttachmentView)?.changeLoadingProggress(1)
                                     DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                         (attachmentView as? PSDAudioAttachmentView)?.changeState(.stopped)
                                     }
                                     
                                 } catch {
                                     
                                 }
                                 
                             }
                         }.resume()
                     }
                 } else {
                     (attachmentView as? PSDAudioAttachmentView)?.changeLoadingProggress(1)
                     (attachmentView as? PSDAudioAttachmentView)?.changeState(.stopped)
                 }
             }
            if let previewImage = attachment.previewImage {
                if (attachmentView.previewImage != previewImage) {//if its doesnt set yet - set image
                    attachmentView.setPreviewImage(previewImage, animated: animated)
                }
            } else if let previewImage = imageRepository?.loadImage(name: attachment.name, id: attachment.serverIdentifer, type: .image) {
                if attachmentView.previewImage != previewImage {//if its doesnt set yet - set image
                    attachmentView.setPreviewImage(previewImage, animated: animated)
                }
            }
            else if (loadingAttachments[attachment.localId] == .needLoad || loadingAttachments[attachment.localId] == nil){
                loadingAttachments[attachment.localId] = .loading
                DispatchQueue.global().async {
                    [weak attachment, weak attachmentView, weak delegate] in
                    guard let attachment = attachment else{
                        return
                    }
                    loadPreview(of: attachment){
                        (image : UIImage?, retryAfter:Bool) in
                        DispatchQueue.main.async { //[weak imageRepository] in
                            if let image {
                                loadingAttachments[attachment.localId] = .loaded
                                attachment.previewImage = image
                               // imageRepository?.saveImage(image, name: attachment.name, type: .image)
                                attachmentView?.setPreviewImage(image, animated: true)//set image with animation
                                DispatchQueue.main.async {delegate?.reloadCells(with: attachment.localId)}
                            }else{
                                loadingAttachments[attachment.localId] = attachment.isImage ? .needLoad : .loaded
                                if(retryAfter && loadingAttachments.count != 0){
                                    setPreview(of: attachment, in: attachmentView, delegate: delegate, animated: animated, startAfter:retryInterval(with: startAfter))
                                }
                            }
                        }
                    }
                }
            }
        })
        
    }
    ///The array with loading attachment for users' avatars.
    ///For each message's (with attachment) local id  contain downloadState
    private static var loadingAttachments: [String:downloadState] = [String:downloadState]()
    ///Clean all info about loading
    static func clean(){
        loadingAttachments.removeAll()
    }
    /**
     Load image from server .
     - parameter attachment: PSDAttachment which image need to be loaded
     - parameter size: Size of image that need to load
     - parameter image: Return image if it was loaded or nil if not
     - parameter image: Return image if it was loaded or nil if not
     - parameter reloadAfter: Float value incate after what time need to retry loading. Pass nil if dont need to retry.
     */
    private static func loadPreview(of attachment: PSDAttachment,completion: @escaping (_ image: UIImage?, _ retryAfter: Bool) -> Void){
        if !attachment.emptyId(){
            let url = PyrusServiceDeskAPI.PSDURL(type: .download, ticketId: attachment.serverIdentifer!)
            PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
                guard let data = data, error == nil else{
                    //connection error
                    completion(nil,true)
                    return
                }
                if data.count != 0{
                    let image = UIImage(data: data)
                    
                    completion(image,false)
                }
                else{
                    completion(nil,false)
                }
                }.resume()
        }
    }
    private static func retryInterval(with step:Int)->Int{
        var newStep = max(10,step)
        newStep = newStep*2
        return max(step,60)
    }
}

