
import UIKit
let MESSAGE_CORNER_RADIUS : CGFloat = 15.0
class PSDMessageView: PSDView{
    weak var delegate: PSDRetryActionDelegate?
    enum colorType {
        case brightColor
        case defaultColor
    }
    
    private lazy var timeView: UIView = {
        let view = UIView()
        view.addSubview(timeLabel)
        timeLabel.translatesAutoresizingMaskIntoConstraints = false 
        timeLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: TIME_BORDER).isActive = true
        timeLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: TIME_BORDER).isActive = true
        timeLabel.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -TIME_BORDER).isActive = true
        timeLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -TIME_BORDER).isActive = true
        view.heightAnchor.constraint(equalToConstant: TIME_HEIGHT).isActive = true
        view.layer.cornerRadius = TIME_HEIGHT / 2
        return view
    }()
    
    ///A label with time when message was sent.
    private lazy var timeLabel: UILabel =
    {
        let label = UILabel()
        label.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(TIME_ALPHA)
        label.font = DETAIL_FONT
        return label;
    }()
    
    var color : colorType = .brightColor
    {
        didSet{
            recolor()
        }
    }
    override func recolor(){
        super.recolor()
        switch (color) {
        case .brightColor:
            self.backgroundColor = CustomizationHelper.userMassageBackgroundColor
            recolorWithTextColor(CustomizationHelper.userMassageTextColor)
        case .defaultColor:
            self.backgroundColor = CustomizationHelper.supportMassageBackgroundColor
            recolorWithTextColor(CustomizationHelper.supportMassageTextColor)
        }
        setColorToTimeView()
    }
    
    private func setColorToTimeView() {
        let hasText = messageTextView.attributedText.string.count > 0
        let hasImageAttachment = (attachmentView is PSDImageAttachmentView) && attachmentView?.superview != nil
        if !hasText && hasImageAttachment {
            timeView.backgroundColor = .black.withAlphaComponent(TIME_VIEW_ALPHA)
            timeLabel.textColor = .white
        } else {
            timeView.backgroundColor = .clear
            switch (color) {
            case .brightColor:
                timeLabel.textColor = CustomizationHelper.userMassageTextColor.withAlphaComponent(TIME_ALPHA)
            case .defaultColor:
                timeLabel.textColor = CustomizationHelper.supportMassageTextColor.withAlphaComponent(TIME_ALPHA)
            }
        }
    }
    
    private func recolorWithTextColor(_ color: UIColor) {
        attachmentView?.color = color
        separatorView.tintColor = color.withAlphaComponent(PSDMessageView.separatorAlpha)
    }
    private static let distToBoard : CGFloat = 10.0
    private let PLACEHOLDER_HEIGHT: CGFloat = 20
    private let PLACEHOLDER_WIDTH: CGFloat = 50
    var maxWidth : CGFloat = 50 {
        didSet {
            updateTimeLayout()
        }
    }
    private static let separatorAlpha :CGFloat = 0.8
    func draw(message:PSDRowMessage)
    {
        timeLabel.text = message.message.date.timeAsString()
        messageTextView.attributedText = message.attributedText
        attachmentView?.removeFromSuperview()
        placeholderImageView.isHidden = message.message as? PSDPlaceholderMessage == nil
        placeholderBottomConstraint?.isActive = message.message as? PSDPlaceholderMessage != nil
        placeholderRightConstraint?.isActive = message.message as? PSDPlaceholderMessage != nil
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
            attachmentView.addZeroConstraint([.leading,.trailing,.top,.bottom])
            recolor()
        }
        if let rating = message.rating{
            ratingLabel.text = rateArray[rating]
            if(message.attachment == nil && message.text.count == 0){
                self.backgroundColor = .clear
            }
        }else{
            ratingLabel.text = nil
        }
        
        setColorToTimeView()
        
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
        text.font = .messageTextView
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
    private let placeholderImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage.PSDImage(name: "messagePlaceholder")
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    private var ratingLabel: UILabel = {
        let label = UILabel()
        label.font = .ratingLabel
        return label
    }()
    private lazy var attachmentHolderView : UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        return view
    }()
    private lazy var separatorView : UIImageView = {
        let view = UIImageView()
        let image = UIImage.PSDImage(name: "dotted_line")?.withRenderingMode(.alwaysTemplate)
        view.image = image
        view.contentMode = .scaleAspectFit
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
        addSubview(placeholderImageView)
        addSubview(timeView)
        addConstraints()
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    private var placeholderBottomConstraint: NSLayoutConstraint?
    private var placeholderRightConstraint: NSLayoutConstraint?
    private var separatorWidthConstraint: NSLayoutConstraint?
    private var separatorHeightConstraint: NSLayoutConstraint?
    private var messageEmptyHeightConstraint: NSLayoutConstraint?
    private var attachmentHolderRightConstraint: NSLayoutConstraint?//the cosntaint to right side with "lessThanOrEqualTo"
    private var attachmentHolderImageRightConstraint: NSLayoutConstraint?//the cosntaint to right side with "EqualTo""
    private var timeInBottomConstraints = [NSLayoutConstraint]()
    private var timeInBottomAndLineConstraints = [NSLayoutConstraint]()
    private var timeInLineConstraints = [NSLayoutConstraint]()
    
    private func addConstraints()
    {
        timeConstraints()
        messageTextView.translatesAutoresizingMaskIntoConstraints = false
        attachmentHolderView.translatesAutoresizingMaskIntoConstraints = false
        separatorView.translatesAutoresizingMaskIntoConstraints = false
        ratingLabel.translatesAutoresizingMaskIntoConstraints = false
        placeholderImageView.translatesAutoresizingMaskIntoConstraints = false
        
        placeholderImageView.leftAnchor.constraint(equalTo: leftAnchor, constant: PSDMessageView.distToBoard).isActive = true
        placeholderImageView.topAnchor.constraint(equalTo: topAnchor, constant: PSDMessageView.distToBoard).isActive = true
        placeholderBottomConstraint = placeholderImageView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -PSDMessageView.distToBoard)
        placeholderRightConstraint = placeholderImageView.rightAnchor.constraint(equalTo: rightAnchor, constant: -PSDMessageView.distToBoard)
        placeholderImageView.heightAnchor.constraint(equalToConstant: PLACEHOLDER_HEIGHT).isActive = true
        placeholderImageView.widthAnchor.constraint(equalToConstant: PLACEHOLDER_WIDTH).isActive = true
        
        
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
        
       // messageTextView.addConstraint([.bottom], constant: 0)
        messageTextView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 0).isActive = true
        
        let bTrailingConstraint = messageTextView.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: 0)
        bTrailingConstraint.isActive = true
        let bBottomConstraint = messageTextView.bottomAnchor.constraint(equalTo: timeView.topAnchor)
        bBottomConstraint.isActive = true
        timeInBottomConstraints.append(bTrailingConstraint)
        timeInBottomConstraints.append(bBottomConstraint)
        timeInBottomAndLineConstraints.append(bTrailingConstraint)
        
        let lTrailingConstraint = messageTextView.trailingAnchor.constraint(lessThanOrEqualTo: timeView.leadingAnchor, constant: 0)
        lTrailingConstraint.isActive = false
        let lBottomConstaint = messageTextView.bottomAnchor.constraint(equalTo: bottomAnchor)
        lBottomConstaint.isActive = false
        timeInLineConstraints.append(lBottomConstaint)
        timeInLineConstraints.append(lTrailingConstraint)
        timeInBottomAndLineConstraints.append(lBottomConstaint)
        
        messageEmptyHeightConstraint = messageTextView.heightAnchor.constraint(equalToConstant: 0)
    }
    
    private func timeConstraints()
    {
        timeView.translatesAutoresizingMaskIntoConstraints = false
        timeView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -TIME_LEFT_OFFSET).isActive = true
        timeView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -TIME_LEFT_OFFSET).isActive = true
    }
    
    private func updateTimeLayout() {
        let position = messageTextView.endOfDocument
        let caretRect = messageTextView.caretRect(for: position)
        let canBeInLine = caretRect.maxX < maxWidth - OFFSET_FOR_DETAIL - TIME_LEFT_OFFSET - TIME_BORDER - TIME_BORDER - TIME_LEFT_OFFSET - TIME_LEFT_OFFSET
        let numLines = messageTextView.numberOfLines
        let size = messageTextView.attributedText.boundingRect(with: CGSize(width: maxWidth, height: CGFloat.greatestFiniteMagnitude),
                                                               options: [.usesLineFragmentOrigin, .usesFontLeading],
                                                               context: nil)
        if messageTextView.attributedText.string.count == 0 || (canBeInLine && numLines > 1 && size.width >= maxWidth - 32) {
            activate(false, constraintsIn: timeInBottomConstraints)
            activate(false, constraintsIn: timeInLineConstraints)
            activate(true, constraintsIn: timeInBottomAndLineConstraints)
        }
        else if canBeInLine {
            activate(false, constraintsIn: timeInBottomAndLineConstraints)
            activate(false, constraintsIn: timeInBottomConstraints)
            activate(true, constraintsIn: timeInLineConstraints)
        } else {
            activate(false, constraintsIn: timeInBottomAndLineConstraints)
            activate(false, constraintsIn: timeInLineConstraints)
            activate(true, constraintsIn: timeInBottomConstraints)
        }
        layoutIfNeeded()
    }
    
    private func activate(_ activate: Bool, constraintsIn constraintsArray: [NSLayoutConstraint]) {
        for con in constraintsArray {
            con.isActive = activate
        }
    }
    
    override func layoutSubviews() {
        separatorWidthConstraint?.constant = self.frame.size.width - (PSDMessageView.distToBoard*2)//change separator width, or it will resize its parent view
        updateTimeLayout()
    }
    override var intrinsicContentSize: CGSize{
        return CGSize.zero
    }
}

extension UIFont {
    static let ratingLabel = CustomizationHelper.systemFont(ofSize: 40)
    static let messageTextView = CustomizationHelper.systemFont(ofSize: 18.0)
}

private extension PSDMessageView {
    var TIME_ALPHA: CGFloat { 0.4 }
    var TIME_VIEW_ALPHA: CGFloat { 0.5 }
    var TIME_HEIGHT: CGFloat { 20 }
    var TIME_LEFT_OFFSET: CGFloat { 8 }
    var TIME_BORDER: CGFloat { 4 }
}

private extension UITextView {
    var numberOfLines: Int {
        guard let textStorage = layoutManager.textStorage else { return 0 }
        var count = 0
        layoutManager.enumerateLineFragments(forGlyphRange: NSMakeRange(0, layoutManager.numberOfGlyphs)) { _, _, _, _, _ in
            count += 1
        }
        return count
    }
}
