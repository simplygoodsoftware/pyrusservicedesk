import Foundation

@objc protocol VoiceRecordButtonDelegate: NSObjectProtocol {
    ///Tell delegate to show warning message from the button
    func showBuble(title: String)
    func hideRecordBuble(_ stopRecord: Bool)
    ///Tell delegate that user whant to start recording
    func voiceRecordStart()
    ///Tell to delegate that user is stopped record
    func voiceRecordStop()
}

///The button to for "hold-to-record" action
@objc class VoiceRecordButton: UIButton {
    @objc weak var delegate: VoiceRecordButtonDelegate?
    
    private let touchInterval = 0.20//seconds to hold button to start recording
    private var touchTimer: Timer?//timer to detect long press
    private var fingerDown: Bool = false
    private let gestureThreshold: CGFloat = 50.0
    private var isAutoHoldingRecording = false
    
    private static let shakeAnimationDuration: CGFloat = 0.07
    private static let shakeAnimationRepeats: Float = 4
    private static let shakeAnimationDeviation: CGFloat = 3
    
    private static let recordOnImage = UIImage.PSDImage(name: "bigMicrophone")
    var customRecordOffImage = VoiceRecordButton.recordOffImage{
        didSet{
            if !self.isRecording{
                self.setImage(customRecordOffImage, for: .normal)
                self.setImage(customRecordOffImage, for: .highlighted)
            }
        }
    }
    private static let recordOffImage = UIImage.PSDImage(name: "micro")
    
    ///The value to detect is recording is perfoms now
    private(set) var isRecording: Bool = false{
        didSet{
            if self.isRecording {
                let mainColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? .blue
                self.setImage(VoiceRecordButton.recordOnImage, for: .normal)
                self.setImage(VoiceRecordButton.recordOnImage, for: .highlighted)
            }else{
                self.setImage(customRecordOffImage, for: .normal)
                self.setImage(customRecordOffImage, for: .highlighted)
            }
        }
    }
    
    // Для перемещения кнопки
    private var initialTouchLocation: CGPoint = .zero
    private var initialButtonCenter: CGPoint = .zero
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
        tapG.cancelsTouchesInView = true
        self.addGestureRecognizer(tapG)
    }
    
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        parentView = self.superview
    }
    
    @objc private func longTap(_ recognizer: UILongPressGestureRecognizer) {
        switch recognizer.state {
        case .began:
            initialTouchLocation = recognizer.location(in: parentView)
            initialButtonCenter = self.center
            self.touchDown()
            
        case .changed:
            if isRecording {
                let currentLocation = recognizer.location(in: parentView)
                let deltaX = currentLocation.x - initialTouchLocation.x
                let deltaY = currentLocation.y - initialTouchLocation.y
                
                if !isAutoHoldingRecording {
                    let newCenter = CGPoint(
                        x: initialButtonCenter.x + deltaX,
                        y: initialButtonCenter.y + deltaY
                    )
                    self.center = newCenter
                }
                
                // Проверяем направление относительно оси y = -x
                if deltaY < deltaX { // Движение ВВЕРХ (выше оси y = -x)
                    let distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if distance >= gestureThreshold {
                        print("➡️ Движение вверх на 20+ пикселей")
                        isAutoHoldingRecording = true
                        UIView.animate(withDuration: 0.3) {
                            self.center = self.initialButtonCenter
                        }
                    }
                } else { // Движение ВЛЕВО (ниже оси y = -x)
                    let distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if distance >= gestureThreshold {
                        print("⬅️ Движение влево на 20+ пикселей")
                        // Здесь можно вызвать нужный метод, например:
                        // delegate?.didDragLeft()
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
    
    @objc private func appClosed() {
        self.touchUp()
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
        self.delegate?.voiceRecordStop()
    }
    
    func fingerIsDown() -> Bool {
        return fingerDown
    }
}
