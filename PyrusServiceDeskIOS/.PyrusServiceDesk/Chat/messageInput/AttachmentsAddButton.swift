
import UIKit
protocol AttachmentsAddButtonDelegate: class {
    func prepairToShowAlert()
    func showInput()
    func attachmentChoosed(_ data:Data, _ url:URL?)
}
class AttachmentsAddButton: UIButton {
    weak var delegate: AttachmentsAddButtonDelegate?
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.contentMode = .scaleAspectFit
        self.addTarget(self, action: #selector(buttonPressed), for: .touchUpInside)
        self.layer.cornerRadius = BUTTONS_CORNER_RADIUS
        recolor()
    }
    private func resetImage(){
        let color = PyrusServiceDesk.mainController?.customization?.addAttachmentButtonColor ?? UIColor.darkAppColor
        let addImage = UIImage.PSDImage(name: "Add")?.imageWith(color: color)
        self.setImage(addImage, for: .normal)
    }
    @objc func buttonPressed()
    {
        self.delegate?.prepairToShowAlert()
        AttachmentHandler.shared.showAttachmentActionSheet(self.findViewController()!, sourseView:self)
        AttachmentHandler.shared.attachmentPickedBlock = { (data,url) in
            DispatchQueue.main.async {
                self.delegate?.attachmentChoosed(data,url)
                self.delegate?.showInput()
            }
            
        }
        AttachmentHandler.shared.closeBlock = { (closed)  in
            DispatchQueue.main.async {
                self.delegate?.showInput()
            }
            
        }
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard self.traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
            recolor()
        }
    }
    private func recolor() {
        resetImage()
        self.setBackgroundColor(color: CustomizationHelper.lightGrayInputColor, forState: .highlighted)
    }
}
