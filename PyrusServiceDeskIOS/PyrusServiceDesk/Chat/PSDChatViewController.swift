
import UIKit

///Protocol for updateting info
protocol PSDUpdateInfo{
    func startGettingInfo()
    func refreshChat()
}

class PSDChatViewController: UIViewController{
    
    var chatId: String = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .never//.automatic
            
        }
        self.tableView.chatDelegate = self
        self.automaticallyAdjustsScrollViewInsets = false
        
        self.design()
        self.designNavigation()
        self.customiseDesign(color: UIColor.darkAppColor)
        
        self.openChat()
        
        self.startGettingInfo()
        
    }
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        resizeTable()
    }
    private func resizeTable(){
        var fr = self.view.bounds
        fr.origin.y = (self.navigationController?.navigationBar.frame.size.height ?? 0) +  UIApplication.shared.statusBarFrame.height
        fr.size.height =  fr.size.height - fr.origin.y 
        if #available(iOS 11.0, *) {
            fr.origin.x = self.view.safeAreaInsets.left
            fr.size.width = fr.size.width - (fr.origin.x*2)
        }
        self.tableView.frame = fr
        
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        self.tableView.removeListeners()
        self.tableView.bottomPSDRefreshControl.isEnabled = false
        if(self.tableView.visibleCells.last  != nil ){
            let lastVisibleRow : IndexPath? = self.tableView.indexPath(for: self.tableView.visibleCells.last!)
            if(lastVisibleRow != nil){
                coordinator.animateAlongsideTransition(in: self.tableView, animation: { (context) in
                    self.tableView.reloadData()
                    if(self.tableView.contentOffset.y > 100){
                        self.tableView.scrollToRow(at: lastVisibleRow!, at: .bottom, animated: false)
                    }
                }, completion: { context in
                    self.tableView.bottomPSDRefreshControl.isEnabled = true
                    self.tableView.addKeyboardListeners()
                })
            }
            
        }
    }
    ///Constraint to inputView width for iPad View.
    private var iPadInputConstraintWidth : NSLayoutConstraint?
    ///Constraint to inputView origin x for iPad View.
    private var iPadInputConstraintX : NSLayoutConstraint?
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if(PyrusServiceDeskController.iPadView){
            if(iPadInputConstraintWidth == nil)
            {
                iPadInputConstraintWidth = NSLayoutConstraint(
                    item: self.messageInputView.backgroundView!,
                    attribute: .width,
                    relatedBy: .equal,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1,
                    constant:self.view.frame.size.width)
                self.messageInputView.addConstraint(iPadInputConstraintWidth!)
            }
            else{
                iPadInputConstraintWidth?.constant = self.view.frame.size.width
            }
            if(iPadInputConstraintX == nil)
            {
                let originPointX = self.view.convert(self.view.frame.origin, to: nil).x
                iPadInputConstraintX = NSLayoutConstraint(
                    item: self.messageInputView.backgroundView!,
                    attribute: .leading,
                    relatedBy: .equal,
                    toItem: self.messageInputView,
                    attribute: .leading,
                    multiplier: 1,
                    constant:originPointX  )
                self.messageInputView.addConstraint(iPadInputConstraintX!)
            }
            else{
                let originPointX = self.view.convert(self.view.frame.origin, to: nil).x
                iPadInputConstraintX?.constant = originPointX
            }
            
        }
        
        
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        startGettingInfo()
      //  self.becomeFirstResponder()
        resizeTable()
     //   self.tableView.addKeyboardListeners()
        
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        self.tableView.addKeyboardListeners()
        UIView.performWithoutAnimation {
             self.becomeFirstResponder()
        }
    }
    @objc private func appEnteredForeground(){
        self.tableView.addKeyboardListeners()
    }
    @objc private func appEnteredBackground(){
        self.tableView.removeListeners()
    }
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
        EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "hideAllKeyboard() called in viewWillDisappear")
        hideAllKeyboard()
        self.tableView.removeListeners()
        stopGettingInfo()
    }
    ///hide keyboard with inputAccessoryView
    private func hideAllKeyboard(){
        self.resignFirstResponder()
        self.messageInputView.inputTextView.resignFirstResponder()
    }
    ///Opens chat - full reload existing data in tableView according to chat id
    func openChat(){
        self.messageInputView.setToDefault()
        self.tableView.isLoading = true
        self.tableView.chatId = self.chatId
        self.tableView.reloadChat()
        
    }
    lazy private var messageInputView : PSDMessageInputView = {
        let inputView = PSDMessageInputView.init(frame: CGRect(x: 0, y: self.view.frame.size.height-70, width: self.view.frame.size.width, height: 50))
        inputView.delegate = self
        return inputView
    }()
    lazy var tableView: PSDChatTableView = {
        let table = PSDChatTableView(frame: self.view.bounds)
        table.setupTableView()
        return table
    }()
    /**Setting design To PyrusSupportChatViewController view, add subviews*/
    private func design() {
        self.view.backgroundColor = .psdBackground
        self.view.addSubview(self.tableView)
        self.tableView.addActivityView()
    }
    
    //MARK : Navigation
    //Setting design to navigation bar, title and buttons
    private func designNavigation()
    {
        self.title = PSD_ChatTitle()
        self.setItems()
    }
    ///Set chats item if it's not iPadView or oneChat mode.
    private func setItems()
    {
        resetImage()
        if !PyrusServiceDeskController.iPadView && !PyrusServiceDesk.oneChat{
            self.navigationItem.rightBarButtonItem = chatsItem
        }
    }
    private lazy var leftButton : UIButton = {
        let button = UIButton.init(type: .custom)
        button.setTitle("Back".localizedPSD(), for: .normal)
        button.setTitleColor(UIColor.darkAppColor, for: .normal)
        button.setImage(UIImage.PSDImage(name: "Back").imageWith(color: UIColor.darkAppColor), for: .normal)
        button.addTarget(self, action: #selector(closeButtonAction), for: .touchUpInside)
        button.sizeToFit()
        return button
    }()
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        resetImage()
    }
    private func resetImage(){
        leftButton.setImage(UIImage.PSDImage(name: "Back").imageWith(color: UIColor.darkAppColor), for: .normal)
        let item = UIBarButtonItem.init(customView: leftButton)
        navigationItem.leftBarButtonItem = item
    }
  
    lazy private var chatsItem: ChatListBarButtonItem = {
        let chatsListItem = ChatListBarButtonItem.init()
        chatsListItem.target = self
        chatsListItem.action = #selector(openChatsList)
        return chatsListItem
    }()
    private func customiseDesign(color:UIColor)
    {
        self.navigationItem.leftBarButtonItem?.tintColor = color
        self.navigationItem.rightBarButtonItem?.tintColor = color
    }
    @objc private func openChatsList(){
        self.navigationController?.popViewController(animated: true)
    }
    @objc private func closeButtonAction(){
        if(PyrusServiceDesk.mainController != nil){
            PyrusServiceDesk.mainController?.remove()//with quick opening - closing can be nil
        }
        else{
            if let navigationController = self.navigationController as? PyrusServiceDeskController{
                navigationController.remove()
            }
        }
        EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "hideAllKeyboard() called after press on back button")
        UIView.performWithoutAnimation {
            hideAllKeyboard()
        }
    }
    //MARK: KeyBoard hiding and moving
    override var canBecomeFirstResponder: Bool
    {
        if self.tableView.window != nil && !hasNoConnection() {
            return true;
        }
        return false;
    }
    private func hasNoConnection()->Bool{
        if self.view.subviews.contains(self.tableView.noConnectionView){
            return true
        }
        return false
    }
    override var inputAccessoryView: UIView?
    {
        return messageInputView;
    }
  
    //MARK: get user info automatic
    private let reloadInterval = 30.0 //seconds
    private var timer: Timer?
    private func stopGettingInfo() {
        if((timer) != nil){
            timer!.invalidate()
            timer=nil
        }
    }
    @objc private func updateTable(){
        startGettingInfo()
        if !PSDChatTableView.isNewChat(self.tableView.chatId) && !hasNoConnection() && !PSDGetChat.isActive(){
           self.tableView.updateChat(needProgress:false)
        }
        
    }
}
extension PSDChatViewController : PSDMessageInputViewDelegate{
    func send(_ message:String,_ attachments:[PSDAttachment]){
        let newMessage :PSDMessage = PSDObjectsCreator.createMessage(message, attachments: attachments)
        tableView.addNewRow(message: newMessage)
        PSDMessageSend.pass(newMessage, to: self.tableView.chatId, delegate:self.tableView)
    }
    func sendRate(_ rateValue: Int) {
        let newMessage = PSDObjectsCreator.createMessage(rating: rateValue)
        tableView.addNewRow(message: newMessage)
        PSDMessageSend.pass(newMessage, to: self.tableView.chatId, delegate:self.tableView)
    }
}
extension PSDChatViewController : PSDUpdateInfo{
    public static let PSD_LAST_ACTIVITY_INTEVAL_MINUTE = TimeInterval(90)
    public static let PSD_LAST_ACTIVITY_INTEVAL_5_MINUTES = TimeInterval(300)
    public static let PSD_LAST_ACTIVITY_INTEVAL_HOUR = TimeInterval(3600)
    public static let PSD_LAST_ACTIVITY_INTEVAL_3_DAYS = TimeInterval(3*24*60*60)
    public static let REFRESH_TIME_INTEVAL_5_SECONDS = TimeInterval(5)
    public static let REFRESH_TIME_INTEVAL_15_SECONDS = TimeInterval(15)
    public static let REFRESH_TIME_INTEVAL_1_MINUTE = TimeInterval(60)
    public static let REFRESH_TIME_INTEVAL_3_MINUTES = TimeInterval(180)
    private static let PSD_LAST_ACTIVITY_KEY = "PSDLastActivityDate"

    static func userLastActivityKey() -> String{
        return PSD_LAST_ACTIVITY_KEY + "_" + PyrusServiceDesk.userId
    }
    
    private static func getTimerInerval() -> TimeInterval{
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(), let date = pyrusUserDefaults.object(forKey: PSDChatViewController.userLastActivityKey()) as? Date{
            let difference = Date().timeIntervalSince(date)
            if difference <= PSD_LAST_ACTIVITY_INTEVAL_MINUTE{
                return REFRESH_TIME_INTEVAL_5_SECONDS
            } else if difference <= PSD_LAST_ACTIVITY_INTEVAL_5_MINUTES{
                return REFRESH_TIME_INTEVAL_15_SECONDS
            }
        }
        return REFRESH_TIME_INTEVAL_1_MINUTE
    }

    
    func startGettingInfo() {
        stopGettingInfo()
        timer = Timer.scheduledTimer(timeInterval: PSDChatViewController.getTimerInerval(), target: self, selector: #selector(updateTable), userInfo:nil , repeats: false)
    }
    func refreshChat() {
        if !PSDChatTableView.isNewChat(self.tableView.chatId){
            self.tableView.forceBottomRefresh()
        }
    }
}
extension PSDChatViewController: PSDChatTableViewDelegate {
    func needShowRate(_ showRate: Bool) {
        messageInputView.showRate = showRate
    }
}
