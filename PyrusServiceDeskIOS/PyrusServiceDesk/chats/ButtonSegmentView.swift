import UIKit

protocol ButtonSegmentViewDelegate: NSObjectProtocol {
    func changeSelection(_ sender: UIButton)
}

class ButtonSegmentView: UIButton {
    weak var delegate: ButtonSegmentViewDelegate?
    
    func updateTitle(_ title: String?) {
        titleView.text = title
    }
    
    private var labelTrailingConstraint: NSLayoutConstraint?
    private var badgeTrailingConstraint: NSLayoutConstraint?
    
    lazy var titleView: UILabel = {
        let label = UILabel()
        label.tag = 1
        label.font = UIFont.systemFont(ofSize: 15.0)
        label.textColor = .lightGray
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
        titleLabel?.font = UIFont.systemFont(ofSize: 13.0)
        setTitleColor(.psdGray, for: .normal)
        translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(titleView)
        titleView.centerYAnchor.constraint(equalTo: centerYAnchor, constant: -6).isActive = true
        
        labelTrailingConstraint = titleView.trailingAnchor.constraint(equalTo: trailingAnchor)
        labelTrailingConstraint?.isActive = false
        
        leadingAnchor.constraint(equalTo: titleView.leadingAnchor).isActive = true
    }
    
    @objc private func tapOnButton() {
        delegate?.changeSelection(self)
    }
}

