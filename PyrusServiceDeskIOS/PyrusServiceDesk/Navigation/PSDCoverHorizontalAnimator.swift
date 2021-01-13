
import Foundation
let TRANSITION_DURATION = 0.3

class CoverHorizontalAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    
    ///The type of transition. Has value presenting and dismissing. If viewcontroller is presenting use presenting, if dismissing -  dismissing
    enum TransitionType {
        case presenting
        case dismissing
    }
    
    let transitionType: TransitionType
    
    init(transitionType: TransitionType) {
        self.transitionType = transitionType
        super.init()
    }
    
    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        return TRANSITION_DURATION
    }
    
    func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        
        let inView  = transitionContext.containerView
        
        var _toView  = transitionContext.view(forKey: .to)
        if(_toView == nil){
            _toView  = transitionContext.viewController(forKey: .to)?.view
        }
        
        var  _fromView = transitionContext.view(forKey: .from)
        if(_fromView == nil){
            _fromView  = transitionContext.viewController(forKey: .from)?.view
        }
        let viewForSnapshot = transitionType == .presenting ? _toView : _fromView
        
        guard let fromView = _fromView,
              let toView = _toView,
              let snapshot = viewForSnapshot?.snapshotView(afterScreenUpdates: true) else {
            transitionContext.completeTransition(true)
            return
        }
        var frame = inView.bounds
        switch transitionType {
        case .presenting:
            inView.addSubview(toView)
            inView.addSubview(snapshot)
            toView.isHidden = true
            
            frame.origin.x = frame.size.width
            snapshot.frame = frame
            UIView.animate(withDuration: transitionDuration(using: transitionContext), animations: {
                snapshot.frame = inView.bounds
            }, completion: { finished in
                if UIDevice.current.userInterfaceIdiom == .pad {
                    toView.frame = inView.frame//В iPad в сплит режиме неверная верска, так как toView всегда имеет размер экрана, еще не подстроенный под выделенный отрезок экрана
                }
                toView.isHidden = false
                snapshot.removeFromSuperview()
                transitionContext.completeTransition(true)
            })
        case .dismissing:
            inView.addSubview(snapshot)
            fromView.isHidden = true
            toView.frame = frame
            //Started with IOS13 "toView" is adding to UIDropShadowView, if add it to other superview - after calling completeTransition(Bool) will be black screan.
            if (toView.superview == nil){
                inView.insertSubview(toView, belowSubview: fromView)
            }
            UIView.animate(withDuration: transitionDuration(using: transitionContext), animations: {
                frame.origin.x = frame.size.width
                frame.origin.y = fromView.frame.origin.y//for .pageSheet y != 0, save it
                snapshot.frame = frame
            }, completion: { finished in
                fromView.isHidden = false
                snapshot.removeFromSuperview()
                transitionContext.completeTransition(true)
            })
        }
    }
    
    
    
}
