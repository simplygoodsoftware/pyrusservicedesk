
import UIKit
class PSDAttachmentImageView: UIImageView{
    var widthConstraint : NSLayoutConstraint?
    var heightConstraint : NSLayoutConstraint?
    override init(frame: CGRect) {
        super.init(frame: frame)
        customInit()
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        customInit()
    }
    private func customInit(){
        widthConstraint = widthAnchor.constraint(greaterThanOrEqualToConstant: 0)
        widthConstraint?.isActive = true
        heightConstraint = heightAnchor.constraint(equalToConstant: 0)
        heightConstraint?.isActive = true
    }
   override var intrinsicContentSize: CGSize{
    return CGSize(width: widthConstraint?.constant ?? 0, height: PREVIEW_ATTACHMENT_HEIGHT)
   }
}
class PSDAttachmentView: UIView{
    var attachment : PSDAttachment?
    var color :UIColor = .white{
        didSet{
            uploadView.color = self.color
        }
    }
    func setPreviewImage(_ image:UIImage?, animated: Bool){
        self.previewImage = image
    }
    lazy private(set) var previewImageView : PSDAttachmentImageView = {
        let iv = PSDAttachmentImageView.init(frame: CGRect.zero)
        iv.backgroundColor = CustomizationHelper.lightGrayViewColor
        iv.clipsToBounds = true
        iv.contentMode = .scaleAspectFill
        return iv
    }()
    var previewImage: UIImage?{
        didSet{
            self.previewImageView.image = previewImage
        }
    }
    ///The progress of loading between 0...1. May have value >2, that mean that loading end with error
    var progress : CGFloat = 1.0{
        willSet(newValue)
        {
            uploadView.progress = newValue
        }
    }
    var downloadState: messageState = .sent {
        didSet(oldValue){
            uploadView.downloadState = downloadState
        }
    }
    static let uploadSize :CGFloat  = 50.0
    
    var maxWidth :CGFloat = 1000
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.addSubview(uploadView)
        self.addGestureRecognizer(tapGesture)
        tapGesture.delegate = self
        self.translatesAutoresizingMaskIntoConstraints = false
        
    }
    lazy private var tapGesture : UITapGestureRecognizer = {
        let gesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        gesture.cancelsTouchesInView = false
        return gesture
    }()
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        openAttachment()
    }
    fileprivate func canOpenAttachment() -> Bool {
        return !(attachment?.emptyId() ?? true )
    }
    ///Open attachment in PSDAttachmentLoadViewController
    private func openAttachment(){
        if downloadState != .sent{
            return
        }
        if (self.attachment?.serverIdentifer?.count ?? 0)>0{
            let web :PSDAttachmentLoadViewController = PSDAttachmentLoadViewController(nibName:nil, bundle:nil)
            web.attachment = self.attachment
            let navCotroller = PSDNavigationController(rootViewController: web)
                navCotroller.modalPresentationStyle = .overFullScreen
        self.findViewController()?.navigationController?.present(navCotroller, animated: true, completion: nil)
        }
       
    }
    
    ///Redraw view with attachment
    ///- parameter attachment: PSDAttachment to draw
    ///- parameter state: the state of message
    func draw(_ attachment:PSDAttachment, state: messageState) {
        self.attachment = attachment
        progress = attachment.uploadingProgress
        downloadState = state
        uploadView.progress = attachment.uploadingProgress
        uploadView.downloadState = state
        addConstraints()
        uploadView.redraw()
    }
    private func addConstraints(){
         uploadView.translatesAutoresizingMaskIntoConstraints = false
        
        
        self.addConstraint( NSLayoutConstraint(
                item: uploadView,
                attribute: .width,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1,
                constant:PSDAttachmentView.uploadSize))
        
        
        self.addConstraint(NSLayoutConstraint(
                item: uploadView,
                attribute: .height,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1,
                constant:PSDAttachmentView.uploadSize))
    }
    ///Shows attachment state and load progress
    lazy var uploadView : PSDUploadView = {
        let view = PSDUploadView.init(frame: CGRect(x: 0, y: 0, width: PSDAttachmentView.uploadSize, height: PSDAttachmentView.uploadSize))
        view.delegate = self
        return view
    }()

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
extension PSDAttachmentView : PSDUploadViewDelegate{
    //PSDUploadViewDelegate
    func stopUpload(){
        if self.attachment != nil{
            PSDMessageSend.stopUpload(self.attachment!)
        }
    }
}
extension PSDAttachmentView: UIGestureRecognizerDelegate{
    override func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        return canOpenAttachment()
    }
}
