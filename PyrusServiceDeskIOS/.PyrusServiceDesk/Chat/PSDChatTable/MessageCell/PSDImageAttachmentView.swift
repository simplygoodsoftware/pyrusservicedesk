
import UIKit

let PREVIEW_ATTACHMENT_HEIGHT : CGFloat = 140.0
let PREVIEW_BORDER_WIDTH : CGFloat = 1.0
///AttachmentView with type isImage = true, has 3 states:
//1)loading
//2)can load
//3)with image
//by default draw with loading
class PSDImageAttachmentView: PSDAttachmentView {
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.insertSubview(previewImageView, at: 0)
        self.addSubview(downloadView)
        self.backgroundColor = .clear
        
        self.previewImageView.layer.borderWidth = PREVIEW_BORDER_WIDTH
        self.previewImageView.layer.cornerRadius = MESSAGE_CORNER_RADIUS
        
        self.addConstraints()
    }
    ///The min supported width
    private var minWidth :CGFloat = 65
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func draw(_ attachment: PSDAttachment, state: messageState) {
        super.draw(attachment, state: state)
        self.previewImageView.widthConstraint?.constant = calculateWidth()
    }
    private func addConstraints(){
            self.uploadView.translatesAutoresizingMaskIntoConstraints = false
            self.uploadView.addConstraint([.centerX,.centerY], constant: 0)
            
            self.downloadView.translatesAutoresizingMaskIntoConstraints = false
            self.downloadView.addConstraint([.centerX,.centerY], constant: 0)
            self.downloadView.addSizeConstraint([.width, .height], constant: PSDDownloadView.downloadSize)
            
            self.previewImageView.translatesAutoresizingMaskIntoConstraints = false
            self.previewImageView.addZeroConstraint([.leading,.trailing,.top,.bottom])
            self.previewImageView.widthConstraint?.constant = calculateWidth()
            self.previewImageView.heightConstraint?.constant = PREVIEW_ATTACHMENT_HEIGHT
            self.previewImageView.heightConstraint?.priority = UILayoutPriority(rawValue: 999)
    }
    private func calculateWidth()->CGFloat{
        if let previewImage = self.previewImage{
            let scale = previewImage.size.height/PREVIEW_ATTACHMENT_HEIGHT
            var width = previewImage.size.width/scale
            width = min(width, maxWidth)
            width = max(width, minWidth)
            return width
        }
        return PREVIEW_ATTACHMENT_HEIGHT
    }
    private func updateWidthConstraint(animated:Bool){
        
        self.superview?.layoutIfNeeded()
        self.previewImageView.widthConstraint?.constant = self.calculateWidth()
        if !animated{
            previewImageView.invalidateIntrinsicContentSize()
            self.superview?.layoutIfNeeded()
        }
        if #available(iOS 10.0, *){
            if(animated){
                var rect = self.frame
                let deltaWidth = self.frame.size.width - self.calculateWidth()
                rect.origin.x = rect.origin.x<=(AVATAR_SIZE+(TO_BOARD_DISTANCE*2)) ? rect.origin.x :  rect.origin.x - deltaWidth
                rect.size.width =  rect.size.width + deltaWidth
                UIView.animate(withDuration: 0.2,
                               delay: 0,
                               options: [.beginFromCurrentState, .allowUserInteraction],
                               animations: { () -> Void in
                                self.frame = rect
                                self.layoutIfNeeded()
                                
                }, completion: { copmlete in
                    self.previewImageView.widthConstraint?.constant = self.calculateWidth()
                    self.previewImageView.invalidateIntrinsicContentSize()
                    self.superview?.layoutIfNeeded()
                })
            }
        }
        

    
    }
    override func setPreviewImage(_ image:UIImage?, animated: Bool){
        super.setPreviewImage(image, animated:animated)
        self.updateWidthConstraint(animated: animated)
    }
    override var previewImage : UIImage?{
        didSet{
            if(previewImage != nil){
                downloadView.removeFromSuperview()
            }
        }
    }
    override var downloadState: messageState{
        didSet{
            self.uploadView.isHidden = downloadState == .sent
        }
    }
    private let downloadView = PSDDownloadView()
    override var color: UIColor {
        didSet {
            self.previewImageView.layer.borderColor = color.withAlphaComponent(BORDER_ALPHA).cgColor
            downloadView.color = color
        }
    }
}
private let BORDER_ALPHA: CGFloat = 0.15
