import UIKit

final class AnnouncementsReadCell: UITableViewCell {
    static let identifier = "AnnouncementsReadCell"

    private let checkImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "circleCheck"))
        imageView.contentMode = .scaleAspectFit
        imageView.clipsToBounds = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.layer.cornerRadius = 14
        return imageView
    }()
    
    private let messageLabel: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.font = .systemFont(ofSize: 12)
        label.textColor = .messageTextColor
        label.text = "ReadAnnoncements".localizedPSD()
        return label
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .psdDarkBackgroundColor
        contentView.backgroundColor = .psdDarkBackgroundColor
        
        
        contentView.addSubview(checkImageView)
        contentView.addSubview(messageLabel)

        NSLayoutConstraint.activate([
            checkImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 20),
            checkImageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            
            messageLabel.topAnchor.constraint(equalTo: checkImageView.bottomAnchor, constant: 8),
            messageLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
}

private extension UIColor {
    static let messageTextColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#A9A9AD") ?? .systemGray
        default:
            return UIColor(hex: "#797A7F") ?? .systemGray
        }
    }
}
