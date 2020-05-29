
import UIKit

///Change appear transaction animation to CoverHorizontal for UINavigationController
class PSDNavigationController: UINavigationController, UIViewControllerTransitioningDelegate{
    func animationController(forPresented presented: UIViewController,
                             presenting: UIViewController,
                             source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return CoverHorizontalAnimator.init(transitionType: .presenting)
    }
    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        
        return CoverHorizontalAnimator.init(transitionType: .dismissing)
    }

}
