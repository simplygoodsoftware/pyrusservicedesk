
import UIKit
protocol PSDMessageStateButtonDelegate: class{
    ///Pass to delegate  that PSDMessageStateButton was pressed while messageState = .cantSend, so need to try send one more time
    func showRetryAction()
}
class PSDMessageStateButton: UIButton {
    weak var delegate: PSDMessageStateButtonDelegate?
    ///state of PSDMessageStateButton taken from PSDMessage. Default is .sent.
    var _messageState : messageState = .sent{
        didSet(oldValue)
        {
            if(oldValue != _messageState){
                redraw()
            }
        }
    }
    let stateSize :CGFloat = 20.0
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.frame=CGRect(x: 0, y: 0, width: stateSize, height: stateSize)
        self.addSubview(stateImage)
        self.addTarget(self, action: #selector(stateButtonPressed), for: .touchUpInside)
    }
    @objc func stateButtonPressed(){
        if _messageState == .cantSend{
            self.delegate?.showRetryAction()
        }
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
    private func redraw(){
        self.stateImage.animationImages=nil
        stateImageBack.removeFromSuperview()
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
    private lazy var stateImage:UIImageView = {
        let imV = UIImageView()
        imV.frame=CGRect(x: 0, y: 0, width: stateSize-2, height: stateSize-2)
        imV.center=CGPoint(x:stateSize/2,y:stateSize/2)
        return imV
    }()
    ///A back image view used only when draw .sending. Is back for clock.
    private lazy var stateImageBack:UIImageView = {
        let imV = UIImageView()
        imV.frame=self.bounds
        imV.image = UIImage.PSDImage(name: PSDMessageStateButton.clockBackImageName())
        return imV
    }()
    private func redrawSent(){
        self.isHidden = true
    }
    private func redrawSending()
    {
        self.isHidden = false
        
        resetBackClockImage()
        resetGifClockImage()
        self.addSubview(stateImageBack)
    }
    private func resetBackClockImage(){
        if _messageState == .sending{
            stateImageBack.image = UIImage.PSDImage(name: PSDMessageStateButton.clockBackImageName())
        }
    }
    private static func clockBackImageName()->String{
        if #available(iOS 13.0, *) {
            if UITraitCollection.current.userInterfaceStyle == .dark {
                return "clock-face-dark"
            }
        }
        return "clock-face"
    }
    private func resetGifClockImage(){
        if _messageState == .sending{
            let image = UIImage.PSDGifImage(name:PSDMessageStateButton.clockGifImageName())
            self.stateImage.animationImages = image
            self.stateImage.animationDuration = 10.0
            self.stateImage.animationRepeatCount = 0
            self.stateImage.startAnimating()
        }
    }
    private static func clockGifImageName()->String{
        if #available(iOS 13.0, *) {
            if UITraitCollection.current.userInterfaceStyle == .dark {
                return "clock-dark"
            }
        }
        return "clock"
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            if previousTraitCollection != UITraitCollection.current{
                resetBackClockImage()
                resetGifClockImage()
            }
        }
    }
    private func redrawCantSend()
    {
        let image = UIImage.PSDImage(name: "icn_error")
        self.isHidden = false
        self.stateImage.image=image
    }
 }
