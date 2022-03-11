import Foundation
extension PSDTableView   {
    func addKeyboardListeners(){
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardWillHide(_:)), name: UIResponder.keyboardWillHideNotification, object: nil)
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
    private func animationOptions(_ notification: NSNotification)->AnimationOptions{
        let curve : UInt = keyboardAnimationCurve(notification)
        return UIView.AnimationOptions(rawValue: curve << 16)
    }
    private func keyboardAnimationCurve(_ notification: NSNotification) -> UInt{
        if let animationCurveInt = (notification.userInfo?[UIResponder.keyboardAnimationCurveUserInfoKey] as? NSNumber)?.uintValue{
            return animationCurveInt
        }
        return 0
    }
    
    @objc private func keyboardWillHide(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue{
            let keyboardEndFrame = infoEndKey.cgRectValue
            
            animateInset(keyboardEndFrame.height, notification: notification)
        }
    }
    @objc private func keyboardWillShow(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            let keyboardEndFrame = infoEndKey.cgRectValue
            animateInset(keyboardEndFrame.height, notification: notification)
        }
    }
    private func animateInset(_ keyboardHeight:CGFloat, notification: NSNotification){
       
        let duration = keyboardAnimationDuration(notification)
        let animationOption = animationOptions(notification)
    
        
        let safeArea : CGFloat =  self.bottomPSDRefreshControl.insetHeight
        let bottomUnvisibleHeight = keyboardHeight + safeArea
        
        var offsetDelta : CGFloat = 0
        
        if(needChangeOffset( keyboardHeight: bottomUnvisibleHeight )){
            offsetDelta = bottomUnvisibleHeight - self.contentInset.bottom
            offsetDelta = min(offsetDelta,  self.contentSize.height - (self.frame.size.height-bottomUnvisibleHeight))
            offsetDelta = offsetDelta - self.bottomPSDRefreshControl.insetHeight
        }
       
        UIView.performWithoutAnimation {
            self.contentInset.bottom = bottomUnvisibleHeight
            self.scrollIndicatorInsets.bottom = bottomUnvisibleHeight
        }
        DispatchQueue.main.async {
            UIView.animate(withDuration: duration, delay: 0, options: [animationOption ,.beginFromCurrentState], animations: {
                
                self.changeContentOffset(changeOffset: offsetDelta)
                
            }, completion:{ (comletion : Bool) in
            })
        }
    }
    private func changeContentOffset(changeOffset:CGFloat){
        if changeOffset>0{
            self.changeOffset(delta: changeOffset)
        }
    }
    func changeOffset(delta:CGFloat){
        var newOffsetY = max(0,self.contentOffset.y + delta)//block  too little offset
        newOffsetY = min(self.contentSize.height - (self.frame.size.height - self.contentInset.bottom),newOffsetY)//block  too big offset
        self.contentOffset = CGPoint(x:0, y:newOffsetY)
    }
    private func needChangeOffset(keyboardHeight:CGFloat)->Bool{
        if(self.contentSize.height > (self.frame.size.height-keyboardHeight) && !self.isDragging){
            
            return true
        }
        return false
    
    }
   
}

