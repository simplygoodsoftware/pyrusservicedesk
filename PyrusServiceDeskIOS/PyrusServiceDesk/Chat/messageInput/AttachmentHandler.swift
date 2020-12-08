
import Foundation
import Photos
import AVFoundation
import MobileCoreServices
/*
 Use example
 AttachmentHandler.shared.showAttachmentActionSheet(vc: self, sourseView:nil)
 AttachmentHandler.shared.attachmentPickedBlock = { (data, url) in
 /* get your image here */
 }
 */

class AttachmentHandler: NSObject,UIImagePickerControllerDelegate, UINavigationControllerDelegate,FileChooserDelegate{
    
    static let shared = AttachmentHandler()
    private var logsSendController: LogsSendController?
    enum AttachmentType: String{
        case camera, gallery, customChooser, localLogs
    }
    //https://medium.com/@deepakrajmurugesan/swift-access-ios-camera-photo-library-video-and-file-from-user-device-6a7fd66beca2
    
    func showAttachmentActionSheet( _ viewController: UIViewController, sourseView: UIView?) {
        var actions : [UIAlertAction] = [
            UIAlertAction(title: "Camera".localizedPSD(), style: .default, handler: { (action) -> Void in self.authorizationStatus(type: .camera, viewController: viewController)}),
            UIAlertAction(title: "Phone_Gallery".localizedPSD(), style: .default, handler: { (action) -> Void in self.authorizationStatus(type: .gallery, viewController: viewController)}),
        ]
        if PyrusServiceDesk.fileChooserController != nil {
            actions.append(UIAlertAction(title: PyrusServiceDesk.fileChooserController?.label, style: .default, handler: { (action) -> Void in
                self.authorizationStatus(type: .customChooser, viewController: viewController)
            }))
        }
        if PyrusServiceDesk.loggingEnabled {
            if logsSendController == nil {
                logsSendController = LogsSendController()
                logsSendController?.modalPresentationStyle = .overFullScreen
                logsSendController?.chooserDelegate = self
            }
            actions.append(UIAlertAction(title: logsSendController?.label, style: .default, handler: { (action) -> Void in
                self.authorizationStatus(type: .localLogs, viewController: viewController)
            }))
        }
        showMenuAlert(actions, on: viewController, sourseView: sourseView)
    }
    func cameraAutorizationStatus(type:AVMediaType?, nextType:AVMediaType?, viewController: UIViewController)
    {
        if type == nil{
            self.openCamera(viewController)
        }
        else{
            let status = AVCaptureDevice.authorizationStatus(for: type!)
            switch status{
            case .authorized:
                self.cameraAutorizationStatus(type: nextType, nextType: nil, viewController: viewController)
            case .notDetermined:
                AVCaptureDevice.requestAccess(for: type!) { granted in
                    if granted {
                        self.cameraAutorizationStatus(type: nextType, nextType: nil, viewController: viewController)
                    }
                }
            case .restricted,.denied:
                self.alertAccess( .camera , on:viewController)
            default:
                break
            }
        }
    }
    /// is window to present PyrusServiceDesk.fileChooserController over keyboard (accessoryView)
    var _alertWindow : UIWindow? = nil
    func authorizationStatus(type: AttachmentType, viewController: UIViewController){
        switch type {
        case .camera:
            self.cameraAutorizationStatus(type: .video, nextType: .audio, viewController: viewController)
        case .gallery:
            let status = PHPhotoLibrary.authorizationStatus()
            switch status{
            case .authorized, .limited:
                self.openGallery(viewController)
            case .notDetermined:
                PHPhotoLibrary.requestAuthorization({ (status) in
                    if status == PHAuthorizationStatus.authorized{
                        self.openGallery(viewController)
                    }
                })
            case .restricted,.denied:
                self.alertAccess(type, on:viewController)
            default:
                break
            }
        case .customChooser, .localLogs:
            if type == .customChooser {
                PyrusServiceDesk.fileChooserController?.chooserDelegate = self
            }
            let fileController = type == .customChooser ? PyrusServiceDesk.fileChooserController : logsSendController
            guard let overlayFrame = viewController.view?.window?.frame else {
                return
            }
            if _alertWindow == nil {
                _alertWindow = UIWindow(frame: overlayFrame)
            }
            guard let _alertWindow = _alertWindow,
                  let controller = fileController else {
                return
            }
            _alertWindow.rootViewController = UIViewController()
            _alertWindow.windowLevel = .alert
            _alertWindow.isHidden = false
            _alertWindow.rootViewController?.present(controller, animated: true, completion: nil)
        }
    }
    
    
    //FileChooserDelegate
    func didEndWithSuccess(_ data: Data?, url: URL?) {
         DispatchQueue.main.async {
            let viewController = self._alertWindow?.rootViewController?.presentedViewController
            viewController?.dismiss(animated: true, completion: {
                self._alertWindow?.isHidden = true
                self._alertWindow = nil
                if !self.needShowSizeError(for: data, on: nil), let data = data, let  url = url {
                    self.attachmentPickedBlock?(data,url)
                }
            })
        }
        
    }
    func didEndWithCancel() {
         DispatchQueue.main.async {
            let viewController = self._alertWindow?.rootViewController?.presentingViewController
            viewController?.dismiss(animated: true, completion: {
                self.closeBlock?(true)
                self._alertWindow?.isHidden = true
                self._alertWindow = nil
            })
        }
       
    }
    
    func openCamera(_ viewController: UIViewController)
    {
        DispatchQueue.main.async {
            if UIImagePickerController.isSourceTypeAvailable(.camera){
                let cameraController = UIImagePickerController()
                cameraController.delegate = self
                cameraController.modalPresentationStyle = .overFullScreen
                cameraController.sourceType = .camera
                cameraController.mediaTypes = [kUTTypeImage as String, kUTTypeMovie as String, kUTTypeVideo as String]
                viewController.present(cameraController, animated: true, completion: nil)
            }
        }
        
    }
    var oldScrollContentInset : Int? = nil
    func openGallery(_ viewController: UIViewController)
    {
        DispatchQueue.main.async {
            if UIImagePickerController.isSourceTypeAvailable(.photoLibrary){
                let libraryController = UIImagePickerController()
                libraryController.modalPresentationStyle = .overFullScreen
                libraryController.delegate = self
                libraryController.sourceType = .photoLibrary
                libraryController.mediaTypes = [kUTTypeImage as String, kUTTypeMovie as String, kUTTypeVideo as String]
                if #available(iOS 11.0, *) {
                    self.oldScrollContentInset =  UIScrollView.appearance().contentInsetAdjustmentBehavior.rawValue
                    UIScrollView.appearance().contentInsetAdjustmentBehavior = .automatic
                }
                viewController.present(libraryController, animated: true, completion: nil)
            }
        }
        
    }
    /**
     Show alert about no access to camera/photoLibrary
     - Parameter type: AttachmentType type of access.
     - Parameter viewController: UIViewController where alert must be presented
     */
    func alertAccess(_ type: AttachmentType, on viewController:UIViewController)
    {
        DispatchQueue.main.async {
            var alertMessage : String = ""
            if(type == .camera)
            {
                alertMessage = "Camera_Access_Error".localizedPSD()
            }
            if(type == .gallery)
            {
                alertMessage = "Phone_Gallery_Access_Error".localizedPSD()
            }
            showAccessAlert(with: alertMessage, on:viewController)
        }
    }
    //MARK: delegate methods
     func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        closedImagePicker()
        var hadSizeError = false
        if let image = info[.originalImage] as? UIImage {
            let imageCompressedData = image.compressImage()
            hadSizeError = needShowSizeError(for: imageCompressedData, on: picker)
            if !hadSizeError{
                self.attachmentPickedBlock?(imageCompressedData!, (info[UIImagePickerController.InfoKey.referenceURL] as? URL))
            }
        } else if let videoUrl = info[UIImagePickerController.InfoKey.mediaURL] as? NSURL{
            do {
                let data : Data = try Data(contentsOf: videoUrl as URL)
                hadSizeError = needShowSizeError(for: data, on: picker)
                //compressWithSessionStatusFunc(videoUrl)
                if !hadSizeError{
                    self.attachmentPickedBlock?(data , videoUrl as URL)
                }
            } catch {
                hadSizeError = needShowSizeError(for: nil, on: picker)
            }
        }
        if !hadSizeError{
            DispatchQueue.main.async {
                self.dismissPicker(picker)
            }
            
        }
        
    }
    func getURL(ofPhotoWith mPhasset: PHAsset, completionHandler : @escaping ((_ responseURL : URL?) -> Void)) {
        
        if mPhasset.mediaType == .image {
            let options: PHContentEditingInputRequestOptions = PHContentEditingInputRequestOptions()
            options.canHandleAdjustmentData = {(adjustmeta: PHAdjustmentData) -> Bool in
                return true
            }
            mPhasset.requestContentEditingInput(with: options, completionHandler: { (contentEditingInput, info) in
                completionHandler(contentEditingInput!.fullSizeImageURL)
            })
        } else if mPhasset.mediaType == .video {
            let options: PHVideoRequestOptions = PHVideoRequestOptions()
            options.version = .original
            PHImageManager.default().requestAVAsset(forVideo: mPhasset, options: options, resultHandler: { (asset, audioMix, info) in
                if let urlAsset = asset as? AVURLAsset {
                    let localVideoUrl = urlAsset.url
                    completionHandler(localVideoUrl)
                } else {
                    completionHandler(nil)
                }
            })
        }
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        closedImagePicker()
        dismissPicker(picker)
    }
    private func closedImagePicker(){
        if #available(iOS 11.0, *) {
            if(oldScrollContentInset != nil && UIScrollView.ContentInsetAdjustmentBehavior.init(rawValue: oldScrollContentInset!) != nil){
                UIScrollView.appearance().contentInsetAdjustmentBehavior = UIScrollView.ContentInsetAdjustmentBehavior.init(rawValue: oldScrollContentInset!)!
            }
        }
    }
    private func dismissPicker(_ picker: UIImagePickerController){
        
        picker.dismiss(animated: true, completion:{
            self.closeBlock?(true)
        })
    }
    private func needShowSizeError(for data:Data?, on picker:UIImagePickerController?)->Bool{
        if data != nil && (data?.count ?? 0)>0{
            if(Double(data!.count / 1000000) > MAX_ATTACHMENT_SIZE){
                showSizeError("Size_Error".localizedPSD(),on: picker)
                return true
            }
        }
        else{
            showSizeError("No_File_Error".localizedPSD(),on: picker)
            return true
        }
        return false
    }
    private func showSizeError(_ errorString:String,on picker:UIImagePickerController?){
        if(picker != nil){
            picker?.dismiss(animated: true, completion: {
                self.closeBlock?(true)
                showErrorAlert(errorString, on: UIApplication.topViewController())
            })
        }
        else{
            showErrorAlert(errorString, on: UIApplication.topViewController())
        }
    }


    //MARK: - Internal Properties
    ///Comletion block with data, url, fromLibrary
    var attachmentPickedBlock: ((Data, URL?) -> Void)?
    var closeBlock: ((Bool) -> Void)?
}
let MAX_ATTACHMENT_SIZE = Double(200)
