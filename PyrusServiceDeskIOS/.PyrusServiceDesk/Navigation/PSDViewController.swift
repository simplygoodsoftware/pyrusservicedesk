import Foundation
class PSDViewController: UIViewController {
    override var title: String? {
        didSet {
            recolor()
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
}
extension PSDViewController: Recolorable {
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard self.traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
            recolor()
        }
    }
    @objc func recolor() {
        guard PyrusServiceDesk.mainController?.customization?.chatTitleColor != nil || PyrusServiceDesk.mainController?.customization?.customFontName != nil  else {
            return
        }
        navigationItem.titleView = customNavigationTitle()
        view.sizeToFit()
        navigationController?.navigationBar.layoutIfNeeded()
    }
}
private extension UIFont {
    static let titleFont = CustomizationHelper.systemBoldFont(ofSize: 18)
}
