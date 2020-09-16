
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
        resetImage()
        self.setBackgroundColor(color: .psdLightGray, forState: .highlighted)
        self.layer.cornerRadius = BUTTONS_CORNER_RADIUS
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        resetImage()
    }
    private func resetImage(){
        let addImage : UIImage = UIImage.PSDImage(name: "Add").imageWith(color: UIColor.darkAppColor)!
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
}
