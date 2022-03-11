
import UIKit

///Change appear transaction animation to CoverHorizontal for UINavigationController
class PSDNavigationController: UINavigationController, UIViewControllerTransitioningDelegate{
    override init(rootViewController: UIViewController) {
        super.init(rootViewController: rootViewController)
        recolor()
    }
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        recolor()
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    func animationController(forPresented presented: UIViewController,
                             presenting: UIViewController,
                             source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return CoverHorizontalAnimator.init(transitionType: .presenting)
    }
    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        
        return CoverHorizontalAnimator.init(transitionType: .dismissing)
    }
    override var preferredStatusBarStyle: UIStatusBarStyle {
        guard let statusBarStyle = CustomizationHelper.statusBarStyle(for: self) else {
            return super.preferredStatusBarStyle
        }
        return statusBarStyle
    }
}
extension PSDNavigationController: Recolorable {
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
        if let barStyle = CustomizationHelper.barStyle {
            navigationBar.barStyle = barStyle
        }
        if let color = PyrusServiceDesk.mainController?.customization?.customBarColor {
            navigationBar.barTintColor = color
        }
    }
}
