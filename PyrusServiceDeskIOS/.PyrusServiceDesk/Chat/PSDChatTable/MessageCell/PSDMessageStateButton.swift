
import UIKit
protocol PSDRetryActionDelegate: class{
    ///Pass to delegate  that message was pressed so need to try send one more time, if it needs to
    func tryShowRetryAction()
}
class PSDMessageStateButton: UIButton {
    weak var delegate: PSDRetryActionDelegate?
    ///state of PSDMessageStateButton taken from PSDMessage. Default is .sent.
    var _messageState: messageState = .sent{
        didSet(oldValue)
        {
            guard oldValue != _messageState else {
                return
            }
            redraw()
        }
    }
    let stateSize :CGFloat = 20.0
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.frame=CGRect(x: 0, y: 0, width: stateSize, height: stateSize)
        self.addSubview(stateImage)
        self.addTarget(self, action: #selector(stateButtonPressed), for: .touchUpInside)
    }
    @objc private func stateButtonPressed() {
        self.delegate?.tryShowRetryAction()
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    /**
     Restart animation
     */
    func restart(){
        if self.stateImage.animationImages != nil{
            self.stateImage.startAnimating()
        }
    }
    /**
     Redraw button according to it messageState
    */
    private func redraw() {
        self.stateImage.animationImages=nil
        switch _messageState {
        case .sent:
            redrawSent()
        case .sending:
            redrawSending()
        case .cantSend:
            redrawCantSend()
        }
    }
    
    ///A image that displays the current status of the message. Clock or error.
    private lazy var stateImage: UIImageView = {
        let imV = UIImageView()
        imV.frame=CGRect(x: 0, y: 0, width: stateSize-2, height: stateSize-2)
        imV.center=CGPoint(x:stateSize/2,y:stateSize/2)
        return imV
    }()
    
    private func redrawSent() {
        self.isHidden = true
    }
    
    private func redrawSending() {
        self.isHidden = false
        resetGifClockImage()
    }
    
    private func resetGifClockImage() {
        guard _messageState == .sending else {
            return
        }
        var images = [UIImage]()
        let imagePrefix = PSDMessageStateButton.clockGifImageName()
        for i in 1...CLOCK_IMAGES_COUNT {
            let imageSuffix = String(format: "%02d", i)
            guard let image = UIImage.PSDImage(name: imagePrefix+imageSuffix) else {
                continue
            }
            images.append(image)
        }
        stateImage.animationImages = images
        stateImage.animationDuration = 5
        stateImage.animationRepeatCount = 0
        stateImage.startAnimating()
    }
    private static func clockGifImageName() -> String {
        guard CustomizationHelper.textColorForTable.isDarkColor else {
            return "clock-light-"
        }
        return "clock-dark-"
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            if previousTraitCollection != UITraitCollection.current{
                resetGifClockImage()
            }
        }
    }
    private func redrawCantSend() {
        let image = UIImage.PSDImage(name: "icn_error")
        self.isHidden = false
        self.stateImage.image=image
    }
 }
private let CLOCK_IMAGES_COUNT = 55
