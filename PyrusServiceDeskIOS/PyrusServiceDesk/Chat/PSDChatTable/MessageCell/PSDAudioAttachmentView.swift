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
    
    var presenter: AudioPlayerPresenter?
    
    override var color: UIColor {
        didSet {
            stateLabel.textColor = color
            previewBorderLayer.strokeColor = color.withAlphaComponent(0.15).cgColor
        }
    }
    
    var state: AudioState = .loading {
        didSet {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                
                switch state {
                case .loading:
                    playImageView.image = UIImage.PSDImage(name: "Close")?.imageWith(color: .white)
                    playView.layer.addLoadingAnimation()
                    slider.isUserInteractionEnabled = false
                    slider.setValue(0, animated: false)
                    setTime(string: "--:--")
                    //presenter?.changeTime(0)
                case .playing:
                    hasBeenTracked = false
                    playImageView.image = UIImage.PSDImage(name: "pause")?.imageWith(color: .white)
                    playView.layer.removeLoadingAnimation()
                    slider.isUserInteractionEnabled = true
                    //                    if oldValue == .stopped && slider.value == 1 {
                    //                        slider.setValue(0, animated: false)
                    //                    }
                case .paused:
                    guard oldValue != .loading && oldValue != .needLoad else { return }
                    if !hasBeenTracked {
                        playImageView.image = UIImage.PSDImage(name: "playIcon")?.imageWith(color: .white)
                        playView.layer.removeLoadingAnimation()
                        slider.isUserInteractionEnabled = true
                    }
                case .stopped:
                    playImageView.image = UIImage.PSDImage(name: "playIcon")?.imageWith(color: .white)
                    playView.layer.removeLoadingAnimation()
                    slider.isUserInteractionEnabled = true
                    // if oldValue == .loading {
                    presenter?.changeTime(0)
                    //  }
                    if !slider.isTracking {
                        slider.setValue(0, animated: false)
                        presenter?.drawCurrentProgress()
                    }
                case .needLoad:
                    playImageView.image = UIImage.PSDImage(name: "download")?.imageWith(color: .white)
                    playView.layer.removeLoadingAnimation()
                    slider.isUserInteractionEnabled = true
                }
            }
        }
    }
    
    var sliderColor: ColorType = .brightColor {
        didSet {
            switch sliderColor {
            case .brightColor:
                //slider.tintColor = .white
                slider.setThumbImage(UIImage.PSDImage(name: "circle"), for: .normal)
                slider.minimumTrackTintColor = .white
                slider.maximumTrackTintColor = UIColor(hex: "#E3E5E84D")?.withAlphaComponent(0.3)
            case .defaultColor:
                slider.setThumbImage(UIImage.PSDImage(name: "darkCircle"), for: .normal)
                slider.minimumTrackTintColor = .appColor
                slider.maximumTrackTintColor = .trackColor
            }
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
    
    private lazy var playImageView = UIImageView(image: UIImage.PSDImage(name: "Close")?.imageWith(color: .white))
    
    private lazy var playView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 25
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .previewBackgroundColor
        view.isHidden = true
        return view
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
    
    var slider: UISlider = AudioCellSlider()
    
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
       // presenter?.drawCurrentProgress()
        //   playView.layer.addLoadingAnimation()
    }
    
    private func setupViews() {
        addSubview(stateLabel)
        addSubview(slider)
        addSubview(playView)
        insertSubview(previewImageView, at: 0)
        frame = CGRect(x: 0, y: 0, width: 0, height: height)
        previewImageView.layer.addSublayer(previewBorderLayer)
    }
    
    private func setupConstraints() {
        uploadView.translatesAutoresizingMaskIntoConstraints = false
        stateLabel.translatesAutoresizingMaskIntoConstraints = false
        previewImageView.translatesAutoresizingMaskIntoConstraints = false
        slider.translatesAutoresizingMaskIntoConstraints = false
        
        let heightConstraint = heightAnchor.constraint(equalToConstant: height)
        heightConstraint.priority = .init(999)
        
        playImageView.translatesAutoresizingMaskIntoConstraints = false
        playView.addSubview(playImageView)
        
        NSLayoutConstraint.activate([
            uploadView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Constants.distance),
            uploadView.centerYAnchor.constraint(equalTo: centerYAnchor),
            playView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Constants.distance),
            playView.centerYAnchor.constraint(equalTo: centerYAnchor),
            playView.heightAnchor.constraint(equalToConstant: 50),
            playView.widthAnchor.constraint(equalToConstant: 50),
            playImageView.centerXAnchor.constraint(equalTo: playView.centerXAnchor),
            playImageView.centerYAnchor.constraint(equalTo: playView.centerYAnchor),
            playImageView.widthAnchor.constraint(equalToConstant: 30),
            playImageView.heightAnchor.constraint(equalToConstant: 30),
            
            heightConstraint,
            
            stateLabel.leadingAnchor.constraint(equalTo: uploadView.trailingAnchor, constant: Constants.distance),
            stateLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Constants.distance),
            stateLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Constants.distance),
            stateLabel.heightAnchor.constraint(equalToConstant: Constants.stateLabelHeight),
            stateLabel.widthAnchor.constraint(equalToConstant: getMaxWidth()),
            
            slider.leadingAnchor.constraint(equalTo: stateLabel.leadingAnchor),
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
        
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(buttonTapped))
        playView.addGestureRecognizer(tapGestureRecognizer)
        playView.isUserInteractionEnabled = true
        
        slider.addTarget(self, action: #selector(endDragging), for: .touchUpInside)
        slider.addTarget(self, action: #selector(endDragging), for: .touchUpOutside)
        slider.addTarget(self, action: #selector(startDragging), for: .touchDown)
    }
    
    private var hasBeenTracked: Bool = false
    
    @objc func endDragging() {
        if state == .playing {
            presenter?.startPlay(progress: slider.value)
        } else {
            presenter?.changeTime(CGFloat(slider.value))
        }
        
    }
    
    @objc func startDragging() {
        hasBeenTracked = true

        // presenter?.startPlay(progress: slider.value * Float(OpusPlayer.OpusAudioPlayerSampleRate))
        //  presenter?.pausePlay()
    }
    
    @objc func buttonTapped() {
        presenter?.buttonWasPressed()
    }
    
    private func updateStateText(with newState: messageState) {
        //stateLabel.text = newState == .sending ? Constants.uploadString : attachment?.dataSize()
    }
    
    private func getMaxWidth() -> CGFloat {
        return 233
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
        uploadView.isHidden = downloadState == .sent //&& previewImage != nil)
        playView.isHidden = !uploadView.isHidden
        // UIImage.PSDImage(name: "playIcon")
    }
    
    private func showPreviewImage() {
        previewImageView.isHidden = false
    }
}

extension PSDAudioAttachmentView: AudioPlayerViewProtocol {
    func changeState(_ state: AudioState) {
        self.state = state
    }
    
    func playingProgress(_ progress: CGFloat) {
        if !slider.isTracking && (state == .playing || state == .paused) && downloadState == .sent {// && (Float(progress) >= slider.value || progress == 0) {
            slider.setValue(Float(progress), animated: true)
            print(progress)
        }
    }
    
    func changeProgress(_ progress: CGFloat) {
        slider.value = Float(progress)
    }
    
    func setTime(string: String) {
        stateLabel.text = string
    }
    
    func changeLoadingProggress(_ progress: Float) {
        DispatchQueue.main.async { [weak self] in
            self?.playView.layer.updateLoadingProgress(progress)
        }
    }
}

private extension UIFont {
    static let nameFont = CustomizationHelper.systemFont(ofSize: 16.0)
    static let stateFont = CustomizationHelper.systemFont(ofSize: 14.0)
}

private extension UIColor {
    static let previewBackgroundColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#7182FD") ?? .white
        default:
            return UIColor(hex: "#4861F2") ?? .darkAppColor
        }
    }
    
    static let trackColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFF4D")?.withAlphaComponent(0.3) ?? .white
        default:
            return UIColor(hex: "#0000001A")?.withAlphaComponent(0.1) ?? .darkAppColor
        }
    }
}
