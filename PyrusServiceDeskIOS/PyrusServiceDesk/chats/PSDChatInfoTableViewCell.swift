
import UIKit

class PSDChatInfoTableViewCell: UITableViewCell {
    private let timeLabel: UILabel = {
        let label = UILabel()
        label.textColor = .psdGray
        label.font = .timeLabel
        label.adjustsFontSizeToFitWidth = true
        return label;
    }()
    private static let messageLabelLines: Int = 1
    private let messageLabel: UILabel = {
        let label = UILabel()
        label.textColor = .psdLabel
        label.font = .messageLabel
        label.numberOfLines = messageLabelLines
        label.lineBreakMode = .byTruncatingTail
        return label;
    }()
    private let notificationButton: UIButton = {
        let button = UIButton()
        button.backgroundColor = .appColor
        button .setTitleColor(UIColor.appTextColor, for: .normal)
        button.titleLabel?.font = .notificationButton
        button.isUserInteractionEnabled = false
        button.isHidden = true
        return button;
    }()
    private static let lastMessageLines: Int = 2
    private let lastMessageInfo: UILabel = {
        let label = UILabel()
        label.textColor = .psdGray
        label.font = .lastMessageInfo
        label.numberOfLines = lastMessageLines
        label.lineBreakMode = .byTruncatingTail
        label.text = "Last_Message".localizedPSD()
        return label;
    }()
    
    private static let x : CGFloat = 16.0
    private static let notificationSize : CGFloat = 20.0
    private static let trailing : CGFloat = 16.0
    
    ///The time when last message was
    var time : String = ""
    {
        didSet{
            timeLabel.text = time
            timeLabel .sizeToFit()
        }
    }
    var firstMessageText : String = ""
    {
        didSet{
            messageLabel.text = firstMessageText
            messageLabel .sizeToFit()
        }
    }
    var lastMessageText : String = ""
    {
        didSet{
            lastMessageInfo.text = lastMessageText
            lastMessageInfo .sizeToFit()
        }
    }
    var notifacationNumber : NSNumber = 0
    {
        didSet{
            if notifacationNumber==0{
                notificationButton.isHidden = true
                notificationButton .setTitle("", for: .normal)
            }
            else{
                notificationButton.isHidden = false
                notificationButton .setTitle(notifacationNumber.stringValue, for: .normal)
            }
        }
    }
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        self.backgroundColor = .psdBackground
        self.contentView.backgroundColor = .psdBackground
        
        self.accessoryType = .disclosureIndicator
        
        self.contentView.addSubview(timeLabel)
        
        self.contentView.addSubview(messageLabel)
        
        self.contentView.addSubview(lastMessageInfo)
        
        self.contentView.addSubview(notificationButton)
        
        self.selectedBackgroundView = self.selectedBackground
        
        notificationButton.layer.cornerRadius=PSDChatInfoTableViewCell.notificationSize/2
        
        addConstraints()
    }
    private lazy var selectedBackground : UIView = {
        let view = UIView()
        view.backgroundColor = .psdLightGray
        return view
    }()
    private static let distanseToBoard:CGFloat = 12.0
    private func addConstraints()
    {
        timeLabel.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        lastMessageInfo.translatesAutoresizingMaskIntoConstraints = false
        notificationButton.translatesAutoresizingMaskIntoConstraints = false
        
        addLeadingConstraints(to: timeLabel)
        addLeadingConstraints(to: messageLabel)
        addLeadingConstraints(to: lastMessageInfo)
        
        let y : CGFloat = 7
        
        
        timeLabel.addConstraint([.top], constant: PSDChatInfoTableViewCell.distanseToBoard)
        
        self.contentView.addConstraint(NSLayoutConstraint(
            item: timeLabel,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: messageLabel,
            attribute: .top,
            multiplier: 1,
            constant:-y))
        
        self.contentView.addConstraint(NSLayoutConstraint(
            item: messageLabel,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: lastMessageInfo,
            attribute: .top,
            multiplier: 1,
            constant:-y))
        

        lastMessageInfo.addConstraint([.bottom], constant: -PSDChatInfoTableViewCell.distanseToBoard)
        
        notificationButton.addConstraint([.centerY,.trailing], constant: 0)
        notificationButton.addSizeConstraint([.height,.width], constant: PSDChatInfoTableViewCell.notificationSize)
        
        addTrailingConstraints(to: timeLabel)
        addTrailingConstraints(to: messageLabel)
        addTrailingConstraints(to: lastMessageInfo)
        
    }
    /**
     Add leading constraint equal to x
     @param view view is item to add constraint
     */
    private func addLeadingConstraints(to view:UIView)
    {
        view.addConstraint([.leading], constant: PSDChatInfoTableViewCell.x)
    }
    /**
     Add trailing constraint equal to notificationButton
     @param view view is item to add constraint
     */
    private func addTrailingConstraints(to view:UIView)
    {
        self.contentView.addConstraint(NSLayoutConstraint(
            item: view,
            attribute: .trailing,
            relatedBy: .equal,
            toItem: notificationButton,
            attribute: .leading,
            multiplier: 1,
            constant:-PSDChatInfoTableViewCell.x))
    }
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func layoutSubviews() {
        super.layoutSubviews()
    }
}
private extension UIFont {
    static let timeLabel = CustomizationHelper.systemFont(ofSize: 15.0)
    static let messageLabel = CustomizationHelper.systemFont(ofSize: 17)
    static let notificationButton = CustomizationHelper.systemFont(ofSize: 12.0)
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}
