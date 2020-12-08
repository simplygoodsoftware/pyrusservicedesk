import Foundation
class PSDTableView : UITableView{
    let headerHeight :CGFloat = 40
    ///Is table view is loading. Hides or show activity.
    var isLoading : Bool = false{
        didSet(oldValue){
            if oldValue != isLoading{
                changeInset()
            }
            if isLoading{
                activity.startAnimating()
            }
            else{
                activity.stopAnimating()
            }
        }
    }
    ///Comparing rows and sections to remove with rows and section to add
    ///returs removeIndexPaths, addIndexPaths, reloadIndexPaths, removeSections, addSections, reloadSections
    static func compareAddAndRemoveRows(removeIndexPaths: [IndexPath], addIndexPaths: [IndexPath], removeSections: IndexSet, addSections: IndexSet) -> ([IndexPath], [IndexPath], [IndexPath], IndexSet, IndexSet, IndexSet){
        var newRemoveIndexPaths = removeIndexPaths
        var newAddIndexPaths = addIndexPaths
        var reloadIndexPaths = [IndexPath]()
        
        var newRemoveSections = removeSections
        var newAddsections = addSections
        var reloadSections = [Int]()
        
        if removeIndexPaths.count > 0{
            for removeIP in removeIndexPaths{
                for ip in addIndexPaths{
                    if removeIP.row == ip.row && removeIP.section == ip.section{
                        newRemoveIndexPaths.removeAll(where: {$0.row == ip.row && $0.section == ip.section})
                        newAddIndexPaths.removeAll(where: {$0.row == ip.row && $0.section == ip.section})
                        reloadIndexPaths.append(IndexPath(row: ip.row, section: ip.section))
                    }
                }
            }
        }
        if removeSections.count > 0{
            for removeSec in removeSections{
                for sec in addSections{
                    if removeSec == sec{
                        newRemoveSections.remove(removeSec)
                        newAddsections.remove(removeSec)
                        reloadSections.append(removeSec)
                    }
                }
            }
        }
        
        return (newRemoveIndexPaths, newAddIndexPaths, reloadIndexPaths, newRemoveSections, newAddsections, IndexSet(reloadSections))
    }
    private lazy var activity : UIActivityIndicatorView = {
        let activity = UIActivityIndicatorView()
        activity.style = UIActivityIndicatorView.Style.whiteLarge
        activity.hidesWhenStopped = true
        activity.color = CustomizationHelper.textColorForTable
        let transform = CATransform3DScale(CATransform3DIdentity, MAXIMUM_ACTIVITY_SCALE, MAXIMUM_ACTIVITY_SCALE, MAXIMUM_ACTIVITY_SCALE)
        activity.layer.transform = transform
        return activity
    }()
    private lazy var activityView: UIView = {
        let view = UIView()
        view.isUserInteractionEnabled = false
        view.backgroundColor = .clear
        return view
    }()
    override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: style)
    }
    private var topActivityConstraint : NSLayoutConstraint?
    ///add activity in table view - it will be automatic removed with tableview.
    func addActivityView(){
        //self.backgroundView = activityView
        self.superview?.addSubview(activityView)
        self.superview?.bringSubviewToFront(activityView)
        activityView.translatesAutoresizingMaskIntoConstraints = false
        activityView.addZeroConstraint([.left,.width,])
        self.superview?.addConstraint(NSLayoutConstraint(
            item: activityView,
            attribute: .top,
            relatedBy: .equal,
            toItem: self,
            attribute: .top,
            multiplier: 1,
            constant:0))
        self.superview?.addConstraint(NSLayoutConstraint(
            item: activityView,
            attribute: .height,
            relatedBy: .equal,
            toItem: self,
            attribute: .height,
            multiplier: 1,
            constant:0))
        addActivity()
    }
    override func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)
        if newSuperview == nil{
            self.activityView.removeFromSuperview()
            self.activity.removeFromSuperview()
        }
    }
    deinit {
        self.activityView.removeFromSuperview()
        self.activity.removeFromSuperview()
    }

    private func addActivity(){
        activityView.addSubview(activity)
        activity.sizeToFit()
        activity.translatesAutoresizingMaskIntoConstraints = false
        activity.addZeroConstraint([.centerX])
        activity.addSizeConstraint([.width,.height], constant: activity.frame.size.height)
        topActivityConstraint = NSLayoutConstraint(
            item: activity,
            attribute: .top,
            relatedBy: .equal,
            toItem: activity.superview,
            attribute: .top,
            multiplier: 1,
            constant:visibleAreaCenter())
        activityView.addConstraint(topActivityConstraint!)
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    private static let ACTIVITY_MOVE_DURATION : CGFloat = 0.25
    
    override var contentInset: UIEdgeInsets{
        didSet{
            if(!self.isDragging){
                changeConstraints(emptyTable: self.numberOfRows(inSection: 0) == 0){}
            }
        }
    }

    ///Add new row - if tableView is empty - perform animation of moving loader up - and return completion when animation ended, else return completion right now
    func addNewRow(completion: @escaping () -> Void){
        if(self.numberOfRows(inSection: 0) == 0){
            self.changeConstraints(emptyTable: false){
                completion()
            }
        }else{
            completion()
        }
    }
   
    private func calculateConstraint(emptyTable:Bool)->CGFloat{
        return emptyTable ?  self.visibleAreaCenter() : self.topActivityPosition()
    }

    private func changeConstraints(emptyTable:Bool, completion: @escaping () -> Void){
        if(self.isLoading){
            DispatchQueue.main.async {
                if(self.topActivityConstraint?.constant != self.calculateConstraint(emptyTable:emptyTable)){
                    
                    var oldFrame = self.activity.frame
                    oldFrame.origin.y = self.calculateConstraint(emptyTable:emptyTable)
                    let duration = emptyTable ? PSDTableView.ACTIVITY_MOVE_DURATION : 0.1
                    UIView.animate(withDuration: TimeInterval(duration), delay: 0, options: [ .beginFromCurrentState,.curveLinear], animations: {
                        self.activity.frame = oldFrame
                    }, completion:{ (comleted : Bool) in
                        if(comleted){
                            self.topActivityConstraint?.constant = self.calculateConstraint(emptyTable:emptyTable)
                        }
                        completion()
                    })
                }
                else{
                    completion()
                }
                
            }
        }
        else{
            completion()
        }
    }
    private func changeInset(){
        var oldInset : UIEdgeInsets  = self.contentInset
        oldInset.top = isLoading ?  REFRESH_CONTROL_HEIGHT : 0.0
        self.contentInset = oldInset
    }
    private static let MAX_LOADER_Y : CGFloat = 160
    private func visibleAreaCenter()->CGFloat{
        return min(PSDTableView.MAX_LOADER_Y,(self.bounds.size.height - self.contentInset.bottom)/2)
    }
    private func topActivityPosition()->CGFloat{
        return REFRESH_CONTROL_DISTANCE
    }
}
extension PSDTableView: Recolorable {
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            guard traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) else {
                return
            }
            recolor()
        }
    }
    @objc func recolor() {
        activity.color = CustomizationHelper.textColorForTable
    }
}
