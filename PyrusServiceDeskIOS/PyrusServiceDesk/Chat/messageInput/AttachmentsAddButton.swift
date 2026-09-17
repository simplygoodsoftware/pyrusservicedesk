import UIKit
protocol AttachmentsAddButtonDelegate: class {
    func attachmentChoosed(_ data:Data, _ url:URL?)
    func addButtonPressed()
    ///Контроллер, от имени которого показывать меню вложений.
    ///Искать его по цепочке респондеров от кнопки нельзя: панель ввода — `inputAccessoryView`,
    ///она живёт в окне клавиатуры, и первым по цепочке найдётся приватный контроллер
    ///этого окна, а не экран чата. Презентация оттуда идёт без системной анимации.
    func attachmentMenuPresenter() -> UIViewController?
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
        if PSDLiquidGlassStyle.isEnabled {
            //На стекле иконка одного цвета с остальными стеклянными кнопками.
            //Template + динамический tintColor: при смене темы перекрашивается сама.
            self.setImage(UIImage.PSDImage(name: "lgpaperclip")?.withRenderingMode(.alwaysTemplate), for: .normal)
            self.tintColor = PSDLiquidGlassStyle.iconColor
            return
        }
        var addImage: UIImage?
        if let color = PyrusServiceDesk.mainController?.customization?.addAttachmentButtonColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor  {
            addImage = UIImage.PSDImage(name: "clip")?.imageWith(color: color)
        } else {
            addImage = UIImage.PSDImage(name: "clip")
        }
        self.setImage(addImage, for: .normal)
    }
    
    @objc func buttonPressed()
    {
        delegate?.addButtonPressed()
        //Показываем сразу: презентует экран чата из окна приложения, и UIKit сам
        //уводит клавиатуру вместе с панелью, как в системных мессенджерах.
        //Прежняя задержка нужна была только потому, что презентация шла из окна клавиатуры.
        guard let presenter = delegate?.attachmentMenuPresenter() else { return }
        AttachmentHandler.shared.showAttachmentActionSheet(presenter, sourseView: self)
        AttachmentHandler.shared.attachmentPickedBlock = { [weak self] (data, url) in
            DispatchQueue.main.async {
                self?.delegate?.attachmentChoosed(data, url)
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
