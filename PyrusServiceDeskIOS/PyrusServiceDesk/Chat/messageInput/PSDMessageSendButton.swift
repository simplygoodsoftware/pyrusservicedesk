
import UIKit

protocol PSDMessageSendButtonDelegate: class {
    func sendMessage()
}

class PSDMessageSendButton: UIButton {
    
    weak var delegate: PSDMessageSendButtonDelegate?
    private let titleHorizontalInsets : CGFloat = 4.0
    static let titleDisabledAlpha: CGFloat = 0.3
    override init(frame: CGRect) {
        super.init(frame: frame)
        let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? .appColor
        setImage(UIImage.PSDImage(name: "sendFill")?.imageWith(color: color), for: .normal)
        setImage(UIImage.PSDImage(name: "send"), for: .disabled)
        self.titleLabel?.font = .title
        setTitleColor(color, for: .normal)
        alpha = 0
        self.contentEdgeInsets = UIEdgeInsets(top: 0.0, left: titleHorizontalInsets, bottom: 0.0, right: titleHorizontalInsets)
        
        self.addTarget(self, action: #selector(sendPressed), for: .touchUpInside)
        self.layer.cornerRadius = BUTTONS_CORNER_RADIUS
       // recolor()
        self.sizeToFit()
    }
    
    @objc func sendPressed() {
        alpha = 0
        //isHidden = true
        //isEnabled = false
        self.delegate?.sendMessage()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
private extension UIFont {
    static let title = CustomizationHelper.systemFont(ofSize: 16.0)
}
extension PSDMessageSendButton: Recolorable {
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard self.traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
            let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? .appColor
            setImage(UIImage.PSDImage(name: "sendFill")?.imageWith(color: color), for: .normal)
           // recolor()
        }
    }
    func recolor() {
        self.setBackgroundColor(color: CustomizationHelper.lightGrayInputColor, forState: .highlighted)
        setTitleColor(CustomizationHelper.textColorForInput.withAlphaComponent(PSDMessageSendButton.titleDisabledAlpha), for: .disabled)
    }
}
