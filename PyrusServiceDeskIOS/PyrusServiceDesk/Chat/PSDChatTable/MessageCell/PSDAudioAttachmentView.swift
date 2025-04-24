import UIKit

class PSDAudioAttachmentView: PSDAttachmentView {
    private enum Constants {
        static let stateLabelAlpha: CGFloat = 0.6
        static let uploadString = "Uploading".localizedPSD()
        static var sizeString: String = " "
        static let stateLabelHeight: CGFloat = 22
        static let nameLabelHeight: CGFloat = 19
        static let distance: CGFloat = 10.0
    }
    
    let height: CGFloat = PSDAttachmentView.uploadSize + (Constants.distance * 2)
    
    override var color: UIColor {
        didSet {
            nameLabel.textColor = color
            stateLabel.textColor = color
            previewBorderLayer.strokeColor = color.withAlphaComponent(0.15).cgColor
        }
    }
    
    override var downloadState: messageState {
        didSet {
            let currentState = downloadState  // Сохраняем состояние перед асинхронным вызовом
            super.downloadState = downloadState
            checkUploadView()
            if oldValue != downloadState {
                DispatchQueue.main.async { [weak self] in
                    self?.updateStateText(with: currentState)
                }
            }
        }
    }
    
    override var previewImage: UIImage? {
        didSet {
            checkUploadView()
            if previewImage != nil {
                showPreviewImage()
            }
        }
    }
    
    private lazy var nameLabel: UILabel = {
        let label = UILabel()
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = LABEL_MINIMUM_SCALE_FACTOR
        label.lineBreakMode = .byTruncatingMiddle
        label.frame = CGRect(
            x: (Constants.distance * 2) + PSDAttachmentView.uploadSize,
            y: height - Constants.stateLabelHeight - Constants.distance - Constants.nameLabelHeight,
            width: 10,
            height: Constants.nameLabelHeight
        )
        label.isHidden = true
        return label
    }()
    
    private lazy var stateLabel: UILabel = {
        let label = UILabel()
        label.alpha = Constants.stateLabelAlpha
        label.adjustsFontSizeToFitWidth = true
        label.frame = CGRect(
            x: (Constants.distance * 2) + PSDAttachmentView.uploadSize,
            y: height - Constants.stateLabelHeight - Constants.distance,
            width: 0,
            height: Constants.stateLabelHeight
        )
        return label
    }()
    
    private lazy var previewBorderLayer: CAShapeLayer = {
        let borderLayer = CAShapeLayer()
        borderLayer.fillColor = UIColor.clear.cgColor
        borderLayer.strokeColor = UIColor.psdLabel.withAlphaComponent(0.15).cgColor
        borderLayer.lineWidth = PREVIEW_BORDER_WIDTH
        return borderLayer
    }()
    
    private lazy var slider = AudioCellSlider()
    
    private var stateWidthConstraint: NSLayoutConstraint?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
        setupConstraints()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func draw(_ attachment: PSDAttachment, state: messageState) {
        super.draw(attachment, state: state)
        previewImageView.isHidden = true
        Constants.sizeString = attachment.dataSize()
        updateStateText(with: state)
        
        stateLabel.font = .stateFont
        nameLabel.font = .nameFont
        nameLabel.text = attachment.name
        
        var rect = nameLabel.frame
        rect.size.width = nameLabelWidth()
        nameLabel.frame = rect
        updateLabelsConstraints()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        let rectShape = CAShapeLayer()
        rectShape.path = UIBezierPath(
            roundedRect: previewImageView.bounds,
            byRoundingCorners: [.bottomLeft, .topLeft],
            cornerRadii: CGSize(width: MESSAGE_CORNER_RADIUS, height: MESSAGE_CORNER_RADIUS)
        ).cgPath
        
        previewImageView.layer.mask = rectShape
        previewBorderLayer.path = rectShape.path
        previewBorderLayer.frame = previewImageView.bounds
    }
    
    private func setupViews() {
        addSubview(nameLabel)
        addSubview(stateLabel)
        addSubview(slider)
        insertSubview(previewImageView, at: 0)
        frame = CGRect(x: 0, y: 0, width: 0, height: height)
        previewImageView.layer.addSublayer(previewBorderLayer)
    }
    
    private func setupConstraints() {
        uploadView.translatesAutoresizingMaskIntoConstraints = false
        stateLabel.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        previewImageView.translatesAutoresizingMaskIntoConstraints = false
        slider.translatesAutoresizingMaskIntoConstraints = false
        
        let heightConstraint = heightAnchor.constraint(equalToConstant: height)
        heightConstraint.priority = .init(999)
        
        NSLayoutConstraint.activate([
            uploadView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Constants.distance),
            uploadView.centerYAnchor.constraint(equalTo: centerYAnchor),
            
            heightConstraint,
            
            stateLabel.leadingAnchor.constraint(equalTo: uploadView.trailingAnchor, constant: Constants.distance),
            stateLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Constants.distance),
            stateLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Constants.distance),
            stateLabel.heightAnchor.constraint(equalToConstant: Constants.stateLabelHeight),
            stateLabel.widthAnchor.constraint(equalToConstant: getMaxWidth()),
            
//            nameLabel.leadingAnchor.constraint(equalTo: stateLabel.leadingAnchor),
//            nameLabel.trailingAnchor.constraint(equalTo: stateLabel.trailingAnchor),
//            nameLabel.bottomAnchor.constraint(equalTo: stateLabel.topAnchor),
//            nameLabel.heightAnchor.constraint(equalToConstant: Constants.nameLabelHeight),
            
            slider.leadingAnchor.constraint(equalTo: stateLabel.leadingAnchor),
//            slider.trailingAnchor.constraint(equalTo: stateLabel.trailingAnchor),
//            slider.widthAnchor.constraint(equalToConstant: 233),
            slider.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Constants.distance),
            slider.bottomAnchor.constraint(equalTo: stateLabel.topAnchor),
            slider.heightAnchor.constraint(equalToConstant: Constants.nameLabelHeight),
            
            previewImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: -1.0),
            previewImageView.topAnchor.constraint(equalTo: topAnchor),
            previewImageView.bottomAnchor.constraint(equalTo: bottomAnchor),
            previewImageView.centerYAnchor.constraint(equalTo: centerYAnchor),
            previewImageView.widthAnchor.constraint(equalToConstant: PSDAttachmentView.uploadSize + Constants.distance + 2.0)
        ])
        
        stateWidthConstraint = stateLabel.widthAnchor.constraint(equalToConstant: getMaxWidth())
//        stateWidthConstraint?.priority = .init(999)
        stateWidthConstraint?.isActive = true
        widthAnchor.constraint(greaterThanOrEqualToConstant: 307).isActive = true
        previewImageView.heightConstraint?.isActive = false
    }
    
    private func updateStateText(with newState: messageState) {
        stateLabel.text = newState == .sending ? Constants.uploadString : attachment?.dataSize()
    }
    
    private func getMaxWidth() -> CGFloat {
        return min(
            maxWidth - (Constants.distance * 2) - PSDAttachmentView.uploadSize,
            max(maxStateLabelWidth(), nameLabelWidth())
        )
    }
    
    private func nameLabelWidth() -> CGFloat {
        return 233//nameLabel.text?.size(withAttributes: [.font: UIFont.nameFont]).width ?? 0
    }
    
    private func maxStateLabelWidth() -> CGFloat {
        let downloadWidth = Constants.uploadString.size(withAttributes: [.font: UIFont.stateFont]).width
        let attachmentSizeWidth = Constants.sizeString.size(withAttributes: [.font: UIFont.stateFont]).width
        return max(downloadWidth, attachmentSizeWidth)
    }
    
    private func updateLabelsConstraints() {
        stateWidthConstraint?.constant = getMaxWidth()
    }
    
    private func checkUploadView() {
        uploadView.isHidden = (downloadState == .sent && previewImage != nil)
       // UIImage.PSDImage(name: "playIcon")
    }
    
    private func showPreviewImage() {
        previewImageView.isHidden = false
    }
}

private extension UIFont {
    static let nameFont = CustomizationHelper.systemFont(ofSize: 16.0)
    static let stateFont = CustomizationHelper.systemFont(ofSize: 14.0)
}
