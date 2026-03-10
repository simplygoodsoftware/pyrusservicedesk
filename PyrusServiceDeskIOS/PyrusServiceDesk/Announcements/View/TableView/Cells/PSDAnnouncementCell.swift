import UIKit

protocol AnnouncementsAttachmentsDelegate: AnyObject {
    func selectAttachment(cell: UITableViewCell, index: Int)
}

final class PSDAnnouncementCell: UITableViewCell {
    static let identifier = "PSDAnnouncementCell"
    
    private let bubbleView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 16
        view.layer.masksToBounds = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private var topMessageLabelView: NSLayoutConstraint?
    private var zeroMessageLabelHeight: NSLayoutConstraint?
    private let messageLabel: UITextView = {
        let textView = UITextView()
        textView.font = .systemFont(ofSize: 15)
        textView.textColor = .label
        textView.isEditable = false
        textView.isScrollEnabled = false
        textView.backgroundColor = .clear
        textView.dataDetectorTypes = [.link, .phoneNumber]
        textView.textContainerInset = .zero
        textView.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor
        textView.translatesAutoresizingMaskIntoConstraints = false
        return textView
    }()
    
    private var timeLabelTop: NSLayoutConstraint?
    private lazy var timeLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 13)
        label.adjustsFontForContentSizeCategory = true
        label.textColor = .secondaryLabel
        label.setContentCompressionResistancePriority(.required, for: .horizontal)
        label.setContentHuggingPriority(.required, for: .horizontal)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private var imageAttachmentViewHeight: NSLayoutConstraint?
    private lazy var imagesGridCollectionView: ImagesGridCollectionView = {
       let collectionView = ImagesGridCollectionView()
        collectionView.backgroundColor = .clear
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.layer.cornerRadius = 16
        collectionView.layer.maskedCorners = [
            .layerMinXMinYCorner,
            .layerMaxXMinYCorner
        ]
        collectionView.isScrollEnabled = false
        return collectionView
    }()
    
    private var topAttachmentsTableView: NSLayoutConstraint?
    private var tableViewHeight: NSLayoutConstraint?
    private let attachmentsTableView = AttachmentsTableView()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .psdDarkBackgroundColor
        contentView.backgroundColor = .psdDarkBackgroundColor

        contentView.addSubview(bubbleView)
        
        bubbleView.addSubview(imagesGridCollectionView)
        imageAttachmentViewHeight = imagesGridCollectionView.heightAnchor.constraint(equalToConstant: 0)
        imageAttachmentViewHeight?.isActive = true
        
        bubbleView.addSubview(attachmentsTableView)
        attachmentsTableView.translatesAutoresizingMaskIntoConstraints = false
        tableViewHeight = attachmentsTableView.heightAnchor.constraint(equalToConstant: 0)
        tableViewHeight?.isActive = true
        topAttachmentsTableView = attachmentsTableView.topAnchor.constraint(equalTo: imagesGridCollectionView.bottomAnchor, constant: 8)
        topAttachmentsTableView?.isActive = true

        
        bubbleView.addSubview(messageLabel)
        zeroMessageLabelHeight = messageLabel.heightAnchor.constraint(equalToConstant: 0)
        
        bubbleView.addSubview(timeLabel)

        NSLayoutConstraint.activate([
            imagesGridCollectionView.topAnchor.constraint(equalTo: bubbleView.topAnchor, constant: 2),
            imagesGridCollectionView.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: 2),
            imagesGridCollectionView.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -2),
            
            attachmentsTableView.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor),
            attachmentsTableView.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor),
            
            bubbleView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            bubbleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 8),
            bubbleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -8),
            
            messageLabel.topAnchor.constraint(equalTo: attachmentsTableView.bottomAnchor, constant: 4),
            messageLabel.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: 10),
            messageLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -10),
            
            timeLabel.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 2),
            timeLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -10),
            bubbleView.bottomAnchor.constraint(equalTo: timeLabel.bottomAnchor, constant: 10),
            
            contentView.bottomAnchor.constraint(equalTo: bubbleView.bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func configure(with vm: PSDAnnouncement, delegate: AnnouncementsAttachmentsDelegate?) {
        contentView.setNeedsLayout()
        contentView.layoutIfNeeded()
        messageLabel.text = vm.text
        
        timeLabel.text = vm.date.announcementTime()

        bubbleView.backgroundColor = vm.isRead ? .bubbleViewColor : .newBubbleViewColor
        messageLabel.textColor = .label
        
        let hasText = vm.text?.count ?? 0 > 0
        zeroMessageLabelHeight?.isActive = !hasText
        
        let imageAttachments = vm.attachments
            .filter({ $0.media && !$0.isVideo })
            .map({ AnnouncementCellAttachmentModel(attachment: $0, isRead: vm.isRead) })
        let hasImages = imageAttachments.count > 0
        
        let fileAttachments = vm.attachments
            .filter({ !$0.media || $0.isVideo })
            .map({ AnnouncementCellAttachmentModel(attachment: $0, isRead: vm.isRead) })
        let hasFiles = fileAttachments.count > 0
        
        if hasFiles && hasImages {
            topAttachmentsTableView?.constant = 4
            
        } else if hasFiles {
            topAttachmentsTableView?.constant = 8
            
        } else if hasImages {
            topAttachmentsTableView?.constant = 0
        } else {
            topAttachmentsTableView?.constant = 4
        }
        
        if hasImages {
            var height: CGFloat = 0
            switch imageAttachments.count {
            case 1:
                let attach = imageAttachments.first?.attachment
                height = 2 + min(AnnouncementsHelper.scaledHeight(
                    originalWidth: attach?.width ?? 0,
                    originalHeight: attach?.height ?? 0,
                    maxWidth: imagesGridCollectionView.frame.width
                ), UIScreen.main.bounds.height * 0.6)
            case 2:
                height = 217
            case 3:
                height = 333
            case 4:
                height = 448
            case 5:
                height = 448
            case 6:
                height = 448
            case 7:
                height = 503
            case 8:
                height = 503
            default:
                height = 167 * 3
            }
            imageAttachmentViewHeight?.constant = height
            imagesGridCollectionView.update(images: imageAttachments)
            
            imagesGridCollectionView.onSelectItem = { attach in
                delegate?.selectAttachment(cell: self, index: vm.attachments.firstIndex(of: attach.attachment) ?? 0)
            }
            imagesGridCollectionView.attachDelegate = delegate
        } else {
            imageAttachmentViewHeight?.constant = 0
        }
            
        if hasFiles {
            attachmentsTableView.update(items: fileAttachments)
            attachmentsTableView.onSelectItem = { attach in
                delegate?.selectAttachment(cell: self, index: vm.attachments.firstIndex(of: attach.attachment) ?? 0)
            }
            
            if vm.text?.count ?? 0 == 0 {
                tableViewHeight?.constant = CGFloat(fileAttachments.count * 54)
            } else {
                tableViewHeight?.constant = CGFloat(fileAttachments.count * 54) + 6
            }
        } else {
            attachmentsTableView.update(items: [])
            tableViewHeight?.constant = 0
        }
        
        contentView.setNeedsLayout()
        contentView.layoutIfNeeded()
    }
    
    override func prepareForReuse() {
        tableViewHeight?.constant = 0
        imageAttachmentViewHeight?.constant = 0
        topAttachmentsTableView?.constant = 8
        timeLabelTop?.constant = 2
        messageLabel.text = nil
        attachmentsTableView.update(items: [])
        imagesGridCollectionView.update(images: [])
        bubbleView.backgroundColor = .bubbleViewColor
        contentView.setNeedsLayout()
        contentView.layoutIfNeeded()
    }
}

private extension UIColor {
    static let timeLabel = UIColor(hex: "#9199A1") ?? .systemGray
    static let secondColor = UIColor(hex: "#FFB049")
    
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
    
    static let imagePreviewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#3A3B3D") ?? .black
        default:
            return UIColor(hex: "#ECEDEF") ?? .systemGray
        }
    }
    
    static let newImagePreviewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#35354F") ?? .black
        default:
            return UIColor(hex: "#D1DFFA") ?? .systemGray
        }
    }
}
