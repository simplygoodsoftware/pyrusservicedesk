
import UIKit

///Protocol for updateting info
protocol PSDUpdateInfo{
    func startGettingInfo()
    func refreshChat(showFakeMessage: Int?)
}

class PSDChatViewController: UIViewController{
    
    var chatId: String = ""
    
    public func updateTitle(){
        designNavigation()
        self.messageInputView.setToDefault()
        self.tableView.isLoading = false
        self.tableView.chatId = self.chatId
        self.tableView.reloadChat()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        presentationController?.delegate = self
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
        let deadlineTime = DispatchTime.now() + 0.45
        DispatchQueue.main.asyncAfter(deadline: deadlineTime, execute: {
            self.messageInputView.inputTextView.becomeFirstResponder()
        })
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        resizeTable()
    }
    func resizeTable(){
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
        self.navigationController?.navigationBar.isHidden = false
        startGettingInfo()
        resizeTable()
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
        if let view = PSD_ChatTitleView(){
            self.navigationItem.titleView = view
            view.sizeToFit()
            navigationController?.navigationBar.layoutIfNeeded()
        } else {
            self.title = PSD_ChatTitle()
        }
        
        self.setItems()
    }
    ///Set navigation items
    private func setItems()
    {
        if !PyrusServiceDeskController.iPadView && !PyrusServiceDesk.oneChat{
            self.navigationItem.rightBarButtonItem = chatsItem
        }
        if let rightBarButtonItem = PSD_СustomRightBarButtonItem(){
            rightBarButtonItem.tintColor = PSD_CustomColor()
            navigationItem.rightBarButtonItem = rightBarButtonItem
        }
        if let leftBarButtonItem = PSD_СustomLeftBarButtonItem(){
            leftBarButtonItem.tintColor = PSD_CustomColor()
            navigationItem.leftBarButtonItem = leftBarButtonItem
        }else{
            let item = UIBarButtonItem.init(customView: leftButton)
            navigationItem.leftBarButtonItem = item
        }
    }

    private lazy var leftButton : UIButton = {
        let button = UIButton.init(type: .custom)
        button.setTitle("Back".localizedPSD(), for: .normal)
        button.setTitleColor(UIColor.darkAppColor, for: .normal)
        let backImage = UIImage.PSDImage(name: "Back").withRenderingMode(.alwaysTemplate)
        button.setImage(backImage, for: .normal)
        button.addTarget(self, action: #selector(closeButtonAction), for: .touchUpInside)
        button.sizeToFit()
        return button
    }()
  
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
    func startGettingInfo() {
        stopGettingInfo()
        timer = Timer.scheduledTimer(timeInterval: reloadInterval, target: self, selector: #selector(updateTable), userInfo:nil , repeats: true)
    }
    func refreshChat(showFakeMessage: Int?) {
        if !PSDChatTableView.isNewChat(self.tableView.chatId){
            self.tableView.forceRefresh(showFakeMessage: showFakeMessage)
        }
    }
}
extension PSDChatViewController: PSDChatTableViewDelegate {
    func needShowRate(_ showRate: Bool) {
        messageInputView.showRate = showRate
    }
}

extension PSDChatViewController: UIAdaptivePresentationControllerDelegate {
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
        self.messageInputView.inputTextView.resignFirstResponder()
        super.present(viewControllerToPresent, animated: flag, completion: completion)
    }
}
