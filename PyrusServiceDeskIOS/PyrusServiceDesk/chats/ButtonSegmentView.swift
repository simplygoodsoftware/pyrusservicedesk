import UIKit
protocol ButtonSegmentViewDelegate: NSObjectProtocol {
    func changeSelection(_ sender: UIButton)
}

class ButtonSegmentView: UIButton {
    weak var delegate: ButtonSegmentViewDelegate?
    
    func updateTitle(_ title: String?) {
        titleView.text = title
    }
    
    func updateBadge(_ badge: Int?) {
        if let badgeCounter = badge, badgeCounter > 0 {
            badgeView.isHidden = false
            badgeTrailingConstraint?.isActive = true
            labelTrailingConstraint?.isActive = false
            layoutIfNeeded()
            badgeView.badgeValue = "\(badgeCounter)"
        } else {
            badgeView.badgeValue = nil
            badgeView.isHidden = true
            badgeTrailingConstraint?.isActive = false
            labelTrailingConstraint?.isActive = true
        }
    }
    
    private var labelTrailingConstraint: NSLayoutConstraint?
    private var badgeTrailingConstraint: NSLayoutConstraint?
    
    private lazy var badgeView: NewBadgeView = {
        let badgeView = NewBadgeView()
        badgeView.type = .ListBadge
        badgeView.translatesAutoresizingMaskIntoConstraints = false
        badgeView.heightAnchor.constraint(equalToConstant: NewBadgeView.height).isActive = true
        badgeView.widthAnchor.constraint(greaterThanOrEqualToConstant: NewBadgeView.height).isActive = true
        return badgeView
    }()
    
    lazy var titleView: UILabel = {
        let label = UILabel()
        label.tag = 1
        label.font = UIFont.systemFont(ofSize: 16.0, weight: .medium)
        label.textColor = .secondTintColor
        label.textAlignment = .center
        label.sizeToFit()
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        addTarget(self, action: #selector(tapOnButton), for: .touchUpInside)
        setTitle("", for: .normal)
        titleLabel?.font = UIFont.systemFont(ofSize: 16.0, weight: .medium)
        setTitleColor(.secondTintColor, for: .normal)
        translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(titleView)
        titleView.centerYAnchor.constraint(equalTo: centerYAnchor, constant: -6).isActive = true
        
        addSubview(badgeView)
        badgeView.centerYAnchor.constraint(equalTo: titleView.centerYAnchor).isActive = true
        badgeView.leadingAnchor.constraint(equalTo: titleView.trailingAnchor, constant: 5).isActive = true
        badgeTrailingConstraint = badgeView.trailingAnchor.constraint(equalTo: trailingAnchor)
        badgeTrailingConstraint?.isActive = true
        
        labelTrailingConstraint = titleView.trailingAnchor.constraint(equalTo: trailingAnchor)
        labelTrailingConstraint?.isActive = false
        
        leadingAnchor.constraint(equalTo: titleView.leadingAnchor).isActive = true
    }
    
    @objc private func tapOnButton() {
        delegate?.changeSelection(self)
    }
}

enum NewBadgeViewType {
    case createTask, ListBadge
}
///Custom view to show badge near to views
final class NewBadgeView: UIView {
    static let height: CGFloat = 18
    var type: NewBadgeViewType = .createTask {
        didSet {
            switch type {
            case .createTask:
                badgeLabel.font =  .label
            case .ListBadge:
                badgeLabel.font =  .labelDefault
            }
        }
    }
    
    var badgeValue: String? {
        didSet {
            badgeLabel.text = badgeValue
            isHidden = badgeValue?.count ?? 0 == 0
        }
    }
    
    private lazy var badgeLabel: UILabel = {
        let label = UILabel()
        label.text = nil
        label.textAlignment = .center
        label.textColor = .black
        label.font = .label
        label.adjustsFontSizeToFitWidth = true
        label.isUserInteractionEnabled = false
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor.blue
        isHidden = true
        layer.cornerRadius = NewBadgeView.height / 2
        layer.masksToBounds = true
        isUserInteractionEnabled = false
        
        addSubview(badgeLabel)
        badgeLabel.translatesAutoresizingMaskIntoConstraints = false
        badgeLabel.rightAnchor.constraint(equalTo: rightAnchor, constant: -LABEL_INSET).isActive = true
        badgeLabel.leftAnchor.constraint(equalTo: leftAnchor, constant: LABEL_INSET).isActive = true
        badgeLabel.topAnchor.constraint(equalTo: topAnchor).isActive = true
        badgeLabel.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

//MARK: UI helpers
private extension NewBadgeView {
    var LABEL_INSET: CGFloat { 3 }
}

private extension UIFont {
    static let label = UIFont.systemFont(ofSize: 10)
    static let labelDefault = UIFont.systemFont(ofSize: 10)
}
