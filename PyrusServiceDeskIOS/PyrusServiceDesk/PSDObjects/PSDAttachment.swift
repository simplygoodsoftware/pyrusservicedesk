import Foundation

class PSDAttachment: NSObject {
    let imageRepository: ImageRepositoryProtocol?
    var localPath: String?
    var name: String = "" {
        didSet {
            self.canOpen = name.isSupportedFileFormat()
            self.isImage = name.isImageFileFormat()
            self.isVideo = name.isVideoFormat()
            self.isAudio = name.isAudioFormat()
        }
    }
    ///Size of attachment to show in attachment view
    var size: Int = 0
    ///Data of attachment
    var data: Data = Data()
    ///Identifer from server - used to open and download attachment.
    var serverIdentifer: String? {
        didSet {
            if let image = previewImage {
                imageRepository?.saveImage(image, name: name, id: serverIdentifer, type: .image)
            }
        }
    }
    ///Is attachment has format that WKWebView can open
    var canOpen : Bool = false
    ///Is attachment has image format
    var isImage : Bool = false
    ///Is attachment has video format
    var isVideo : Bool = false
    ///Is attachment has audio format
    var isAudio: Bool = false
    ///uploading of file progrees to server
    var uploadingProgress : CGFloat = 1.0
    ///The preview image of attacment
    var previewImage : UIImage? {
        didSet {
            if let image = previewImage {
                imageRepository?.saveImage(image, name: name, id: serverIdentifer, type: .image)
            }
        }
    }
    
    var localId : String
    var isLoading: Bool = false
    
    init(localPath: String? , data:Data?, serverIdentifer:String?)  {
        imageRepository = ImageRepository()
        self.localPath = localPath
        if let localPath = localPath{
            let url = URL.init(string: localPath)
            self.name = url?.lastPathComponent.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        if(self.name.count == 0){
            self.name = Date().asNumbersString() + ".jpeg"
        }
        self.canOpen = name.isSupportedFileFormat()
        self.isImage = name.isImageFileFormat()
        self.localId = UUID().uuidString
        if (data != nil){
            self.data = data!
            self.previewImage = PSDAttachment.createPreviewImage(from: data!)
            self.size = self.data.count
        }
        self.serverIdentifer = serverIdentifer
    }
    private static func createPreviewImage(from data:Data)-> UIImage?{
        if data.count > 0{
            let image = UIImage.init(data: data)
            let pixelSize = PREVIEW_ATTACHMENT_HEIGHT*2
            if(image != nil){
                return image?.imageResize(to: pixelSize)
            }
        }
        return nil
    }
    func dataSize()->String{
        let bcf = ByteCountFormatter()
        bcf.countStyle = .file
        if(size != 0){
            return bcf.string(fromByteCount: Int64(size))
        }
        else{
            return bcf.string(fromByteCount: Int64(data.count))
        }
    }
    func emptyId()->Bool{
        if self.serverIdentifer != nil && self.serverIdentifer!.count>0 && self.serverIdentifer != "0"{
            return false
        }
        return true
    }
}
