import Foundation

@objc protocol VoiceRecordButtonDelegate: NSObjectProtocol {
    ///Tell delegate to show warning message from the button
    func showBuble(title: String)
    func hideRecordBuble(_ stopRecord: Bool)
    ///Tell delegate that user whant to start recording
    func voiceRecordStart()
    ///Tell to delegate that user is stopped record
    func voiceRecordStop(needShowAudioView: Bool)
    func lockRecord()
    func cancelRecord()
}

///The button to for "hold-to-record" action
@objc class VoiceRecordButton: UIButton {
    @objc weak var delegate: VoiceRecordButtonDelegate?
    
    private let touchInterval = 0.20//seconds to hold button to start recording
    private var touchTimer: Timer?//timer to detect long press
    private var fingerDown: Bool = false
    private let gestureThreshold: CGFloat = 50.0
    var isAutoHoldingRecording = false
    
    private static let shakeAnimationDuration: CGFloat = 0.07
    private static let shakeAnimationRepeats: Float = 4
    private static let shakeAnimationDeviation: CGFloat = 3
    
    private static let recordOnImage = UIImage.PSDImage(name: "bigMicrophone")
    var customRecordOffImage = VoiceRecordButton.recordOffImage {
        didSet{
            if !self.isRecording{
                self.setImage(customRecordOffImage, for: .normal)
                self.setImage(customRecordOffImage, for: .highlighted)
            }
        }
    }
    private static let recordOffImage = UIImage.PSDImage(name: "micro")
    
    private var lockHeightConstraint: NSLayoutConstraint?
    private lazy var lockView: UIButton = {
        let view = UIButton()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .lockBackgroundColor
        view.layer.cornerRadius = 22
        view.alpha = 1
        view.isUserInteractionEnabled = false
        return view
    }()
    
    private lazy var lockImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.PSDImage(name: "lockAudio"))
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        return imageView
    }()
    
    private lazy var stopImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.PSDImage(name: "arrows"))
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        return imageView
    }()
    
    ///The value to detect is recording is perfoms now
    private(set) var isRecording: Bool = false{
        didSet{
            if self.isRecording {
                let mainColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? .blue
                self.setImage(VoiceRecordButton.recordOnImage, for: .normal)
                lockView.alpha = 1
                self.setImage(VoiceRecordButton.recordOnImage, for: .highlighted)
                UIView.animate(withDuration: 0.2, animations: {
                    self.lockHeightConstraint?.constant = 88
                    self.lockImageView.alpha = 1
                    self.stopImageView.alpha = 1
                    self.layoutIfNeeded()
                    self.parentView?.layoutIfNeeded()
                })
                lockView.isUserInteractionEnabled = true
            } else {
                self.setImage(customRecordOffImage, for: .normal)
                self.setImage(customRecordOffImage, for: .highlighted)
                stopImageView.image = UIImage.PSDImage(name: "arrows")
                lockView.alpha = 0
                self.lockHeightConstraint?.constant = 0
                lockImageView.alpha = 0
                stopImageView.alpha = 0
                lockView.isUserInteractionEnabled = false
                isAutoHoldingRecording = false
            }
        }
    }
    
    // Для перемещения кнопки
    private var initialTouchLocation: CGPoint = .zero
    private var initialButtonCenter: CGPoint = .zero
    private var initialLockCenter: CGPoint = .zero
    private weak var parentView: UIView?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createButton()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        createButton()
    }
    
    private func createButton(){
        self.clipsToBounds = false
        self.isRecording = false
        NotificationCenter.default.addObserver(self, selector: #selector(appClosed), name: UIApplication.willResignActiveNotification, object: nil)
        
        let tapG = UILongPressGestureRecognizer(target: self, action: #selector(longTap(_:)))
        tapG.allowableMovement = CGFloat.greatestFiniteMagnitude
        tapG.minimumPressDuration = 0.0
       // tapG.cancelsTouchesInView = true
        self.addGestureRecognizer(tapG)
        
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(sendButtonTapped))
        self.addGestureRecognizer(tapGestureRecognizer)
        
//        setupLockView()
    }
    
    @objc func sendButtonTapped() {
                if isAutoHoldingRecording {
                    touchUp()
                    return
                }
    }
    
    private func setupLockView() {
        guard parentView != nil else { return }
        parentView?.addSubview(lockView)
        lockHeightConstraint = lockView.heightAnchor.constraint(equalToConstant: 0)
        lockHeightConstraint?.isActive = true
        
        lockView.addSubview(lockImageView)
        lockView.addSubview(stopImageView)
        
        NSLayoutConstraint.activate([
            lockView.centerXAnchor.constraint(equalTo: centerXAnchor),
            lockView.widthAnchor.constraint(equalToConstant: 44),
            lockView.bottomAnchor.constraint(equalTo: topAnchor, constant: -10),
            
            lockImageView.centerXAnchor.constraint(equalTo: lockView.centerXAnchor),
            stopImageView.centerXAnchor.constraint(equalTo: lockView.centerXAnchor),
            lockImageView.topAnchor.constraint(equalTo: lockView.topAnchor, constant: 10),
            stopImageView.bottomAnchor.constraint(equalTo: lockView.bottomAnchor, constant: -10),
        ])
        
        lockView.addTarget(self, action: #selector(touchDown), for: .touchUpInside)
    }
    
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        parentView = self.superview
        setupLockView()
    }
    
    @objc private func longTap(_ recognizer: UILongPressGestureRecognizer) {
        switch recognizer.state {
        case .began:
            initialTouchLocation = recognizer.location(in: parentView)
            initialButtonCenter = self.center
            initialLockCenter = lockView.center
            self.touchDown()
            
        case .changed:
            if isRecording {
                let currentLocation = recognizer.location(in: parentView)
                let deltaX = currentLocation.x - initialTouchLocation.x
                let deltaY = currentLocation.y - initialTouchLocation.y
                
                if !isAutoHoldingRecording {
                    let newCenter = CGPoint(
                        x: min(initialButtonCenter.x + deltaX, initialButtonCenter.x),
                        y: min(initialButtonCenter.y + deltaY, initialButtonCenter.y)
                    )
                    let newLockCenter = CGPoint(
                        x: initialLockCenter.x,
                        y: min(initialLockCenter.y + deltaY, initialLockCenter.y) - 59
                    )
                    self.center = newCenter
                    lockView.center = newLockCenter
                }
                
                // Проверяем направление относительно оси y = -x
                if deltaY < deltaX { // Движение ВВЕРХ (выше оси y = -x)
                    let distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if distance >= gestureThreshold {
                        isAutoHoldingRecording = true
                        lockImageView.alpha = 0
                        stopImageView.image = UIImage.PSDImage(name: "stopRecord")
                        lockView.isUserInteractionEnabled = true
                        UIView.animate(withDuration: 0.3) {
                            self.center = self.initialButtonCenter
                            self.lockHeightConstraint?.constant = 44
                            self.setImage(UIImage.PSDImage(name: "whiteSend"), for: .normal)
                            self.layoutIfNeeded()
                            self.parentView?.layoutIfNeeded()
                        }
                        delegate?.lockRecord()
                    }
                } else { // Движение ВЛЕВО (ниже оси y = -x)
                    let distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if distance >= 80 && !isAutoHoldingRecording {
                        cancelRecording()
                    }
                }
            }
            
            fingerDown = self.bounds.contains(recognizer.location(in: self))
            
        case .ended, .cancelled:
            if !isAutoHoldingRecording {
                self.touchUp()
            }
            
        default:
            break
        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        lockView.layer.shadowColor = UIColor.black.cgColor
        lockView.layer.shadowOffset = CGSize(width: 0, height: 4)
        lockView.layer.shadowRadius = 4
        lockView.layer.shadowOpacity = 0.2
        lockView.layer.masksToBounds = false
    }
    
    func cancelRecording() {
        UIApplication.shared.isIdleTimerDisabled = false
        fingerDown = false
        endTimer()
        self.isRecording = false
        delegate?.cancelRecord()
    }
    
    @objc private func appClosed() {
        guard isRecording else { return }
        UIApplication.shared.isIdleTimerDisabled = false
        fingerDown = false
        endTimer()
        self.delegate?.voiceRecordStop(needShowAudioView: true)
        self.isRecording = false
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    private func startTimer() {
        touchTimer = Timer(
            timeInterval: touchInterval,
            target: self,
            selector: #selector(startRecord),
            userInfo: nil,
            repeats: false
        )
        RunLoop.current.add(touchTimer!, forMode: RunLoop.Mode.common)
    }
    
    private func endTimer() {
        touchTimer?.invalidate()
        touchTimer = nil
    }
    
    @objc private func touchDown() {
        UIApplication.shared.isIdleTimerDisabled = true
        fingerDown = true
        startTimer()
    }
    
    @objc private func touchUp() {
        touchEnded()
    }
    
    func stopRecordButtonTapped() {
        alpha = 0
        UIApplication.shared.isIdleTimerDisabled = false
        fingerDown = false
        endTimer()
        self.delegate?.voiceRecordStop(needShowAudioView: true)
        self.isRecording = false
    }
    
    private func touchEnded() {
        UIApplication.shared.isIdleTimerDisabled = false
        fingerDown = false
        if self.isRecording {
            self.stopRecord()
        } else if touchTimer != nil {
            self.shake()
            self.delegate?.showBuble(title: "HoldToRecord".localizedPSD())
        }
        endTimer()
    }
    
    private func shake() {
        let positionKey = "position"
        let animation = CABasicAnimation(keyPath: positionKey)
        animation.duration = CFTimeInterval(VoiceRecordButton.shakeAnimationDuration)
        animation.repeatCount = VoiceRecordButton.shakeAnimationRepeats
        animation.autoreverses = true
        animation.fromValue = NSValue(cgPoint: CGPoint(x: self.center.x - VoiceRecordButton.shakeAnimationDeviation, y: self.center.y))
        animation.toValue = NSValue(cgPoint: CGPoint(x: self.center.x + VoiceRecordButton.shakeAnimationDeviation, y: self.center.y))
        
        self.layer.add(animation, forKey: positionKey)
    }
    
    @objc func needHideBuble() {
        self.delegate?.hideRecordBuble(false)
    }
    
    @objc private func startRecord() {
        if !fingerDown {
            return
        }
        
        if isAutoHoldingRecording {
            touchUp()
            return
        }
        
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.prepare()
        generator.impactOccurred()
        
        self.delegate?.hideRecordBuble(false)
        if !self.isRecording {
            self.isRecording = true
            self.delegate?.voiceRecordStart()
        }
    }
    
    private func stopRecord() {
        self.isRecording = false
        self.delegate?.voiceRecordStop(needShowAudioView: false)
    }
    
    func fingerIsDown() -> Bool {
        return fingerDown
    }
}

private extension UIColor {
    static let lockBackgroundColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#3D4043") ?? .black
        default:
            return .white
        }
    }
}
