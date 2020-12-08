import Foundation
var DETAIL_FONT = CustomizationHelper.systemFont(ofSize: 12.0)
var OFFSET_FOR_DETAIL :CGFloat = ("Time_Format".localizedPSD()+" ").size(withAttributes: [NSAttributedString.Key.font:DETAIL_FONT]).width+TO_BOARD_DISTANCE
let TO_BOARD_DISTANCE : CGFloat = 7.0
/**
TableView that can be scroll to left part to show some detail at right. Use OFFSET_FOR_DETAIL to control details' width.
 */
class PSDDetailTableView : PSDTableView,UIGestureRecognizerDelegate{
    
    private lazy var panGesture :  UIPanGestureRecognizer = {
        let gesture = UIPanGestureRecognizer.init(target: self, action: #selector(handleSwipeGesture))
        gesture.minimumNumberOfTouches=1
        gesture.maximumNumberOfTouches=1
        gesture.cancelsTouchesInView = false
        gesture.delegate = self
        
        return gesture
    }()
    override init(frame: CGRect, style: UITableView.Style) {
        var fr = frame
        fr.size.width =  fr.size.width + OFFSET_FOR_DETAIL
        super.init(frame: fr, style: style)
        self.addGestureRecognizer(panGesture)
    }
  
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        return true
    }
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return false
    }
    override func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        if(gestureRecognizer  == panGesture)
        {
            let panGesture = gestureRecognizer as! UIPanGestureRecognizer
            let velocity = panGesture.velocity(in: self)
            if(abs(velocity.y) < abs(velocity.x) && velocity.x<0){//move left
                return true
            }
            return false
        }
        return super.gestureRecognizerShouldBegin(gestureRecognizer)
    }
    let velocitySmooth : CGFloat = 5
    let moveLeftAnimationDuration = 0.2
    let moveBackAnimationDuration = 0.1
    @objc private func handleSwipeGesture(sender:UIPanGestureRecognizer){
        if(sender.state == .changed)
        {
            let velocity = sender.velocity(in: self)
            var x : CGFloat = self.contentOffset.x - (velocity.x/velocitySmooth)
            x = max(0, x) //limited to not move to right side
            x = min(OFFSET_FOR_DETAIL, x) //limited not to left move more then OFFSET_FOR_DETAIL
            UIView .animate(withDuration: moveLeftAnimationDuration, animations:{
                self.contentOffset = CGPoint(x: x, y: self.contentOffset.y)
            })
           
        }
        if(sender.state == .ended)
        {
            UIView .animate(withDuration: moveBackAnimationDuration, animations:{
                self.contentOffset = CGPoint(x: 0, y: self.contentOffset.y)
            })
        }
       
    }
}
