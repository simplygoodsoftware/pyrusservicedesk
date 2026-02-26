import UIKit

final class NewAnnouncementsCell: UITableViewCell {
    static let identifier = "NewAnnouncementsCell"

    private let cardView: UIView = {
        let v = UIView()
        v.translatesAutoresizingMaskIntoConstraints = false
        v.backgroundColor = .bubbleViewColor
        v.layer.cornerRadius = 16
        v.layer.masksToBounds = true
        return v
    }()

    private let avatars = AvatarPileStackView()
    private let titleLabel: UILabel = {
        let l = UILabel()
        l.font = .systemFont(ofSize: 13, weight: .semibold)
        l.textColor = .label
        l.numberOfLines = 1
        return l
    }()

    private let subtitleLabel: UILabel = {
        let l = UILabel()
        l.font = .systemFont(ofSize: 15, weight: .regular)
        l.textColor = .label
        l.numberOfLines = 1
        return l
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        contentView.backgroundColor = .clear
        setupUI()
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with vm: NewAnnouncementsModel) {
        avatars.set(images: vm.logos)
        titleLabel.text = vm.title
        subtitleLabel.text = vm.subtitle
    }

    private func setupUI() {
        avatars.translatesAutoresizingMaskIntoConstraints = false
        avatars.itemSize = 16
        avatars.overlap = 8
        avatars.maxVisible = 6
        avatars.showsCounter = true

        let hStack = UIStackView(arrangedSubviews: [avatars, titleLabel])
        hStack.axis = .horizontal
        hStack.alignment = .leading
        hStack.spacing = 4
        
        let vStack = UIStackView(arrangedSubviews: [hStack, subtitleLabel])
        vStack.axis = .vertical
        vStack.alignment = .leading
        vStack.spacing = 4
        vStack.translatesAutoresizingMaskIntoConstraints = false

        contentView.addSubview(cardView)
        cardView.addSubview(vStack)

        NSLayoutConstraint.activate([
            cardView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            cardView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 8),
            cardView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -8),
            cardView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -12),

            vStack.topAnchor.constraint(equalTo: cardView.topAnchor, constant: 10),
            vStack.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: 10),
            vStack.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -10),
            vStack.bottomAnchor.constraint(equalTo: cardView.bottomAnchor, constant: -10),

            avatars.heightAnchor.constraint(equalToConstant: 16),
            // минимальная ширина под хотя бы один логотип
            avatars.widthAnchor.constraint(greaterThanOrEqualToConstant: 16)
        ])
    }
}

private extension UIColor {
    static let bubbleViewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#2C2C2F") ?? .systemGray6
        default:
            return UIColor(hex: "#F4F5F7") ?? .systemGray6
        }
    }
}
