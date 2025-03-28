import Foundation
import UIKit

class SearchChatCell: UITableViewCell {
    static var identifier = "SearchChatCell"
    private let stateSize: CGFloat = 20.0
    
    private let timeLabel: UILabel = {
        let label = UILabel()
        label.textColor = .timeLabel
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
    
    private static let lastMessageLines: Int = 2
    private let lastMessageInfo: UILabel = {
        let label = UILabel()
        label.textColor = .lastMessageInfo
        label.font = .lastMessageInfo
        label.numberOfLines = lastMessageLines
        label.lineBreakMode = .byTruncatingTail
        label.text = ""
        return label;
    }()
    
    private lazy var attachmentIcon: UIImageView = {
        let image = UIImage.PSDImage(name: "paperclip")
        let imageView = UIImageView(image: image?.imageWith(color: .lastMessageInfo))
        imageView.isHidden = true
        return imageView
    }()
    
    private lazy var attachmentName: UILabel = {
        let label = UILabel()
        label.textColor = .lastMessageInfo
        label.font = .lastMessageInfo
        label.isHidden = true
        return label
    }()
    
    private static let x: CGFloat = 16.0
    private static let notificationSize : CGFloat = 20.0
    private static let trailing : CGFloat = 16.0
    
    func configure(with model: SearchChatViewModel) {
        timeLabel.text = model.date
        messageLabel.attributedText = model.subject.removeLinkAttributes()
        lastMessageInfo.attributedText = model.messageText.removeLinkAttributes()
        if model.hasAttachments {
            attachmentIcon.isHidden = false
            attachmentName.isHidden = false
            attachmentName.text = model.attachmentName
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .psdBackgroundColor
        selectedBackgroundView = self.selectedBackground
        
        contentView.addSubview(timeLabel)
        contentView.addSubview(messageLabel)
        contentView.addSubview(lastMessageInfo)
        
        addConstraints()
    }
    
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        attachmentIcon.image = UIImage.PSDImage(name: "paperclip")?.imageWith(color: .lastMessageInfo)
    }
    
    private lazy var selectedBackground : UIView = {
        let view = UIView()
        view.backgroundColor = .psdLightGray
        return view
    }()
    
    private func addConstraints() {
        timeLabel.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        lastMessageInfo.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            messageLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            messageLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            messageLabel.trailingAnchor.constraint(lessThanOrEqualTo: contentView.trailingAnchor, constant: -100),
            
            timeLabel.bottomAnchor.constraint(equalTo: messageLabel.bottomAnchor),
            timeLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            lastMessageInfo.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 2),
            lastMessageInfo.leadingAnchor.constraint(equalTo: messageLabel.leadingAnchor),
            lastMessageInfo.trailingAnchor.constraint(lessThanOrEqualTo: timeLabel.leadingAnchor, constant: -8),
            lastMessageInfo.bottomAnchor.constraint(lessThanOrEqualTo: contentView.bottomAnchor, constant: -8),
        ])
        
        lastMessageInfo.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        timeLabel.setContentCompressionResistancePriority(.defaultHigh, for: .horizontal)
        
        setupAttachment()
    }
    
    private func setupAttachment() {
        contentView.addSubview(attachmentIcon)
        contentView.addSubview(attachmentName)
        attachmentIcon.translatesAutoresizingMaskIntoConstraints = false
        attachmentName.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            attachmentIcon.heightAnchor.constraint(equalToConstant: 20),
            attachmentIcon.widthAnchor.constraint(equalToConstant: 20),
            attachmentIcon.leadingAnchor.constraint(equalTo: lastMessageInfo.trailingAnchor, constant: 0),
            attachmentIcon.centerYAnchor.constraint(equalTo: lastMessageInfo.centerYAnchor),
            
            attachmentName.centerYAnchor.constraint(equalTo: lastMessageInfo.centerYAnchor),
            attachmentName.leadingAnchor.constraint(equalTo: attachmentIcon.trailingAnchor, constant: 0)
        ])
    }
    
    override func prepareForReuse() {
        attachmentIcon.isHidden = true
        attachmentName.isHidden = true
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private extension UIFont {
    static let timeLabel = CustomizationHelper.systemFont(ofSize: 13.0)
    static let messageLabel = CustomizationHelper.systemBoldFont(ofSize: 17)
    static let notificationButton = CustomizationHelper.systemFont(ofSize: 12.0)
    static let lastMessageInfo = CustomizationHelper.systemFont(ofSize: 15.0)
}

private extension UIColor {
    static let timeLabel = UIColor(hex: "#9199A1") ?? .systemGray
    static let secondColor = UIColor(hex: "#FFB049")
    
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}

extension NSAttributedString {
    func removeLinkAttributes() -> NSAttributedString? {
        let mutableAttributedString = NSMutableAttributedString(attributedString: self)
        let range = NSRange(location: 0, length: mutableAttributedString.length)
        
        mutableAttributedString.enumerateAttribute(.link, in: range, options: []) { value, range, _ in
            if value != nil {
                mutableAttributedString.removeAttribute(.link, range: range)
            }
        }
        
        return mutableAttributedString
    }
}
