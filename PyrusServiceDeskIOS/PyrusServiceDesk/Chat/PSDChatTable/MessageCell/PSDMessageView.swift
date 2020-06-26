
import UIKit
let MESSAGE_CORNER_RADIUS : CGFloat = 15.0
class PSDMessageView: UIView{
    
    weak var delegate: PSDRetryActionDelegate?
    enum colorType {
        case brightColor
        case defaultColor
    }
    var color : colorType = .brightColor
    {
        didSet{
            recolor()
        }
    }
    private func recolor(){
        switch (color) {
        case .brightColor:
            self.backgroundColor = UIColor.appColor
            messageTextView.textColor = UIColor.appTextColor
            attachmentView?.color = UIColor.appTextColor
            separatorView.tintColor = UIColor.appTextColor.withAlphaComponent(PSDMessageView.separatorAlpha)
            
        case .defaultColor:
            self.backgroundColor = UIColor.psdLightGray
            messageTextView.textColor = .psdLabel
            attachmentView?.color = .psdLabel
            separatorView.tintColor = UIColor.psdLabel.withAlphaComponent(PSDMessageView.separatorAlpha)
        }
    }
    private static let distToBoard : CGFloat = 10.0
    
    var maxWidth : CGFloat = 50
    private static let separatorAlpha :CGFloat = 0.8
    func draw(message:PSDRowMessage)
    {
        messageTextView.text = message.text
        attachmentView?.removeFromSuperview()
        attachmentView = nil
        if(self.backgroundColor == .clear){
            self.recolor()
        }
        var hasImageAttachment = false
        if let data = message.attachment{
            if data.isImage{
                hasImageAttachment = true
                attachmentView = PSDImageAttachmentView.init(frame: CGRect.zero)
            }
            else{
                attachmentView = PSDFileAttachmentView.init(frame: CGRect.zero)
            }
            guard let attachmentView = attachmentView else{
                return
            }
            attachmentHolderView.addSubview(attachmentView)
            attachmentView.maxWidth = maxWidth
            attachmentView.draw(data, state: message.message.state)
            attachmentView.color = self.color == .defaultColor ?  .psdLabel : UIColor.appTextColor
            attachmentView.addZeroConstraint([.leading,.trailing,.top,.bottom])
        }
        if let rating = message.rating{
            ratingLabel.text = rateArray[rating]
            if(message.attachment == nil && message.text.count == 0){
                self.backgroundColor = .clear
            }
        }else{
            ratingLabel.text = nil
        }
        
        attachmentHolderRightConstraint?.isActive = !hasImageAttachment
        attachmentHolderImageRightConstraint?.isActive = hasImageAttachment
        separatorHeightConstraint?.constant = (messageTextView.text.count == 0 || attachmentView == nil) ? 0 : PSDMessageView.separatorHeight //show separatoe only is has both text and image
        messageEmptyHeightConstraint?.isActive = messageTextView.text.count == 0
    }
    lazy private var tapGesture : UITapGestureRecognizer = {
        let gesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        gesture.cancelsTouchesInView = false
        return gesture
    }()
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        self.delegate?.tryShowRetryAction()
    }
    private static let messageTextViewFontSize : CGFloat = 18.0
    private static let separatorHeight : CGFloat = 3.0
    private(set) var messageTextView : PSDCopyTextView = {
        let text = PSDCopyTextView.init(frame: CGRect.zero)
        text.backgroundColor = .clear
        text.font = UIFont.systemFont(ofSize: messageTextViewFontSize)
        text.textColor = .psdLabel
        text.isScrollEnabled = false
        text.isEditable = false
        let inset = UIEdgeInsets(top: PSDMessageView.distToBoard, left: PSDMessageView.distToBoard, bottom: PSDMessageView.distToBoard, right: PSDMessageView.distToBoard)
        
        text.textContainerInset = inset
        text.layoutMargins = UIEdgeInsets.zero
        text.contentInset = UIEdgeInsets.zero
        
        text.dataDetectorTypes = [.link,.phoneNumber]
        text.linkTextAttributes = [ NSAttributedString.Key.underlineStyle: NSUnderlineStyle.single.rawValue]
        
        return text
    }()
    private var ratingLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.RATING_FONT
        return label
    }()
    private lazy var attachmentHolderView : UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        return view
    }()
    private lazy var separatorView : UIImageView = {
        let view = UIImageView()
        let image = UIImage.PSDImage(name: "dotted_line").withRenderingMode(.alwaysTemplate)
        view.image = image
        view.contentMode = .scaleAspectFit
        view.tintColor = UIColor.psdLabel.withAlphaComponent(PSDMessageView.separatorAlpha)
        return view
    }()
    var attachmentView : PSDAttachmentView?
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.layer.cornerRadius = MESSAGE_CORNER_RADIUS
        self.addSubview(messageTextView)
        self.addSubview(attachmentHolderView)
        self.addSubview(separatorView)
        self.addGestureRecognizer(tapGesture)
        self.addSubview(ratingLabel)
        addConstraints()
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    private var separatorWidthConstraint : NSLayoutConstraint?
    private var separatorHeightConstraint : NSLayoutConstraint?
    private var messageEmptyHeightConstraint : NSLayoutConstraint?
    private var attachmentHolderRightConstraint : NSLayoutConstraint?//the cosntaint to right side with "lessThanOrEqualTo"
    private var attachmentHolderImageRightConstraint : NSLayoutConstraint?//the cosntaint to right side with "EqualTo""
    
    private func addConstraints()
    {
        messageTextView.translatesAutoresizingMaskIntoConstraints = false
        attachmentHolderView.translatesAutoresizingMaskIntoConstraints = false
        separatorView.translatesAutoresizingMaskIntoConstraints = false
        ratingLabel.translatesAutoresizingMaskIntoConstraints = false
        
        ratingLabel.addZeroConstraint([.top])
        ratingLabel.leftAnchor.constraint(greaterThanOrEqualTo: leftAnchor, constant: 0).isActive = true
        ratingLabel.rightAnchor.constraint(lessThanOrEqualTo: rightAnchor, constant: 0).isActive = true
        ratingLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        
        attachmentHolderView.topAnchor.constraint(equalTo: ratingLabel.bottomAnchor, constant: 0).isActive = true
        attachmentHolderView.leftAnchor.constraint(equalTo: leftAnchor, constant: 0).isActive = true
        attachmentHolderRightConstraint = attachmentHolderView.rightAnchor.constraint(lessThanOrEqualTo: rightAnchor, constant: 0)
        attachmentHolderRightConstraint?.isActive = true
        attachmentHolderImageRightConstraint = attachmentHolderView.rightAnchor.constraint(equalTo: rightAnchor, constant: 0)
        attachmentHolderImageRightConstraint?.isActive = false
        
        attachmentHolderView.bottomAnchor.constraint(equalTo: separatorView.topAnchor).isActive = true
        
        separatorHeightConstraint = separatorView.heightAnchor.constraint(equalToConstant: PSDMessageView.separatorHeight)
        separatorHeightConstraint?.isActive = true
        separatorWidthConstraint = separatorView.widthAnchor.constraint(equalToConstant: 0)
        separatorWidthConstraint?.isActive = true
        separatorView.bottomAnchor.constraint(equalTo: messageTextView.topAnchor, constant: 0).isActive = true
        separatorView.addConstraint([.leading], constant: PSDMessageView.distToBoard)
        
        messageTextView.addConstraint([.bottom], constant: 0)
        messageTextView.leftAnchor.constraint(equalTo: leftAnchor, constant: 0).isActive = true
        messageTextView.rightAnchor.constraint(lessThanOrEqualTo: rightAnchor, constant: 0).isActive = true
        messageEmptyHeightConstraint = messageTextView.heightAnchor.constraint(equalToConstant: 0)
       
    }
    override func layoutSubviews() {
        separatorWidthConstraint?.constant = self.frame.size.width - (PSDMessageView.distToBoard*2)//change separator width, or it will resize its parent view
    }
    override var intrinsicContentSize: CGSize{
        return CGSize.zero
    }
}
private extension UIFont {
    static let RATING_FONT = UIFont.systemFont(ofSize: 40)
}
