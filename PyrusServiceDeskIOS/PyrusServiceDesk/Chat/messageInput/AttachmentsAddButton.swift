
import UIKit
protocol AttachmentsAddButtonDelegate: class {
    func attachmentChoosed(_ data:Data, _ url:URL?)
    func addButtonPressed()
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
    private func resetImage() {
        var addImage: UIImage?
        if let color = PyrusServiceDesk.mainController?.customization?.addAttachmentButtonColor {
            addImage = UIImage.PSDImage(name: "clip")?.imageWith(color: color)
        } else {
            addImage = UIImage.PSDImage(name: "clip")
        }
        self.setImage(addImage, for: .normal)
    }
    
    @objc func buttonPressed()
    {
        delegate?.addButtonPressed()
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(500)) {
      //  DispatchQueue.main.async {

            AttachmentHandler.shared.showAttachmentActionSheet(self.findViewController()!, sourseView:self)
            AttachmentHandler.shared.attachmentPickedBlock = { (data,url) in
                DispatchQueue.main.async {
                    self.delegate?.attachmentChoosed(data,url)
                }
                
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
    }
}
