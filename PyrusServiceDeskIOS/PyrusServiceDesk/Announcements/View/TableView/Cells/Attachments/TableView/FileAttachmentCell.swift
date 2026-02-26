
final class FileAttachmentCell: UITableViewCell {
    static let reuseID = "FileAttachmentCell"

    var onTapIcon: (() -> Void)?

    private let iconView = UIImageView()
    private let titleLabel = UILabel()
    private let subtitleLabel = UILabel()
    private let labelsStack = UIStackView()

    private lazy var iconButton: UIButton = {
        let b = UIButton(type: .system)
        b.backgroundColor = .clear
        b.addTarget(self, action: #selector(iconTapped), for: .touchUpInside)
        return b
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .clear
        contentView.backgroundColor = .clear

        // Icon image
        iconView.translatesAutoresizingMaskIntoConstraints = false
        iconView.contentMode = .scaleAspectFit
        iconView.layer.cornerRadius = 23

        // Labels
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = .boldSystemFont(ofSize: 15)
        titleLabel.textColor = .label
        titleLabel.numberOfLines = 1
        titleLabel.lineBreakMode = .byTruncatingMiddle

        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.font = .boldSystemFont(ofSize: 15)
        subtitleLabel.textColor = .secondaryLabel
        subtitleLabel.numberOfLines = 1

        labelsStack.axis = .vertical
        labelsStack.alignment = .leading
        labelsStack.spacing = 4
        labelsStack.translatesAutoresizingMaskIntoConstraints = false
        labelsStack.addArrangedSubview(titleLabel)
        labelsStack.addArrangedSubview(subtitleLabel)

        contentView.addSubview(iconView)
        contentView.addSubview(labelsStack)

        iconButton.translatesAutoresizingMaskIntoConstraints = false
        iconView.addSubview(iconButton)

        NSLayoutConstraint.activate([
            iconView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 10),
            iconView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 46),
            iconView.heightAnchor.constraint(equalTo: iconView.widthAnchor),

            iconButton.topAnchor.constraint(equalTo: iconView.topAnchor),
            iconButton.leadingAnchor.constraint(equalTo: iconView.leadingAnchor),
            iconButton.trailingAnchor.constraint(equalTo: iconView.trailingAnchor),
            iconButton.bottomAnchor.constraint(equalTo: iconView.bottomAnchor),

            labelsStack.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 10),
            labelsStack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            labelsStack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -10),
            labelsStack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8),
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func prepareForReuse() {
        super.prepareForReuse()
        onTapIcon = nil
        titleLabel.text = nil
        subtitleLabel.text = nil
        iconView.image = nil
    }

    func configure(with item: AnnouncementCellAttachmentModel) {
        titleLabel.text = item.attachment.name
        subtitleLabel.text = AnnouncementsHelper.formatDataSize(item.attachment.size, spacing: true)
        iconView.image = item.isRead ? UIImage(named: "annsFile") : UIImage(named: "newAnnsFile")
    }

    @objc private func iconTapped() {
        onTapIcon?()
        UIView.animate(withDuration: 0.08, animations: {
            self.iconView.alpha = 0.5
        }, completion: { _ in
            UIView.animate(withDuration: 0.08) {
                self.iconView.alpha = 1.0
            }
        })
    }
}
