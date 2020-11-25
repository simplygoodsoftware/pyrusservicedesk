import Foundation
class PSDViewController: UIViewController {
    override var title: String? {
        didSet {
            guard PyrusServiceDesk.mainController?.customization?.chatTitleColor != nil || PyrusServiceDesk.mainController?.customization?.customFontName != nil  else {
                return
            }
            navigationItem.titleView = customNavigationTitle()
            view.sizeToFit()
            navigationController?.navigationBar.layoutIfNeeded()
        }
    }
    private func customNavigationTitle() -> UIView? {
        let label = UILabel()
        label.textColor = PyrusServiceDesk.mainController?.customization?.chatTitleColor ?? .psdLabel
        label.font = .titleFont
        label.text = title
        label.lineBreakMode = .byTruncatingTail
        return label
    }
}
private extension UIFont {
    static let titleFont = PSD_SystemBoldFont(ofSize: 18)
}
