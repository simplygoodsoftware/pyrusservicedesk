
import UIKit
let LABEL_MINIMUM_SCALE_FACTOR : CGFloat = 0.7
class PSDFileAttachmentView: PSDAttachmentView {
    private static let stateLabelAlpha : CGFloat = 0.6
    private static let uploadString = "Uploading".localizedPSD()
    private static var sizeString : String = " "
    private static let stateLabelHeight : CGFloat = 22
    private static let nameLabelHeight : CGFloat = 19
   
    private static let distance :CGFloat = 10.0
    let height : CGFloat = PSDAttachmentView.uploadSize+(PSDFileAttachmentView.distance*2)
    override var color :UIColor{
        didSet{
            nameLabel.textColor = self.color
            stateLabel.textColor = self.color
            previewBorderLayer.strokeColor = color.withAlphaComponent(0.15).cgColor
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.addSubview(nameLabel)
        self.addSubview(stateLabel)
        self.insertSubview(previewImageView, at: 0)
        self.frame = CGRect(x: 0, y: 0, width: 0, height: height)
        addConstraints()
        
        self.previewImageView.layer.addSublayer(previewBorderLayer)
    }
    override var downloadState: messageState{
        didSet(oldValue){
            super.downloadState = downloadState
             checkUploadView()
            if(oldValue != downloadState){
                DispatchQueue.main.async {
                    self.updateStateText(with: super.downloadState)
                }
            }
        }
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func draw(_ attachment: PSDAttachment, state: messageState) {
        super.draw(attachment, state: state)
        previewImageView.isHidden = true
        PSDFileAttachmentView.sizeString = attachment.dataSize()
        updateStateText(with: state)
        
        stateLabel.font = .stateFont
        nameLabel.font = .nameFont
        nameLabel.text = attachment.name
        
        var rect = nameLabel.frame
        rect.size.width = nameLabelWidth()
        nameLabel.frame = rect
        updateLabelsConstraints()
    }
    private func updateStateText(with newState:messageState){
        stateLabel.text = newState == .sending ? PSDFileAttachmentView.uploadString : attachment?.dataSize()
    }
    private lazy var nameLabel : UILabel = {
        let label = UILabel()
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = LABEL_MINIMUM_SCALE_FACTOR
        label.lineBreakMode = .byTruncatingMiddle
        label.frame = CGRect(x: (PSDFileAttachmentView.distance*2)+PSDAttachmentView.uploadSize, y: height - PSDFileAttachmentView.stateLabelHeight - PSDFileAttachmentView.distance-PSDFileAttachmentView.nameLabelHeight, width: 10, height: PSDFileAttachmentView.nameLabelHeight)
        return label
    }()
    ///The label with state of attachment - if now is uplo
    private lazy var stateLabel : UILabel = {
        let label = UILabel()
        
        label.alpha = PSDFileAttachmentView.stateLabelAlpha
        label.adjustsFontSizeToFitWidth = true
        label.frame = CGRect(x: (PSDFileAttachmentView.distance*2)+PSDAttachmentView.uploadSize, y: height - PSDFileAttachmentView.stateLabelHeight - PSDFileAttachmentView.distance, width: 0, height: PSDFileAttachmentView.stateLabelHeight)
        return label
    }()
    ///Calculate maximum width of labels name and state
    private func getMaxWidth()->CGFloat{
        return min(maxWidth-(PSDFileAttachmentView.distance*2)-PSDAttachmentView.uploadSize , max(maxStateLabelWidth(),nameLabelWidth()))
    }
    ///Calculate name label width
    private func nameLabelWidth()->CGFloat{
        return nameLabel.text?.size(withAttributes: [NSAttributedString.Key.font: UIFont.nameFont]).width ?? 0
    }
    ///Calculate State label width
    private func maxStateLabelWidth()->CGFloat{
        let downloadWidth :CGFloat = PSDFileAttachmentView.uploadString.size(withAttributes: [NSAttributedString.Key.font: UIFont.stateFont]).width
        let attachmentSizeWidth :CGFloat = PSDFileAttachmentView.sizeString.size(withAttributes: [NSAttributedString.Key.font: UIFont.stateFont]).width
        return max(downloadWidth,attachmentSizeWidth)
    }
    
    //MARK: CONSTRAINTS
    private func updateLabelsConstraints(){
        stateWidthConstraint?.constant = getMaxWidth()
    }
    
    var stateWidthConstraint : NSLayoutConstraint?
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        let rectShape = CAShapeLayer()
        rectShape.path = UIBezierPath(roundedRect: previewImageView.bounds, byRoundingCorners: [.bottomLeft, .topLeft], cornerRadii: CGSize(width: MESSAGE_CORNER_RADIUS, height: MESSAGE_CORNER_RADIUS)).cgPath
        self.previewImageView.layer.mask = rectShape
        
        previewBorderLayer.path = rectShape.path
        previewBorderLayer.frame = self.previewImageView.bounds
    }
    private lazy var previewBorderLayer : CAShapeLayer = {
        let borderLayer = CAShapeLayer()
        borderLayer.fillColor = UIColor.clear.cgColor
        borderLayer.strokeColor = UIColor.psdLabel.withAlphaComponent(0.15).cgColor
        borderLayer.lineWidth = PREVIEW_BORDER_WIDTH
        return borderLayer
    }()
    private func addConstraints(){
        uploadView.addConstraint([.leading], constant: PSDFileAttachmentView.distance)
        uploadView.addZeroConstraint([.centerY])
        
        let heightConstraint =
            NSLayoutConstraint(
                item: self,
                attribute: .height,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1,
                constant:height)
        heightConstraint.priority = UILayoutPriority(rawValue: 999)
        self.addConstraint(heightConstraint)
        
        
        self.stateLabel.translatesAutoresizingMaskIntoConstraints = false
        self.addConstraint(NSLayoutConstraint(
            item: stateLabel,
            attribute: .left,
            relatedBy: .equal,
            toItem: uploadView,
            attribute: .right,
            multiplier: 1,
            constant: PSDFileAttachmentView.distance))
        stateLabel.addConstraint([.trailing,.bottom], constant: -PSDFileAttachmentView.distance)
        
        stateWidthConstraint = NSLayoutConstraint(
            item: stateLabel,
            attribute: .width,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:getMaxWidth())
        stateWidthConstraint!.priority = UILayoutPriority(rawValue: 999)
        self.addConstraint(stateWidthConstraint!)
        
        stateLabel.addSizeConstraint([.height], constant: PSDFileAttachmentView.stateLabelHeight)
        
        self.nameLabel.translatesAutoresizingMaskIntoConstraints = false
        self.addConstraint(NSLayoutConstraint(
            item: nameLabel,
            attribute: .left,
            relatedBy: .equal,
            toItem: stateLabel,
            attribute: .left,
            multiplier: 1,
            constant:0))
        self.addConstraint(NSLayoutConstraint(
            item: nameLabel,
            attribute: .width,
            relatedBy: .equal,
            toItem: stateLabel,
            attribute: .width,
            multiplier: 1,
            constant:0))
        self.addConstraint(NSLayoutConstraint(
            item: nameLabel,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: stateLabel,
            attribute: .top,
            multiplier: 1,
            constant:0))
        nameLabel.addSizeConstraint([.height], constant: PSDFileAttachmentView.nameLabelHeight)
        
        let distanceToFixRoundingCorners : CGFloat = 1.0
        self.previewImageView.translatesAutoresizingMaskIntoConstraints = false
        self.previewImageView.addConstraint([.left], constant: -distanceToFixRoundingCorners)
        self.previewImageView.addConstraint([.top,.bottom], constant: 0)
        self.previewImageView.addZeroConstraint([.centerY])
        self.previewImageView.widthConstraint?.constant = PSDAttachmentView.uploadSize+PSDFileAttachmentView.distance+(distanceToFixRoundingCorners*2)
        self.previewImageView.heightConstraint?.isActive = false
        
    }
    override var previewImage : UIImage?{
        didSet{
            checkUploadView()
            if(previewImage != nil){
                showPreviewImage()
            }
        }
    }
    private func checkUploadView(){
        self.uploadView.isHidden = (downloadState == .sent && previewImage != nil)
    }
    private func showPreviewImage(){
        self.previewImageView.isHidden = false
    }
}
private extension UIFont {
    static let nameFont = CustomizationHelper.systemFont(ofSize: 16.0)
    static let stateFont = CustomizationHelper.systemFont(ofSize: 14.0)
}
