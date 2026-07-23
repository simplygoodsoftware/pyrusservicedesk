import UIKit

protocol AnnouncementsAttachmentsDelegate: AnyObject {
    func selectAttachment(cell: UITableViewCell, index: Int)
}

final class PSDAnnouncementCell: UITableViewCell {
    static let identifier = "PSDAnnouncementCell"

    private enum Layout {
        static let bubbleCornerRadius: CGFloat = 16
        static let bubbleHorizontalInset: CGFloat = 8
        static let bubbleTopToClientIcon: CGFloat = 4
        static let contentHorizontalInset: CGFloat = 10
        static let gridInset: CGFloat = 2
        static let clientIconSize: CGFloat = 20
        static let clientIconCornerRadius: CGFloat = 10
        static let clientIconTop: CGFloat = 12
        static let clientNameSpacing: CGFloat = 4
        static let messageTopSpacing: CGFloat = 6
        static let timeTopSpacing: CGFloat = 2
        static let timeBottomSpacing: CGFloat = 10
        static let filesTopWithImages: CGFloat = 4
        static let filesTopWithoutImages: CGFloat = 8
        static let filesTopImagesOnly: CGFloat = 0
        static let filesTopDefault: CGFloat = 4
        static let filesBottomSpacingWithText: CGFloat = 6
        static let messageFontSize: CGFloat = 16
        static let clientNameFontSize: CGFloat = 15
        static let timeFontSize: CGFloat = 13
    }

    // MARK: - Subviews

    private let bubbleView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = Layout.bubbleCornerRadius
        view.layer.masksToBounds = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private var zeroMessageLabelHeight: NSLayoutConstraint?
    /// Ссылки (в т.ч. телефонные) кладутся в атрибуты заранее, на фоне —
    /// dataDetectorTypes здесь намеренно НЕ используется: синхронная детекция
    /// при каждом присвоении attributedText фризит скролл.
    private let messageLabel: AnnouncementTextView = {
        let textView = AnnouncementTextView.make()
        textView.font = .systemFont(ofSize: Layout.messageFontSize)
        textView.textColor = .label
        textView.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor
        textView.translatesAutoresizingMaskIntoConstraints = false
        return textView
    }()

    private lazy var timeLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: Layout.timeFontSize)
        label.adjustsFontForContentSizeCategory = true
        label.textColor = .secondaryLabel
        label.setContentCompressionResistancePriority(.required, for: .horizontal)
        label.setContentHuggingPriority(.required, for: .horizontal)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private var imagesGridHeight: NSLayoutConstraint?
    private lazy var imagesGridCollectionView: ImagesGridCollectionView = {
        let collectionView = ImagesGridCollectionView()
        collectionView.backgroundColor = .clear
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.layer.cornerRadius = Layout.bubbleCornerRadius
        collectionView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        collectionView.isScrollEnabled = false
        return collectionView
    }()

    private lazy var clientIcon: UIImageView = {
        let imageView = UIImageView()
        imageView.layer.cornerRadius = Layout.clientIconCornerRadius
        imageView.clipsToBounds = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.backgroundColor = .secondarySystemFill
        return imageView
    }()

    private lazy var clientNameLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: Layout.clientNameFontSize, weight: .medium)
        label.adjustsFontForContentSizeCategory = true
        label.textColor = .label
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private var filesTableTop: NSLayoutConstraint?
    private var filesTableHeight: NSLayoutConstraint?
    private let attachmentsTableView = AttachmentsTableView()

    // MARK: - Init

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .psdDarkBackgroundColor
        contentView.backgroundColor = .psdDarkBackgroundColor
        selectionStyle = .none

        contentView.addSubview(bubbleView)
        setupClientInfo()

        bubbleView.addSubview(imagesGridCollectionView)
        imagesGridHeight = imagesGridCollectionView.heightAnchor.constraint(equalToConstant: 0)
        imagesGridHeight?.isActive = true

        bubbleView.addSubview(attachmentsTableView)
        attachmentsTableView.translatesAutoresizingMaskIntoConstraints = false
        filesTableHeight = attachmentsTableView.heightAnchor.constraint(equalToConstant: 0)
        filesTableHeight?.isActive = true
        filesTableTop = attachmentsTableView.topAnchor.constraint(
            equalTo: imagesGridCollectionView.bottomAnchor,
            constant: Layout.filesTopWithoutImages
        )
        filesTableTop?.isActive = true

        bubbleView.addSubview(messageLabel)
        zeroMessageLabelHeight = messageLabel.heightAnchor.constraint(equalToConstant: 0)

        bubbleView.addSubview(timeLabel)

        NSLayoutConstraint.activate([
            imagesGridCollectionView.topAnchor.constraint(equalTo: bubbleView.topAnchor, constant: Layout.gridInset),
            imagesGridCollectionView.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: Layout.gridInset),
            imagesGridCollectionView.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -Layout.gridInset),

            attachmentsTableView.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor),
            attachmentsTableView.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor),

            bubbleView.topAnchor.constraint(equalTo: clientIcon.bottomAnchor, constant: Layout.bubbleTopToClientIcon),
            bubbleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: Layout.bubbleHorizontalInset),
            bubbleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -Layout.bubbleHorizontalInset),

            messageLabel.topAnchor.constraint(equalTo: attachmentsTableView.bottomAnchor, constant: Layout.messageTopSpacing),
            messageLabel.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: Layout.contentHorizontalInset),
            messageLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -Layout.contentHorizontalInset),

            timeLabel.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: Layout.timeTopSpacing),
            timeLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -Layout.contentHorizontalInset),
            bubbleView.bottomAnchor.constraint(equalTo: timeLabel.bottomAnchor, constant: Layout.timeBottomSpacing),

            contentView.bottomAnchor.constraint(equalTo: bubbleView.bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private func setupClientInfo() {
        contentView.addSubview(clientIcon)
        contentView.addSubview(clientNameLabel)

        NSLayoutConstraint.activate([
            clientIcon.topAnchor.constraint(equalTo: contentView.topAnchor, constant: Layout.clientIconTop),
            clientIcon.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: Layout.contentHorizontalInset),
            clientIcon.heightAnchor.constraint(equalToConstant: Layout.clientIconSize),
            clientIcon.widthAnchor.constraint(equalToConstant: Layout.clientIconSize),
            clientNameLabel.bottomAnchor.constraint(equalTo: clientIcon.bottomAnchor),
            clientNameLabel.leadingAnchor.constraint(equalTo: clientIcon.trailingAnchor, constant: Layout.clientNameSpacing),
            clientNameLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -Layout.contentHorizontalInset)
        ])
    }

    // MARK: - Configure

    /// - Parameter availableWidth: ширина таблицы; из неё считается высота одиночной
    ///   картинки — раньше для этого форсился layoutIfNeeded на каждый reuse.
    func configure(
        with viewModel: PSDAnnouncementCellModel,
        availableWidth: CGFloat,
        delegate: AnnouncementsAttachmentsDelegate?
    ) {
        let announcement = viewModel.announcement

        messageLabel.attributedText = viewModel.attributedText
        timeLabel.text = announcement.date.announcementTime()
        bubbleView.backgroundColor = announcement.isRead ? .bubbleViewColor : .newBubbleViewColor
        messageLabel.textColor = .label
        clientIcon.image = viewModel.client?.image
        clientNameLabel.text = viewModel.client?.clientName

        let hasText = (viewModel.attributedText?.length ?? 0) > 0
        zeroMessageLabelHeight?.isActive = !hasText

        // В сетку — всё медийное (фото и видео с media == true),
        // в список файлов — только немедийные вложения.
        let imageAttachments = announcement.attachments
            .filter { $0.media }
            .map { AnnouncementCellAttachmentModel(attachment: $0, isRead: announcement.isRead) }

        let fileAttachments = announcement.attachments
            .filter { !$0.media }
            .map { AnnouncementCellAttachmentModel(attachment: $0, isRead: announcement.isRead) }

        configureFilesTableTop(hasImages: !imageAttachments.isEmpty, hasFiles: !fileAttachments.isEmpty)
        configureImagesGrid(imageAttachments, announcement: announcement, availableWidth: availableWidth, delegate: delegate)
        configureFilesTable(fileAttachments, announcement: announcement, hasText: hasText, delegate: delegate)
    }

    private func configureFilesTableTop(hasImages: Bool, hasFiles: Bool) {
        switch (hasImages, hasFiles) {
        case (true, true):
            filesTableTop?.constant = Layout.filesTopWithImages
        case (false, true):
            filesTableTop?.constant = Layout.filesTopWithoutImages
        case (true, false):
            filesTableTop?.constant = Layout.filesTopImagesOnly
        case (false, false):
            filesTableTop?.constant = Layout.filesTopDefault
        }
    }

    private func configureImagesGrid(
        _ imageAttachments: [AnnouncementCellAttachmentModel],
        announcement: PSDAnnouncement,
        availableWidth: CGFloat,
        delegate: AnnouncementsAttachmentsDelegate?
    ) {
        guard !imageAttachments.isEmpty else {
            imagesGridHeight?.constant = 0
            imagesGridCollectionView.update(images: [])
            imagesGridCollectionView.onSelectItem = nil
            return
        }

        let gridWidth = availableWidth - Layout.bubbleHorizontalInset * 2 - Layout.gridInset * 2
        imagesGridHeight?.constant = AnnouncementsImageGridMetrics.totalHeight(
            forCount: imageAttachments.count,
            singleImageAttachment: imageAttachments.first?.attachment,
            gridWidth: gridWidth
        )
        imagesGridCollectionView.update(images: imageAttachments)

        imagesGridCollectionView.onSelectItem = { [weak self, weak delegate] model in
            guard let self else { return }
            let index = announcement.attachments.firstIndex(of: model.attachment) ?? 0
            delegate?.selectAttachment(cell: self, index: index)
        }
    }

    private func configureFilesTable(
        _ fileAttachments: [AnnouncementCellAttachmentModel],
        announcement: PSDAnnouncement,
        hasText: Bool,
        delegate: AnnouncementsAttachmentsDelegate?
    ) {
        guard !fileAttachments.isEmpty else {
            attachmentsTableView.update(items: [])
            attachmentsTableView.onSelectItem = nil
            filesTableHeight?.constant = 0
            return
        }

        attachmentsTableView.update(items: fileAttachments)
        attachmentsTableView.onSelectItem = { [weak self, weak delegate] model in
            guard let self else { return }
            let index = announcement.attachments.firstIndex(of: model.attachment) ?? 0
            delegate?.selectAttachment(cell: self, index: index)
        }

        let rowsHeight = CGFloat(fileAttachments.count) * AttachmentsTableView.rowHeight
        filesTableHeight?.constant = hasText ? rowsHeight + Layout.filesBottomSpacingWithText : rowsHeight
    }

    // MARK: - Reuse

    override func prepareForReuse() {
        super.prepareForReuse()
        // Контент (тексты, грид, файлы, высоты) намеренно не сбрасываем:
        // configure(with:) перезаписывает всё перед показом, а сброс грида здесь
        // приводил к лишним reloadData/перестройке layout на каждый reuse.
        // Обнуляем только замыкания, чтобы не удерживать старого делегата.
        imagesGridCollectionView.onSelectItem = nil
        attachmentsTableView.onSelectItem = nil
        attachmentsTableView.onTapIcon = nil
    }
}

// MARK: - Цвета

private extension UIColor {
    static let bubbleViewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#2C2C2F") ?? .black
        default:
            return UIColor(hex: "#F4F5F7") ?? .systemGray
        }
    }

    static let newBubbleViewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#29293D") ?? .black
        default:
            return UIColor(hex: "#EEF3FD") ?? .systemGray
        }
    }
}
