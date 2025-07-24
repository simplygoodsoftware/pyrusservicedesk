import Foundation
/**
 The view to show that record is on. Redraws by calling didChangeVolume(array). Values in array must be in range VoiceRecordView.minLinesHeight...VoiceRecordView.linesHeight. If values are not in that range they will be cut. The number of values that is needed is : VoiceRecordView.viewsCount
 - **Example**
 ````
 //Create view. No need in frame - VoiceRecordView will resize itself
 let view = VoiceRecordView.init(frame: CGRect.zero)
 //Set bottom distance from view to superview, default is 0.0
 view.bottomLayoutConstraintConstant = 0.0
 self.addSubview(view)
 ````
*/
class VoiceRecordView: UIImageView, CAAnimationDelegate{
    static let linesHeight: CGFloat = 45.0
    static let minLinesHeight: CGFloat = 1.0
    
    //The size  of VoiceRecordView, never change.
    static let size: CGSize = CGSize(width: 200, height: 50)
    
    private static let shadowOffset: CGSize = CGSize(width: 0, height: 1)
    private static let shadowRadius: CGFloat = 10.0
    private static let shadowOpacity: Float = 0.20
    
    ///Duration for animation in function of appearing: animateAppearance(from startPoint:CGPoint, completion: (()->Void)?)
    static let appearDuration: CGFloat = 0.14
    ///Duration for animation in function of disappearing: animateDismiss(to endPoint:CGPoint, completion: (()->Void)?)
    static let disappearDuration: CGFloat = 0.20
    
    private static let unvisibleAlpha: Float = 0.0
    private static let visibleAlpha: Float = 1.0
    private static let scaleX: CGFloat = 0.05//x scale factor for view while animateAppearance/animateDismiss animation is called
    private static let scaleY: CGFloat = 0.1//y scale factor for view while animateAppearance/animateDismiss animation is called
    
    private static let sticksAnimatiomDuration: CGFloat = 0.2
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setDesign()
    }
    ///The constant for constraint to bottom of superview
    var bottomLayoutConstraintConstant: CGFloat = 0.0{
        didSet{
            bottomLayoutConstraint?.constant = bottomLayoutConstraintConstant
        }
    }
    private var bottomLayoutConstraint: NSLayoutConstraint?
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if self.superview != nil{
            self.translatesAutoresizingMaskIntoConstraints = false
            self.superview?.addConstraint(NSLayoutConstraint.init(item: self, attribute: .centerX, relatedBy: .equal, toItem: self.superview, attribute: .centerX, multiplier: 1.0, constant: 0))
            self.superview?.addConstraint(NSLayoutConstraint.init(item: self, attribute: .width, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: VoiceRecordView.size.width))
            self.superview?.addConstraint(NSLayoutConstraint.init(item: self, attribute: .height, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: VoiceRecordView.size.height))
            bottomLayoutConstraint = NSLayoutConstraint.init(item: self.superview!, attribute: .bottom, relatedBy: .equal, toItem: self, attribute: .bottom, multiplier: 1.0, constant: bottomLayoutConstraintConstant)
            self.superview?.addConstraint(bottomLayoutConstraint!)
        }
        else{
            self.didChangeVolume([CGFloat]())
        }
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setDesign()
    }
    private func setDesign(){
        self.backgroundColor = UIColor.clear
        self.layer.backgroundColor = UIColor.appColor.cgColor
        self.layer.cornerRadius = VoiceRecordView.size.height / 2
        self.tintColor = .white
        clipsToBounds = true
        self.image = UIImage.PSDImage(name: "recordBackground")
        drawShadow()
        drawDefault()
    }
    ///Redraws voice wave with new heights' array
    ///- parameter heights: The array of heights. For good looking the minimum value must be 0.0 - if no voice and maximum is 1.0. The count of heights in the array can be any, VoiceRecordView will fill the array with 1.0 if it is too little, or cut it from the begin - if too big.
    @objc func didChangeVolume(_ array: [CGFloat]){
        heightsMap.removeAll()
        heightsMap = array
        DispatchQueue.main.async {
            self.redrawBy(self.heightsMap)
        }
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    private var heightsMap: [CGFloat] = []
    private func drawShadow(){
        self.layer.masksToBounds = false
        self.layer.shadowOffset = VoiceRecordView.shadowOffset
        self.layer.shadowRadius = VoiceRecordView.shadowRadius
        self.layer.shadowOpacity = VoiceRecordView.shadowOpacity
        self.layer.shadowPath = UIBezierPath(roundedRect: CGRect(x: 0, y: 0, width: VoiceRecordView.size.width, height: VoiceRecordView.size.height), cornerRadius: layer.cornerRadius).cgPath
    }

    private static let distanceToBoard: CGFloat = 25.0
    private static let lineWidth: CGFloat = 2.0
    private static let interLineDist: CGFloat = 1.0
    private func drawDefault(){
        for index in 0..<VoiceRecordView.viewsCount {
            let x = VoiceRecordView.firstX + ((VoiceRecordView.lineWidth + VoiceRecordView.interLineDist) * CGFloat(index))
            let view = UIView.init(frame: CGRect(x: x, y: VoiceRecordView.size.height / 2, width: VoiceRecordView.lineWidth, height: VoiceRecordView.minLinesHeight))
            view.tag = index + 1
            view.backgroundColor = .white
            view.center.y = VoiceRecordView.size.height / 2
            self.addSubview(view)
        }
        self.layoutIfNeeded()
    }
    private func redrawBy(_ heights: [CGFloat], animated: Bool = true){
        let heights = fillOrCutHeight(oldHeights: heights, to: VoiceRecordView.viewsCount)
        for index in 0..<VoiceRecordView.viewsCount {
            if let view = self.viewWithTag(index + 1){
                let duration: CGFloat = animated ? VoiceRecordView.sticksAnimatiomDuration : 0
                var frame = view.frame
                var h: CGFloat = heights[index]
                h = min(VoiceRecordView.linesHeight,max(VoiceRecordView.minLinesHeight, h))
                frame.size.height = h
                UIView.animate(withDuration: TimeInterval(duration), animations: {
                    view.frame = frame
                    view.center.y = VoiceRecordView.size.height / 2
                })
                
            }
        }
        self.layoutIfNeeded()
    }
    private func fillOrCutHeight(oldHeights: [CGFloat], to count: Int) -> [CGFloat]{
        var newHeights = oldHeights
        if oldHeights.count < count{
            let difr = count - oldHeights.count
            for _ in 0...difr {
                newHeights.insert(VoiceRecordView.minLinesHeight, at: 0)
            }
        }
        else{
            newHeights = Array(oldHeights.suffix(count))
        }
        return newHeights
    }
    ///The count of animated views
    static let viewsCount = VoiceRecordView.calculateNeedCount()
    private static let firstX = VoiceRecordView.calculateFirstX(with: VoiceRecordView.viewsCount)//X point for first animated view
    private static func calculateNeedCount() -> Int{
        let onePeaceSize = VoiceRecordView.lineWidth + VoiceRecordView.interLineDist
        let width = VoiceRecordView.size.width - (VoiceRecordView.distanceToBoard * 2)
        return Int(width / onePeaceSize)
    }
    private static func calculateFirstX(with count: Int) -> CGFloat{
        let onePeaceSize = VoiceRecordView.lineWidth + VoiceRecordView.interLineDist
        let width = (onePeaceSize * CGFloat(count)) - VoiceRecordView.interLineDist
        return (VoiceRecordView.size.width - width) / 2
    }
    ///Animation of the appearance of the VoiceRecordView. The view moves from the given point from the "startPoint" parameter to the point determined by the constraints, while increasing from a smaller to a larger one and appears in alpha.
    func animateAppearance(from startPoint: CGPoint, completion: (() -> Void)?){
        self.layoutIfNeeded()
        self.animateMove(from: startPoint, to: self.frame.origin, isAppear: true, completion: completion)
    }
    ///Animation of the dismiss of the VoiceRecordView. The view moves from the point determined by the constraints to "endPoint" from parameter, while increasing from a larger to smaller one and disappears in alpha.
    func animateDismiss(to endPoint: CGPoint, completion: (() -> Void)?){
        self.animateMove(from: self.frame.origin, to: endPoint, isAppear: false, completion: completion)
    }
    private func animateMove(from startPoint: CGPoint, to endPoint: CGPoint, isAppear: Bool, completion: (() -> Void)?){
        self.layer.removeAllAnimations()
        self.transform = .identity
        
        let startAlpha: Float = isAppear ? VoiceRecordView.unvisibleAlpha : VoiceRecordView.visibleAlpha
        let endAlpha: Float = isAppear ? VoiceRecordView.visibleAlpha : VoiceRecordView.unvisibleAlpha
        let duration = isAppear ? VoiceRecordView.appearDuration : VoiceRecordView.disappearDuration
        let option2: UIView.AnimationOptions = isAppear ? .curveEaseOut: .curveEaseIn
        let startTrans = CGAffineTransform.identity.translatedBy(x: startPoint.x - self.center.x, y: startPoint.y - self.frame.origin.y)
        let endTrans = CGAffineTransform.identity.translatedBy(x: endPoint.x - self.center.x, y: endPoint.y - self.frame.origin.y)
        let startScale: CGAffineTransform = isAppear ? startTrans.scaledBy(x: VoiceRecordView.scaleX, y: VoiceRecordView.scaleY) :  .identity
        let endScale: CGAffineTransform = isAppear ? .identity : endTrans.scaledBy(x: VoiceRecordView.scaleX, y: VoiceRecordView.scaleY)
        
        self.alpha = CGFloat(startAlpha)
        self.transform = startScale
        
        UIView.animate(withDuration: TimeInterval(duration), delay: 0.0, options: [.allowUserInteraction,option2,.beginFromCurrentState], animations: {
            self.alpha = CGFloat(endAlpha)
            self.transform = endScale
        }, completion: { complete in
            completion?()
            if complete {
                self.transform = .identity
            }
        })
    }
}
