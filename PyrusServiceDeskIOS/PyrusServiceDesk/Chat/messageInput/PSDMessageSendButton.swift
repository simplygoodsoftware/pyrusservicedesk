
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
        self.setTitle( "Send".localizedPSD() , for:.normal)
        self.titleLabel?.font = .title
        let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor ?? UIColor.darkAppColor
        setTitleColor(color, for: .normal)
        isEnabled = false
        self.contentEdgeInsets = UIEdgeInsets(top: 0.0, left: titleHorizontalInsets, bottom: 0.0, right: titleHorizontalInsets)
        
        self.addTarget(self, action: #selector(sendPressed), for: .touchUpInside)
        self.layer.cornerRadius = BUTTONS_CORNER_RADIUS
        recolor()
        self.sizeToFit()
    }
    @objc func sendPressed()
    {
        isEnabled = false
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
            recolor()
        }
    }
    func recolor() {
        self.setBackgroundColor(color: CustomizationHelper.lightGrayInputColor, forState: .highlighted)
        setTitleColor(CustomizationHelper.textColorForInput.withAlphaComponent(PSDMessageSendButton.titleDisabledAlpha), for: .disabled)
    }
}
