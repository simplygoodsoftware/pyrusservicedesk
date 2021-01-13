
import UIKit

class PSDSupportMessageCell: PSDChatMessageCell {
    private static let personNameAlpha : CGFloat = 0.6
    
    ///Is nameLabel need to be shown. (Show only next to first user's message)
    ///A label with name of person show only needShowName = true
    private let nameLabel : UILabel =
    {
        let label = UILabel()
        label.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(personNameAlpha)
        label.font = .nameLabel
        label.text = ""
        return label;
    }()
    override func recolor() {
        super.recolor()
        nameLabel.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(PSDSupportMessageCell.personNameAlpha)
    }
    ///Is avatarView need to be shown. (Shhow only next with last user's avatar)
    var needShowAvatar = false
    ///UIImageView with person avatar image or default image. Has only "support" users. Show only if needShowAvatar
    let avatarView : PSDAvatarImageView =
    {
        let imageView = PSDAvatarImageView(frame: CGRect(x: 0, y: 0, width: AVATAR_SIZE, height: AVATAR_SIZE))
        return imageView;
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(avatarView)
        cloudView.color = .defaultColor
        self.contentView.addSubview(nameLabel)
        self.nameLabel.sizeToFit()
        
        addConstraints()
        
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func draw(message: PSDRowMessage)
    {
        super.draw(message: message)
        let name = message.message.owner.name?.count ?? 0 > 0 ? message.message.owner.name : (message.message.owner as? PSDPlaceholderUser == nil ? "" : " ")
        self.nameLabel.text = needShowName ? name :  ""
        self.avatarView.isHidden = !needShowAvatar
        

        self.nameLabel.sizeToFit()
        
        self.updateNameConstraint()
        self.updateTopMessageConstrint()
        
        self.layoutIfNeeded()
    }
    private func addConstraints()
    {
        supportMessageConstraints()
        nameConstraints()
    }
    private let nameLeftSpace : CGFloat = 10.0
    private func nameConstraints(){
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        self.contentView.addConstraint(NSLayoutConstraint(
            item: nameLabel,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: cloudView,
            attribute: .top,
            multiplier: 1,
            constant:0))
        self.contentView.addConstraint(NSLayoutConstraint(
            item: nameLabel,
            attribute: .leading,
            relatedBy: .equal,
            toItem: cloudView,
            attribute: .leading,
            multiplier: 1,
            constant:nameLeftSpace))
        nameConstraint = NSLayoutConstraint(
            item: nameLabel,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:self.nameLabel.frame.size.height)
        self.contentView.addConstraint(nameConstraint!)
    }
    private func supportMessageConstraints()
    {
        avatarView.translatesAutoresizingMaskIntoConstraints = false
        avatarView.addConstraint([.leading], constant: TO_BOARD_DISTANCE)
        avatarView.addSizeConstraint([.height,.width],constant:AVATAR_SIZE)
        avatarView.addConstraint([.bottom], constant: -bottomDistance)
        
        self.contentView.addConstraint(NSLayoutConstraint(
            item: cloudView,
            attribute: .trailing,
            relatedBy: .lessThanOrEqual,
            toItem: cloudView.superview!,
            attribute: .trailing,
            multiplier: 1,
            constant:-(AVATAR_SIZE+(TO_BOARD_DISTANCE*2))))
        
        
        cloudView.addConstraint([.leading], constant: AVATAR_SIZE+(TO_BOARD_DISTANCE*2))
    }
    
    ///Name height constraint
    var nameConstraint : NSLayoutConstraint?
    ///Update name height according to new name text
    private func updateNameConstraint(){
        var newConstraint : CGFloat = 0.0
        if(self.nameLabel.frame.size.height>0){
            newConstraint = self.nameLabel.frame.size.height + bottomDistance
        }
        if(nameConstraint?.constant != newConstraint){
            nameConstraint?.constant = newConstraint
        }
    }
    
    private func updateTopMessageConstrint(){
        let topDistanse = firstMessageInDate ? 0 : PSDChatMessageCell.nameTopDistance
        self.topMessageConstraint?.constant = needShowName ? self.nameLabel.frame.size.height + topDistanse : bottomDistance
       
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }
}
private extension UIFont {
    static let nameLabel = CustomizationHelper.systemFont(ofSize: 14)
}
