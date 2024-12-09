import UIKit

protocol ClosedTicketsCellDelegate: NSObject {
    func redrawChats(open: Bool)
}

@available(iOS 13.0, *)
class ClosedTicketsCell: UITableViewCell {
    static var identifier = "ClosedTicketsCell"
    
    weak var delegate: ClosedTicketsCellDelegate?
    
    private var isOpen: Bool = false {
        didSet {
            openButton.setImage(UIImage(systemName: isOpen ? "chevron.up" : "chevron.down")?.imageWith(color: PyrusServiceDesk.mainController?.customization?.themeColor ?? .blue), for: .normal)
        }
    }
    
    private let countLabel: UILabel = {
        let label = UILabel()
        label.textColor = .secondColor
        label.font = .countLabel
        label.adjustsFontSizeToFitWidth = true
        return label;
    }()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .psdLabel
        label.font = .titleLabel
        label.lineBreakMode = .byTruncatingTail
        return label;
    }()
    
    private let openButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(systemName: "chevron.down")?.imageWith(color: PyrusServiceDesk.mainController?.customization?.themeColor ?? .blue), for: .normal)
        return button;
    }()
    
    private static let x : CGFloat = 16.0
    private static let notificationSize : CGFloat = 20.0
    private static let trailing : CGFloat = 16.0
    
    func configure(with model: ClosedTicketsCellModel) {
        countLabel.text = "\(model.count)"
        titleLabel.text = model.title
        isOpen = model.isOpen
        delegate = model.delegate
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .psdBackground
        selectedBackgroundView = self.selectedBackground
        
        contentView.addSubview(countLabel)
        contentView.addSubview(titleLabel)
        contentView.addSubview(openButton)
        
        addConstraints()
    }
    
    private lazy var selectedBackground : UIView = {
        let view = UIView()
        view.backgroundColor = .psdLightGray
        return view
    }()
    
    private func addConstraints() {
        countLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        openButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            titleLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            
            countLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            countLabel.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor, constant: 8),
            
            openButton.heightAnchor.constraint(equalToConstant: 28),
            openButton.widthAnchor.constraint(equalToConstant: 28),
            openButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -12),
            openButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
        ])
        
        openButton.addTarget(self, action: #selector(openButtonTapped), for: .touchUpInside)

    }
    
    @objc func openButtonTapped() {
        isOpen = !isOpen
        delegate?.redrawChats(open: isOpen)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private extension UIFont {
    static let countLabel = CustomizationHelper.systemFont(ofSize: 17.0)
    static let titleLabel = CustomizationHelper.systemBoldFont(ofSize: 17)
}

private extension UIColor {
    static let secondColor = UIColor(hex: "#9199A1")
}
