import UIKit

final class PSDSystemMessageCell: UITableViewCell {

    static let reuseIdentifier = "PSDSystemMessageCell"

    private lazy var messageLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14)
        label.textColor = CustomizationHelper.textColorForTable
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupUI() {
        contentView.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor
        contentView.addSubview(messageLabel)
        NSLayoutConstraint.activate([
            messageLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            messageLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            messageLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            messageLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            contentView.bottomAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 4)
        ])
    }

    func configure(with message: PSDRowMessage) {
        messageLabel.text = message.text
    }
}
