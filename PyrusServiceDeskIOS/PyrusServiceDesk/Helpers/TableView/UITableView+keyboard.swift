import Foundation
extension PSDTableView {
    func addKeyboardListeners(){
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillShowNotification, object: nil)
    }
    func removeListeners(){
        NotificationCenter.default.removeObserver(self)
    }
    
    private func keyboardAnimationDuration(_ notification: NSNotification) -> TimeInterval{
        if let  duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSValue{
            return duration as? TimeInterval ?? 0
        }
        return 0
    }
    
    @objc private func keyboardWillShow(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            let keyboardEndFrame = infoEndKey.cgRectValue
         //   animateInset(keyboardEndFrame.height, notification: notification)
        }
    }
    private func animateInset(_ keyboardHeight:CGFloat, notification: NSNotification){
        let duration = keyboardAnimationDuration(notification)
        let safeArea =  self.bottomPSDRefreshControl.insetHeight
        let bottomUnvisibleHeight = keyboardHeight + safeArea
        var offsetDelta : CGFloat = 0
        guard needChangeInset(bottomUnvisibleHeight) else {
            return
        }
        let needChangeOffset = needChangeOffset(keyboardHeight: bottomUnvisibleHeight )
        if needChangeOffset {
            offsetDelta = bottomUnvisibleHeight - self.contentInset.bottom
            offsetDelta = min(offsetDelta,  self.contentSize.height - (self.frame.size.height-bottomUnvisibleHeight))
            offsetDelta = offsetDelta - self.bottomPSDRefreshControl.insetHeight
        }
        let newOffset = newOffset(delta: offsetDelta, bottomInset: bottomUnvisibleHeight)
        UIView.animate(withDuration: duration, delay: 0, animations: {
            self.contentInset.bottom = bottomUnvisibleHeight
            self.scrollIndicatorInsets.bottom = bottomUnvisibleHeight
            if needChangeOffset {
                self.contentOffset.y = newOffset
            }
        }, completion: nil)
    }
    
    private func needChangeInset(_ newInset: CGFloat) -> Bool {
        if
            let presentedViewController = findViewController()?.presentedViewController,
            presentedViewController.isBeingDismissed
        {
            return contentInset.bottom <= newInset
        }
        return true
    }
    
    private func newOffset(delta: CGFloat, bottomInset: CGFloat) -> CGFloat {
        var newOffsetY = max(0,self.contentOffset.y + delta)//block  too little offset
        newOffsetY = min(self.contentSize.height - (self.frame.size.height - bottomInset),newOffsetY)//block  too big offset
        return newOffsetY
    }
    
    private func needChangeOffset(keyboardHeight:CGFloat)->Bool{
        if(self.contentSize.height > (self.frame.size.height-keyboardHeight) && !self.isDragging){
            return true
        }
        return false
    
    }
}
