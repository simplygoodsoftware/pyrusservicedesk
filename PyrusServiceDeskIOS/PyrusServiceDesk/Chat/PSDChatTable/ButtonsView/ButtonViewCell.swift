import Foundation

class ButtonViewCell: UICollectionViewCell {
    private let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? UIColor.darkAppColor
    private var maxWidthConstraint: NSLayoutConstraint?
    private lazy var label: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.textColor = color
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        return label
    }()
    
    private var linkIconWidthConstraint: NSLayoutConstraint?
    private var linkIconLeadingConstraint: NSLayoutConstraint?
    private lazy var linkIcon: UIImageView = {
        let linkIcon = UIImageView()
        let image = UIImage.PSDImage(name: "linkIcon")?.withRenderingMode(.alwaysTemplate)
        linkIcon.image = image
        linkIcon.tintColor = color
        linkIcon.translatesAutoresizingMaskIntoConstraints = false
        linkIcon.heightAnchor.constraint(equalToConstant: LINK_ICON_SIZE.height).isActive = true
        linkIconWidthConstraint = linkIcon.widthAnchor.constraint(equalToConstant: LINK_ICON_SIZE.width)
        linkIconWidthConstraint?.isActive = true
        return linkIcon
    }()
    
    private lazy var backView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.layer.cornerRadius = BORDER_RADIUS
        view.layer.borderWidth = BORDER_WIDTH
        view.layer.borderColor = color.cgColor
        view.isUserInteractionEnabled = false
        return view
    }()
    
    var text: String = "" {
        didSet {
            label.text = text
        }
    }
    
    var showLinkIcon: Bool = false {
        didSet {
            linkIcon.isHidden = !showLinkIcon
            linkIconWidthConstraint?.constant = showLinkIcon ? LINK_ICON_SIZE.width : 0
            linkIconLeadingConstraint?.constant = showLinkIcon ? -DIST : 0
        }
    }
    
    var maxWidth: CGFloat = 0 {
        didSet {
            maxWidthConstraint?.constant = maxWidth
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(backView)
        backView.addSubview(label)
        backView.addSubview(linkIcon)
        
        backView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        backView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        backView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        backView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        
        linkIcon.centerYAnchor.constraint(equalTo: backView.centerYAnchor).isActive = true
        linkIcon.trailingAnchor.constraint(equalTo: backView.trailingAnchor, constant: -DIST).isActive = true
        
        let heightConstant = backView.heightAnchor.constraint(greaterThanOrEqualToConstant: MIN_HEIGHT)
        heightConstant.priority = UILayoutPriority(rawValue: 999)
        heightConstant.isActive = true
        
        let leadingConstant = label.leadingAnchor.constraint(equalTo: backView.leadingAnchor, constant: DIST)
        leadingConstant.priority = UILayoutPriority(rawValue: 999)
        leadingConstant.isActive = true
        
        let topConstant = label.topAnchor.constraint(equalTo: backView.topAnchor, constant: DIST)
        topConstant.priority = UILayoutPriority(rawValue: 999)
        topConstant.isActive = true
        
        let bottomConstant = label.bottomAnchor.constraint(equalTo: backView.bottomAnchor, constant: -DIST)
        bottomConstant.priority = UILayoutPriority(rawValue: 999)
        bottomConstant.isActive = true
        
        linkIconLeadingConstraint = label.trailingAnchor.constraint(equalTo: linkIcon.leadingAnchor, constant: -DIST)
        linkIconLeadingConstraint?.priority = UILayoutPriority(rawValue: 999)
        linkIconLeadingConstraint?.isActive = true
        
        maxWidthConstraint = backView.widthAnchor.constraint(lessThanOrEqualToConstant: maxWidth)
        maxWidthConstraint?.priority = UILayoutPriority(rawValue: 999)
        maxWidthConstraint?.isActive = true
        label.setContentCompressionResistancePriority(.defaultHigh, for: .vertical)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
}

private extension ButtonViewCell {
    var BORDER_WIDTH: CGFloat { 1 }
    var BORDER_RADIUS: CGFloat { 5 }
    var DIST: CGFloat { 8 }
    var MIN_HEIGHT: CGFloat { 40 }
    var LINK_ICON_SIZE: CGSize { CGSize(width: 20, height: 20) }
}

private extension UIFont {
    static let label = UIFont.systemFont(ofSize: 16)
}
