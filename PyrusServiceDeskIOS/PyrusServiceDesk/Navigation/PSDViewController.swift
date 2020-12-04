import Foundation
class PSDViewController: UIViewController {
    override var title: String? {
        didSet {
            designNavigation()
        }
    }
    private func customNavigationTitle() -> UIView? {
        let label = UILabel()
        var textColor = PyrusServiceDesk.mainController?.customization?.chatTitleColor ??  .psdLabel
        if let color = PyrusServiceDesk.mainController?.customization?.customBarColor {
            textColor = UIColor.getTextColor(for: color)
        }
        label.textColor = textColor
        label.font = .titleFont
        label.text = title
        label.lineBreakMode = .byTruncatingTail
        return label
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard self.traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
            designNavigation()
        }
    }
    private func designNavigation() {
        guard PyrusServiceDesk.mainController?.customization?.chatTitleColor != nil || PyrusServiceDesk.mainController?.customization?.customFontName != nil  else {
            return
        }
        navigationItem.titleView = customNavigationTitle()
        view.sizeToFit()
        navigationController?.navigationBar.layoutIfNeeded()
    }
}
private extension UIFont {
    static let titleFont = PSD_SystemBoldFont(ofSize: 18)
}
