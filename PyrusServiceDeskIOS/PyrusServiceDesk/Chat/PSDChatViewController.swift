
import UIKit

///Protocol for updateting info
protocol PSDUpdateInfo{
    func startGettingInfo()
    func refreshChat(showFakeMessage: Int?)
}

class PSDChatViewController: PSDViewController {
    private var firstLoad: Bool = true
    public func updateTitle(){
        designNavigation()
        self.messageInputView.setToDefault()
        self.tableView.isLoading = false
        self.tableView.reloadChat()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        presentationController?.delegate = self
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .never//.automatic
            navigationItem.largeTitleDisplayMode = .never
            
        }
        self.tableView.chatDelegate = self
        self.automaticallyAdjustsScrollViewInsets = false
        
        self.design()
        self.designNavigation()
        self.customiseDesign(color: PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor)
        
        self.openChat()
        self.startGettingInfo()
        if let infoView = PyrusServiceDesk.mainController?.customization?.infoView, !(PSDMessagesStorage.pyrusUserDefaults()?.bool(forKey: PSD_WAS_CLOSE_INFO_KEY) ?? true) {
            view.addSubview(infoView)
            infoView.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
            infoView.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
            infoView.topAnchor.constraint(equalTo: view.topAnchor, constant: (self.navigationController?.navigationBar.frame.size.height ?? 0) +  UIApplication.shared.statusBarFrame.height).isActive = true
        }
        
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        resizeTable()
    }

    override func recolor() {
        super.recolor()
        recolorTextInput(messageInputView)
    }
    func resizeTable(){
        var fr = self.view.bounds
        fr.origin.y = (self.navigationController?.navigationBar.frame.size.height ?? 0) +  UIApplication.shared.statusBarFrame.height
        fr.size.height =  fr.size.height - fr.origin.y 
        if #available(iOS 11.0, *) {
            fr.origin.x = self.view.safeAreaInsets.left
            fr.size.width = fr.size.width - (fr.origin.x*2)
        }
        if let infoView = PyrusServiceDesk.mainController?.customization?.infoView, !(PSDMessagesStorage.pyrusUserDefaults()?.bool(forKey: PSD_WAS_CLOSE_INFO_KEY) ?? true){
            fr.origin.y += infoView.frame.size.height
            fr.size.height -= infoView.frame.size.height
        }

        self.tableView.frame = fr
        
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        self.tableView.removeListeners()
        self.tableView.bottomPSDRefreshControl.isEnabled = false
        guard let lastVisibleCell = self.tableView.visibleCells.last,
              let lastVisibleRow = self.tableView.indexPath(for: lastVisibleCell) else {
            return
        }
        coordinator.animateAlongsideTransition(in: self.tableView, animation: { (context) in
            self.tableView.reloadData()
            if self.tableView.contentOffset.y > 100,
               self.tableView.numberOfSections > lastVisibleRow.section,
               self.tableView.numberOfRows(inSection: lastVisibleRow.section) > lastVisibleRow.row
            {
                self.tableView.scrollToRow(at: lastVisibleRow, at: .bottom, animated: false)
            }
        }, completion: { context in
            self.tableView.bottomPSDRefreshControl.isEnabled = true
            self.tableView.addKeyboardListeners()
        })
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
        if firstLoad {
            self.messageInputView.inputTextView.becomeFirstResponder()
            firstLoad = false
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
        self.tableView.reloadChat()
        
    }
    lazy private var messageInputView : PSDMessageInputView = {
        let inputView = PSDMessageInputView.init(frame: CGRect(x: 0, y: self.view.frame.size.height-70, width: self.view.frame.size.width, height: 50))
        inputView.delegate = self
        recolorTextInput(inputView)
        return inputView
    }()
    lazy var tableView: PSDChatTableView = {
        let table = PSDChatTableView(frame: self.view.bounds)
        table.setupTableView()
        return table
    }()
    private func recolorTextInput(_ input: PSDMessageInputView) {
        let style = CustomizationHelper.keyboardStyle
        input.inputTextView.keyboardAppearance = style
        let (backInputColor, textInputColor) = CustomizationHelper.colorsForInput
        input.backgroundView.backgroundColor = backInputColor
        input.inputTextView.textColor = textInputColor
        input.sendButton.setTitleColor(textInputColor.withAlphaComponent(PSDMessageSendButton.titleDisabledAlpha), for: .disabled)
    }
    /**Setting design To PyrusSupportChatViewController view, add subviews*/
    private func design() {
        self.view.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground
        self.view.addSubview(self.tableView)
        self.tableView.addActivityView()
    }
    
    //MARK : Navigation
    //Setting design to navigation bar, title and buttons
    private func designNavigation()
    {
        if let view = PyrusServiceDesk.mainController?.customization?.chatTitleView {
            navigationItem.titleView = view
            view.sizeToFit()
            navigationController?.navigationBar.layoutIfNeeded()
        } else {
            title = CustomizationHelper.chatTitle
        }
        
        self.setItems()
    }
    ///Set navigation items
    private func setItems()
    {
        if let rightBarButtonItem = PyrusServiceDesk.mainController?.customization?.customRightBarButtonItem {
            rightBarButtonItem.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor
            navigationItem.rightBarButtonItem = rightBarButtonItem
        }
        if let leftBarButtonItem = PyrusServiceDesk.mainController?.customization?.customLeftBarButtonItem {
            leftBarButtonItem.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor
            navigationItem.leftBarButtonItem = leftBarButtonItem
        }else{
            let item = UIBarButtonItem.init(customView: leftButton)
            navigationItem.leftBarButtonItem = item
        }
    }

    private lazy var leftButton : UIButton = {
        let button = UIButton.init(type: .custom)
        button.titleLabel?.font = .backButton
        button.setTitle("Back".localizedPSD(), for: .normal)
        button.setTitleColor(PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor, for: .normal)
        let backImage = UIImage.PSDImage(name: "Back")?.withRenderingMode(.alwaysTemplate)
        button.setImage(backImage, for: .normal)
        button.addTarget(self, action: #selector(closeButtonAction), for: .touchUpInside)
        button.sizeToFit()
        button.tintColor = PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor
        return button
    }()
    private func customiseDesign(color:UIColor)
    {
        self.navigationItem.leftBarButtonItem?.tintColor = color
        self.navigationItem.rightBarButtonItem?.tintColor = color
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
        if !hasNoConnection() && !PSDGetChat.isActive() {
           self.tableView.updateChat(needProgress:false)
        }
        
    }
}
extension PSDChatViewController : PSDMessageInputViewDelegate{
    func send(_ message:String,_ attachments:[PSDAttachment]){
        let newMessage = PSDObjectsCreator.createMessage(message, attachments: attachments)
        prepareMessageForDrawing(newMessage)
        tableView.addNewRow(message: newMessage)
        PSDMessageSend.pass(newMessage, delegate: self.tableView)
    }
    func sendRate(_ rateValue: Int) {
        let newMessage = PSDObjectsCreator.createMessage(rating: rateValue)
        prepareMessageForDrawing(newMessage)
        tableView.addNewRow(message: newMessage)
        PSDMessageSend.pass(newMessage, delegate: self.tableView)
    }
    private func prepareMessageForDrawing(_ newMessage: PSDMessage) {
        newMessage.state = .sending
        if let attachments = newMessage.attachments {
            for attachment in attachments{
                guard attachment.emptyId() else {
                    continue
                }
                attachment.uploadingProgress = 0
            }
        }
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
    func refreshChat(showFakeMessage: Int?) {
        self.tableView.forceRefresh(showFakeMessage: showFakeMessage)
    }
}
extension PSDChatViewController: PSDChatTableViewDelegate {
    func needShowRate(_ showRate: Bool) {
        messageInputView.showRate = showRate
    }
    
    func restartTimer() {
        startGettingInfo()
    }
}

extension PSDChatViewController: UIAdaptivePresentationControllerDelegate {
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
        self.messageInputView.inputTextView.resignFirstResponder()
        super.present(viewControllerToPresent, animated: flag, completion: completion)
    }
}
private extension UIFont {
    static let backButton = CustomizationHelper.systemFont(ofSize: 18)
}
