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
    
    var maxWidth: CGFloat = 0 {
        didSet {
            maxWidthConstraint?.constant = maxWidth
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(backView)
        backView.addSubview(label)
        backView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        backView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        backView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        backView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
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
        
        let trailingConstant = label.trailingAnchor.constraint(equalTo: backView.trailingAnchor, constant: -DIST)
        trailingConstant.priority = UILayoutPriority(rawValue: 999)
        trailingConstant.isActive = true
        
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
}

private extension UIFont {
    static let label = CustomizationHelper.systemFont(ofSize: 16)
}
