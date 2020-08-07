
import UIKit

protocol PSDMessageSendButtonDelegate: class {
    func sendMessage()
}

class PSDMessageSendButton: UIButton {
    
    weak var delegate: PSDMessageSendButtonDelegate?
    private let titleHorizontalInsets : CGFloat = 4.0
    private let titleFontSize : CGFloat = 16.0
    private let titleDisabledAlpha : CGFloat = 0.3
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.setTitle( "Send".localizedPSD() , for:.normal)
        self.titleLabel?.font = UIFont.systemFont(ofSize: titleFontSize)
        setTitleColor(UIColor.darkAppColor, for: .normal)
        setTitleColor(UIColor.psdLabel.withAlphaComponent(titleDisabledAlpha), for: .disabled)
        isEnabled = false
        self.contentEdgeInsets = UIEdgeInsets(top: 0.0, left: titleHorizontalInsets, bottom: 0.0, right: titleHorizontalInsets)
        
        self.addTarget(self, action: #selector(sendPressed), for: .touchUpInside)
        self.setBackgroundColor(color: .psdLightGray, forState: .highlighted)
        self.layer.cornerRadius = BUTTONS_CORNER_RADIUS
        
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
