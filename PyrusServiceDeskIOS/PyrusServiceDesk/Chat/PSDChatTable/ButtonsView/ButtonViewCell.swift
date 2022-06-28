import Foundation

class ButtonViewCell: UICollectionViewCell {
    private let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? UIColor.darkAppColor
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
    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(backView)
        backView.addSubview(label)
        backView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        backView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        backView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        backView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        backView.heightAnchor.constraint(greaterThanOrEqualToConstant: MIN_HEIGHT).isActive = true
        
        label.leadingAnchor.constraint(equalTo: backView.leadingAnchor, constant: DIST).isActive = true
        label.topAnchor.constraint(equalTo: backView.topAnchor, constant: DIST).isActive = true
        label.bottomAnchor.constraint(equalTo: backView.bottomAnchor, constant: -DIST).isActive = true
        label.trailingAnchor.constraint(equalTo: backView.trailingAnchor, constant: -DIST).isActive = true
        
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
    static let label = UIFont.systemFont(ofSize: 16)
}
