
import UIKit
let AVATAR_SIZE : CGFloat = 32.0


class PSDChatMessageCell: UITableViewCell {
   
    var needShowName = false
    var drawEmpty = false
    private static let timeAlpha : CGFloat = 0.4
    ///Cloud that show message body (text OR attachment)
    let cloudView : PSDMessageView =
    {
        let view = PSDMessageView()
        return view;
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        self.backgroundColor = .clear
        self.contentView.translatesAutoresizingMaskIntoConstraints = true
        self.selectionStyle = .none
        contentView.addSubview(cloudView)
        self.addConstraints()
        
    }
    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
        if(selected){
            self.contentView.backgroundColor = CustomizationHelper.lightGrayViewColor
        }
        else{
            self.contentView.backgroundColor = .clear
        }
    }
    func draw(message:PSDRowMessage, width: CGFloat)
    {
        cloudView.maxWidth =  maxMessageWidth(width)
        cloudView.draw(message: message)
    }
    
    //MARK: constraints block
    
    private func maxMessageWidth(_ width: CGFloat) -> CGFloat {
        return width - (TO_BOARD_DISTANCE*3) - (AVATAR_SIZE*2)
    }
    private func addConstraints()
    {
        sameConstraints()
    }
    var firstMessageInDate : Bool = false
    var topMessageConstraint : NSLayoutConstraint?
    let bottomDistance : CGFloat = 2.0
    static let nameTopDistance : CGFloat = 10.0
    private func sameConstraints(){
        
        
        cloudView.translatesAutoresizingMaskIntoConstraints = false
        topMessageConstraint = NSLayoutConstraint(
            item: cloudView,
            attribute: .top,
            relatedBy: .equal,
            toItem: cloudView.superview!,
            attribute: .top,
            multiplier: 1,
            constant:bottomDistance)
        self.contentView.addConstraint(topMessageConstraint!)
        
        let botttomConstraint = NSLayoutConstraint(
            item: cloudView.superview!,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: cloudView,
            attribute: .bottom,
            multiplier: 1,
            constant:bottomDistance)
        botttomConstraint.priority = UILayoutPriority(rawValue: 999)
        self.contentView.addConstraint(botttomConstraint)
        
        
    }
    
   
   
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
extension PSDChatMessageCell: Recolorable {
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
        }
        recolor()
    }
    
    @objc func recolor(){}
}
