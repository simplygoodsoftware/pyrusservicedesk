class RateViewCell: UICollectionViewCell {
    private let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? UIColor.darkAppColor
    private var maxWidthConstraint: NSLayoutConstraint?
    private lazy var label: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 28)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var backView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.layer.cornerRadius = BORDER_RADIUS
        view.backgroundColor = CustomizationHelper.supportMassageBackgroundColor
        view.isUserInteractionEnabled = false
        return view
    }()
    
    var text: String = "" {
        didSet {
            label.text = text
            if text == "👎" {
                label.transform = CGAffineTransform(scaleX: -1, y: 1)
            }
                
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
        
        NSLayoutConstraint.activate([
            backView.heightAnchor.constraint(equalToConstant: MIN_HEIGHT),
            backView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            backView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            
            label.centerXAnchor.constraint(equalTo: backView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: backView.centerYAnchor),
//            label.heightAnchor.constraint(equalToConstant: 28),
//            label.widthAnchor.constraint(equalToConstant: 28),
            
            contentView.topAnchor.constraint(equalTo: backView.topAnchor),
            contentView.bottomAnchor.constraint(equalTo: backView.bottomAnchor),
            contentView.leadingAnchor.constraint(equalTo: backView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: backView.trailingAnchor),
        ])
        
        maxWidthConstraint = backView.widthAnchor.constraint(equalToConstant: maxWidth)
        maxWidthConstraint?.priority = UILayoutPriority(rawValue: 999)
        maxWidthConstraint?.isActive = true
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
}

private extension RateViewCell {
    var BORDER_WIDTH: CGFloat { 1 }
    var BORDER_RADIUS: CGFloat { 6 }
    var DIST: CGFloat { 8 }
    var MIN_HEIGHT: CGFloat { 40 }
}

private extension UIFont {
    static let label = CustomizationHelper.systemFont(ofSize: 16)
}
