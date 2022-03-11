
import Foundation

protocol PSDAttachmentLoadErrorViewDelegate: class {
    ///Error buttom was pressed. Need to retry load attachment.
    func retryPressed()
    ///Open button was pressed need to try open attachment file in other apps
    func openPressed()
}
///A view with Error message and button. This view has same size as it's superview. Has two mode, see LoadErrorMode. When cancel button pressed - pass to its delegate cancelPressed(),When open button pressed - pass to its delegate openPressed().
class PSDAttachmentLoadErrorView: PSDView {
    weak var delegate: PSDAttachmentLoadErrorViewDelegate?
    override init(frame: CGRect) {
        super.init(frame: frame)
        recolor()
        self.addSubview(errorMessage)
        self.addSubview(button)
        self.addSubview(imageView)
        setConstraints()
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    ///The mode for error view
    ///has values : noPreview and cantLoad
    enum LoadErrorMode{
        ///mode with open button
        case noPreview
        ///mode with retry button
        case cantLoad
    }
    private var attachmentExtentionLogo : UIImage?
    ///The extension of file use to create image in mode = noPreview
    var attachmentExtension : String = ""{
        didSet{
            if(attachmentExtension.count>0){
                let label = UILabel.init()
                label.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(TEXT_ALPHA)
                label.font = .attachmentExtension
                label.text = attachmentExtension
                label.sizeToFit()
                attachmentExtentionLogo = label.asImage().withRenderingMode(.alwaysTemplate)
            }
        }
    }
    var state : LoadErrorMode = .noPreview{
        didSet{
            redraw()
        }
    }
    private func redraw(){
        switch state {
        case .noPreview:
            self.errorMessage.text = "NoPreviewAvailable".localizedPSD()
            self.button.setTitle("OpenWith".localizedPSD(), for: .normal)
            self.imageView.image = attachmentExtentionLogo
        case .cantLoad:
            self.errorMessage.text = "FileLoadError".localizedPSD()
            self.button.setTitle("RetryButton".localizedPSD(), for: .normal)
            self.imageView.image = previewImage
        }
        
        self.errorMessage.sizeToFit()
        self.button.sizeToFit()
        self.setNeedsLayout()
        self.setNeedsDisplay()
    }
    private var previewImage: UIImage? {
        if #available(iOS 13.0, *) {
            if UITraitCollection.current.userInterfaceStyle == .dark {
                return UIImage.PSDImage(name: "landscape_light")
            }
        }
        return UIImage.PSDImage(name: "landscape_dark")
    }
    override func recolor() {
        super.recolor()
        redraw()
        self.backgroundColor = CustomizationHelper.grayViewColor
        self.imageView.tintColor = CustomizationHelper.textColorForTable.withAlphaComponent(TEXT_ALPHA)
        errorMessage.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(TEXT_ALPHA)
    }
    private lazy var errorMessage : UILabel = {
        let label = UILabel.init()
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.textAlignment = .center
        return label
    }()
    private lazy var imageView : UIImageView = {
        let iv = UIImageView.init()
        iv.contentMode = .scaleAspectFit
        return iv
    }()
    ///The button on view changes according to LoadErrorMode.
    ///If mode == noPreview - performs the option "open"
    ///If mode == cantLoad - performs the option "retry"
    lazy var button : UIButton = {
        let button = UIButton()
        button.titleLabel?.font = .buttonFont
        button.setTitleColor(.darkAppColor, for: .normal)
        button.addTarget(self, action: #selector(buttonPressed), for: .touchUpInside)
        return button
    }()
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        self.translatesAutoresizingMaskIntoConstraints = false
        self.addZeroConstraint([.top,.bottom,.leading,.trailing])
        
        redraw()
        
    }
    @objc private func buttonPressed(){
        if(self.state == .cantLoad){
            self.delegate?.retryPressed()
        }
        else{
            self.delegate?.openPressed()
        }
    }
    private static let distBetwinElement : CGFloat  = 15
    private static let dist : CGFloat = 20.0
    private func setConstraints(){
        self.errorMessage.translatesAutoresizingMaskIntoConstraints = false
        self.imageView.translatesAutoresizingMaskIntoConstraints = false
        self.button.translatesAutoresizingMaskIntoConstraints = false
        
        self.imageView.addSizeConstraint([.width,.height], constant: IMAGE_VIEW_SIZE)
        self.imageView.addZeroConstraint([.centerX])
        
        self.addConstraint(NSLayoutConstraint(
            item: imageView,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: errorMessage,
            attribute: .top,
            multiplier: 1,
            constant:-(PSDAttachmentLoadErrorView.distBetwinElement*3)))
        
        self.errorMessage.addZeroConstraint([.centerX,.centerY])
        
        self.setLateralConstraints(errorMessage)
        self.setLateralConstraints(button)
        
        self.addConstraint(NSLayoutConstraint(
            item: button,
            attribute: .top,
            relatedBy: .equal,
            toItem: errorMessage,
            attribute: .bottom,
            multiplier: 1,
            constant:PSDAttachmentLoadErrorView.distBetwinElement))
    }
    
    private func setLateralConstraints(_ view:UIView){
        
        view.leftAnchor.constraint(
            equalTo: self.layoutMarginsGuide.leftAnchor,
            constant: PSDAttachmentLoadErrorView.dist
            ).isActive = true
        view.rightAnchor.constraint(
            equalTo: self.layoutMarginsGuide.rightAnchor,
            constant: -PSDAttachmentLoadErrorView.dist
            ).isActive = true
    }
    
}
private extension UIFont {
    static let attachmentExtension = CustomizationHelper.systemFont(ofSize: IMAGE_VIEW_SIZE/2)
    static let buttonFont = CustomizationHelper.systemFont(ofSize: 18.0)
}
private let TEXT_ALPHA: CGFloat = 0.6
private let IMAGE_VIEW_SIZE: CGFloat = 100.0
