extension PSDTableView{
    private static var _bottomPSDRefreshControl = PSDRefreshControl()
    var bottomPSDRefreshControl: PSDRefreshControl {
        get {
            return PSDTableView._bottomPSDRefreshControl
        }
        set (newValue) {
            PSDTableView._bottomPSDRefreshControl = newValue
        }
    }
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        PSDTableView._bottomPSDRefreshControl.insetHeight = 0.0
    }
    func visibleHeight()->CGFloat{
        return self.bounds.size.height - self.contentInset.top - self.contentInset.bottom
    }
    private func maxOffset()->CGFloat{
        return self.contentSize.height - visibleHeight()
    }
}
import UIKit
let MAXIMUM_ACTIVITY_SCALE : CGFloat = 0.7
let REFRESH_CONTROL_INSET_DURATOIN = 0.4
let REFRESH_CONTROL_HEIGHT : CGFloat = 33.0
let REFRESH_CONTROL_DISTANCE : CGFloat = 5.0
class PSDRefreshControl : UIControl{
    var insetHeight: CGFloat = 0.0
    private(set) var isRefreshing :  Bool = false{
        willSet(newValue){
            if newValue{
                activity.startAnimating()
                self.refreshingState = .refreshing
            }
            else{
                activity.stopAnimating()
                self.refreshingState = .notRefreshing
            }
        }
    }
    private enum RefreshingState{
        case notRefreshing
        case prepairing
        case refreshing
    }
    private var refreshingState :  RefreshingState = .notRefreshing
    
    private lazy var activity : UIActivityIndicatorView = {
        let activity = UIActivityIndicatorView()
        activity.style = UIActivityIndicatorView.Style.whiteLarge
        activity.hidesWhenStopped = false
        activity.color = tintColor
        return activity
    }()
    override var tintColor: UIColor! {
        didSet {
            activity.color = tintColor
        }
    }
    var position : horisontalPosition = .top
    enum horisontalPosition{
        case top
        case bottom
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.addSubview(activity)
        activity.translatesAutoresizingMaskIntoConstraints = false
        activity.addZeroConstraint([.centerX])
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if self.superview is UIScrollView{
            self.frame = CGRect(x: 0, y: 0, width: self.superview!.frame.size.width, height: REFRESH_CONTROL_HEIGHT)
            self.superview?.addObserver(self, forKeyPath: "contentOffset", options: [], context: nil)
            self.superview?.addObserver(self, forKeyPath: "pan.state", options: [], context: nil)
            
            switch(position){
            case .top:
                self.autoresizingMask = [.flexibleWidth,.flexibleBottomMargin]
            case .bottom:
                self.autoresizingMask = [.flexibleWidth,.flexibleTopMargin]
            }
            defaultPosition()
            
            if position == .top{
                activity.addConstraint([.top], constant: REFRESH_CONTROL_DISTANCE)
            }
            else{
                activity.addConstraint([.bottom], constant: REFRESH_CONTROL_DISTANCE)
            }
            
        }
    }
    override func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)
        if newSuperview == nil{
            self.superview?.removeObserver(self, forKeyPath: "contentOffset")
            self.superview?.removeObserver(self, forKeyPath: "pan.state")
        }
        else{
            self.keepOnPlase()
        }
    }

    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if (object as? UIView) == self.superview && (keyPath == "contentOffset" || keyPath == "pan.state"){
            if(keyPath == "contentOffset"){
                self.keepOnPlase()
                self.updateRefreshingState()
            }
            if(keyPath == "pan.state"){
                self.keepOnPlase()
                if canBeginRefreshing(){
                    self.updateRefreshingState()
                }
            }
            
            
        }
        else{
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
        }
    }
    private func forceRefresh() {
        guard let scroll = self.superview as? UIScrollView, !self.isRefreshing else{
            return
        }
        self.willChangeState(true)
        
        var inset = scroll.contentInset
        switch(position){
        case .top:
            inset.top = inset.top + REFRESH_CONTROL_HEIGHT
        case .bottom:
            inset.bottom = inset.bottom + REFRESH_CONTROL_HEIGHT
        }
        let duration = REFRESH_CONTROL_INSET_DURATOIN
        UIView.animate(withDuration: duration, animations: {
            scroll.contentInset = inset
        }, completion: { finished in
            self.insetHeight = REFRESH_CONTROL_HEIGHT
            self.changeState(true)
            self.activityScale(scale:MAXIMUM_ACTIVITY_SCALE)
            })
    }
    ///Set frame to refresh.
    ///- parameter automatic: Is need automatic calculation of y position. If true refresh is show according to superview contentOffset, else it show on its visible position (0 from top or bottom).
    private func keepOnPlase(){
        if !(self.superview is UIScrollView){
            return
        }
        var defaultPosition : CGFloat
        let scroll = (self.superview as! UIScrollView)
        var offset = scroll.contentOffset.y
        switch(position){
        case .top:
            offset = scroll.contentInset.top > 0 ? offset + scroll.contentInset.top : offset
            defaultPosition = min(-REFRESH_CONTROL_HEIGHT - offset,-REFRESH_CONTROL_HEIGHT)
            defaultPosition = max(-REFRESH_CONTROL_HEIGHT,defaultPosition)
            
        case .bottom:
            defaultPosition = scroll.contentSize.height
        }
        self.frame = CGRect(x: 0.0, y: defaultPosition, width: self.frame.size.width, height: REFRESH_CONTROL_HEIGHT)
        
        
    }
    private func updateRefreshingState(){
        if !(self.superview is UIScrollView) || !self.isEnabled{
            return
        }
        let scroll = (self.superview as! UIScrollView)
        if (self.superview is UITableView){
            let table = (self.superview as! UITableView)
            if table.numberOfSections == 0{
                return
            }
            
        }
        let offset = scroll.contentOffset.y - scroll.contentInset.top
        var minValue : CGFloat
        var maxValue : CGFloat
        switch(position){
        case .top:
            minValue = 0
            maxValue = -(REFRESH_CONTROL_HEIGHT * 2)
        case .bottom:
            minValue = scroll.contentSize.height
            maxValue  = scroll.contentSize.height + (REFRESH_CONTROL_HEIGHT * 2)
        }
        
        if(refreshingState != .refreshing){
            
            switch(position){
            case .top:
                if offset < -REFRESH_CONTROL_HEIGHT && refreshingState == .notRefreshing  && canBeginRefreshing(){
                    beginRefreshing()
                }
                else if offset<0{
                    activityScale(scale: calculateProgress(between:abs(maxValue),minValue,abs(offset)))
                }
            case .bottom:
                if(scroll.contentSize.height >= scroll.bounds.size.height){
                    let visibleHeight  = scroll.frame.size.height  - scroll.contentInset.top - scroll.contentInset.bottom 
                    if offset + visibleHeight > maxValue  && refreshingState == .notRefreshing && canBeginRefreshing(){
                        beginRefreshing()
                    }
                    if offset + visibleHeight > minValue+1{
                        activityScale(scale: calculateProgress(between:maxValue,minValue,offset + visibleHeight))
                    }
                }
            }
        }
        
    }
    private func canBeginRefreshing()->Bool{
        return ((self.superview as? UIScrollView)?.panGestureRecognizer.state.rawValue) == 3
    }
    private func defaultPosition(){
        insetHeight = 0.0
        activityScale(scale:0.0)
    }
    private func calculateProgress(between maxValue:CGFloat, _ minValue:CGFloat, _ currentValue: CGFloat)->CGFloat{
        let allWay = maxValue-minValue
        return min(MAXIMUM_ACTIVITY_SCALE,(currentValue-minValue)/allWay)
    }
    private func activityScale(scale:CGFloat){
        if scale > MAXIMUM_ACTIVITY_SCALE*0.8{
            self.activity.startAnimating()
        }
        else{
            self.activity.stopAnimating()
        }
        let transform = CATransform3DScale(CATransform3DIdentity, scale, scale, scale)
        self.activity.layer.transform = transform
    }
    ///Begin refreshing - animated set content inset and pass actions for .valueChanged
    private func beginRefreshing() {
        guard canBeginRefreshing() else {
            return
        }
        forceRefresh()
    }
    
    func endRefreshing(){
        if !self.isRefreshing || !(self.superview is UIScrollView){
            return
        }
        
        let scroll = self.superview as! UIScrollView
        var inset = scroll.contentInset
        if(position == .top){
            inset.top = max(inset.top - self.frame.size.height,0)
        }
        else{
            inset.bottom = inset.bottom - self.frame.size.height
        }
        let duration = REFRESH_CONTROL_INSET_DURATOIN
        UIView.animate(withDuration: duration, animations: {
            scroll.contentInset = inset
            self.defaultPosition()
        }, completion: { finished in
            
            self.willChangeState(false)
            self.changeState(false)
            self.activityScale(scale: 0)
        })
    }
    private func resetRefreshingState(){
        self.willChangeState(false)
        self.changeState(false)
    }
    private func willChangeState(_ newState:Bool){
        self.refreshingState = .prepairing
    }
    private func changeState(_ newState:Bool){
        self.isRefreshing = newState
        self.sendActions(for: .valueChanged)
    }
}
