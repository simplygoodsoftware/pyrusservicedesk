
import UIKit

///Protocol for updateting info
protocol PSDUpdateInfo {
    func startGettingInfo()
    func refreshChat(showFakeMessage: Int?)
}

class PSDChatViewController: PSDViewController {
    private let interactor: PSDChatInteractorProtocol
    private let router: PSDChatRouterProtocol
    
    required init(interactor: PSDChatInteractorProtocol, router: PSDChatRouterProtocol) {
        self.interactor = interactor
        self.router = router
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private var bottomTableView: NSLayoutConstraint?
    private var bottomScrollButton: NSLayoutConstraint?
    private lazy var scrollButton: UIButton = {
        let button = UIButton()
        button.backgroundColor = .white
        button.layer.cornerRadius = 20
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var badgeView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 8
        view.backgroundColor = .hRose
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var newMessageCount: UILabel = {
        let label = UILabel()
        label.font = CustomizationHelper.systemBoldFont(ofSize: 10)
        label.textColor = .white
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    lazy private var messageInputView: PSDMessageInputView = {
        let inputView = PSDMessageInputView.init(
            frame: CGRect(x: 0, y: view.frame.size.height - 70,
                          width: view.frame.size.width, height: 50)
        )
        inputView.delegate = self
        recolorTextInput(inputView)
        return inputView
    }()
    
    lazy var tableView: PSDChatTableView = {
        let table = PSDChatTableView(frame: self.view.bounds)
        table.setupTableView()
        return table
    }()
    
    private var firstLoad: Bool = true
    
    private var tableViewTopConstant: NSLayoutConstraint?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        presentationController?.delegate = self
        extendedLayoutIncludesOpaqueBars = true
        automaticallyAdjustsScrollViewInsets = false
        
        design()
        
        self.messageInputView.setToDefault()
        self.tableView.isLoading = true
        interactor.doInteraction(.viewDidload)
    }
    
    private func keyboardAnimationDuration(_ notification: NSNotification) -> TimeInterval{
        if let  duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSValue{
            return duration as? TimeInterval ?? 0
        }
        return 0
    }
    
//    @objc private func keyboardWillShow(_ notification: NSNotification) {
//        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
//            let keyboardEndFrame = infoEndKey.cgRectValue
//            let duration = keyboardAnimationDuration(notification)
//            animateInset(keyboardEndFrame.height, notification: notification)
//            if !isHide {
//                UIView.animate(withDuration: duration, animations: {
//                 //   self.bottomTableView?.constant =  -keyboardEndFrame.height
//                    self.bottomScrollButton?.constant = -38 - keyboardEndFrame.height
//                    self.view.layoutIfNeeded()
//                })
//            }
//            isHide = false
//        }
//    }
//    
//    private var isHide = false
//    @objc private func keyboardWillHide(_ notification: NSNotification) {
//        isHide = true
//        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
//            let keyboardEndFrame = infoEndKey.cgRectValue
//            let duration = keyboardAnimationDuration(notification)
//            UIView.animate(withDuration: duration, animations: {
//                //self.bottomTableView?.constant = -44
//             //   self.bottomTableView?.constant =  -86//keyboardEndFrame.height
//                self.bottomScrollButton?.constant = -120
//                self.view.setNeedsLayout()
//            })
//        }
//    }
//    
//    private func animateInset(_ keyboardHeight:CGFloat, notification: NSNotification){
//        let duration = keyboardAnimationDuration(notification)
//        let safeArea =  tableView.bottomPSDRefreshControl.insetHeight
//        let bottomUnvisibleHeight = keyboardHeight + safeArea
//        var offsetDelta : CGFloat = 0
//        guard needChangeInset(bottomUnvisibleHeight) else {
//            return
//        }
//        let needChangeOffset = needChangeOffset(keyboardHeight: bottomUnvisibleHeight )
//        if needChangeOffset {
//            offsetDelta = bottomUnvisibleHeight - tableView.contentInset.top
//            offsetDelta = min(offsetDelta,  tableView.contentSize.height - (tableView.frame.size.height-bottomUnvisibleHeight))
//            offsetDelta = offsetDelta - tableView.bottomPSDRefreshControl.insetHeight
//        }
//        let newOffset = newOffset(delta: offsetDelta, bottomInset: bottomUnvisibleHeight)
//        UIView.animate(withDuration: duration, delay: 0, animations: {
//            self.tableView.contentInset.top = bottomUnvisibleHeight
//            self.tableView.scrollIndicatorInsets.top = bottomUnvisibleHeight
//            if needChangeOffset {
//                self.tableView.contentOffset.y = newOffset
//            }
//        }, completion: nil)
//    }
//    
    private func needChangeInset(_ newInset: CGFloat) -> Bool {
        if
            let presentedViewController = tableView.findViewController()?.presentedViewController,
            presentedViewController.isBeingDismissed
        {
            return tableView.contentInset.bottom <= newInset
        }
        return true
    }
    
    private func newOffset(delta: CGFloat, bottomInset: CGFloat) -> CGFloat {
        var newOffsetY = max(0,tableView.contentOffset.y + delta)//block  too little offset
        newOffsetY = min(tableView.contentSize.height - (tableView.frame.size.height - bottomInset),newOffsetY)//block  too big offset
        return newOffsetY
    }
    
    private func needChangeOffset(keyboardHeight:CGFloat)->Bool{
        if(tableView.contentSize.height > (tableView.frame.size.height-keyboardHeight) && !tableView.isDragging){
            return true
        }
        return false
    
    }
    
    private var isKeyBoardOpen = false
    @objc private func keyboardDidHide(_ notification: NSNotification) {
        isKeyBoardOpen = false
    }
    
    @objc private func keyboardDidShow(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            let keyboardEndFrame = infoEndKey.cgRectValue
            let keyboardHeight = keyboardEndFrame.height
            
            if keyboardHeight > 200 {
                isKeyBoardOpen = true
            }
        }
    }
    
    @objc private func keyboardWillShow(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            let keyboardEndFrame = infoEndKey.cgRectValue
            let duration = keyboardAnimationDuration(notification)
            let keyboardHeight = keyboardEndFrame.height
            print("keyboardWillShow: duration: \(duration), keyboardHeight: \(keyboardHeight)")
            UIView.animate(withDuration: duration, delay: 0, animations: {
                let oldInset = self.tableView.contentInset.top
                self.tableView.contentInset.top = keyboardHeight
                if !self.isKeyBoardOpen {
                    self.tableView.contentOffset.y -= self.tableView.contentInset.top - oldInset
                }
            })
        }
    }
    
    @objc private func keyboardWillHide(_ notification: NSNotification) {
        if let infoEndKey: NSValue = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            let keyboardEndFrame = infoEndKey.cgRectValue
            let duration = keyboardAnimationDuration(notification)
            let keyboardHeight = keyboardEndFrame.height
            print("keyboardWillHide: duration: \(duration), keyboardHeight: \(keyboardHeight)")
            UIView.animate(withDuration: duration, delay: 0, animations: {
                let oldInset = self.tableView.contentInset.top
                self.tableView.contentInset.top = keyboardHeight
            })
        }
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        resizeTable()
        scrollButton.layer.shadowColor = UIColor.black.cgColor
        scrollButton.layer.shadowOffset = CGSize(width: 0, height: 4)
        scrollButton.layer.shadowRadius = 4
        scrollButton.layer.shadowOpacity = 0.2
        scrollButton.layer.masksToBounds = false
    }

    private var firstLayout: Bool = true
    override func viewDidLayoutSubviews() {
        if firstLayout {
            tableView.scrollsToBottom(animated: false)
            firstLayout = false
            tableView.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
        }
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        self.tableView.removeListeners()
        self.tableView.bottomPSDRefreshControl.isEnabled = false
        guard let lastVisibleCell = self.tableView.visibleCells.last,
              let lastVisibleRow = self.tableView.indexPath(for: lastVisibleCell) else {
            return
        }
        
        coordinator.animateAlongsideTransition(in: self.tableView, animation: { context in
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
        startGettingInfo()
        resizeTable()
        
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardWillHide(_:)), name: UIResponder.keyboardWillHideNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardDidShow(_:)), name: UIResponder.keyboardDidShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardDidHide(_:)), name: UIResponder.keyboardDidHideNotification, object: nil)
//        NotificationCenter.default.addObserver(self, selector:  #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tableView.isVisible = true
        UIView.performWithoutAnimation {
            self.becomeFirstResponder()
            messageInputView.inputTextView.resignFirstResponder()
        }
        interactor.doInteraction(.viewDidAppear)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
        EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "hideAllKeyboard() called in viewWillDisappear")
        hideAllKeyboard()
        self.tableView.removeListeners()
        interactor.doInteraction(.viewWillDisappear)
    }
    
    override func recolor() {
        super.recolor()
        recolorTextInput(messageInputView)
    }
    
    func resizeTable() {
        if let infoView = PyrusServiceDesk.mainController?.customization?.infoView, !(PSDMessagesStorage.pyrusUserDefaults()?.bool(forKey: PSD_WAS_CLOSE_INFO_KEY) ?? true) {
            tableViewTopConstant?.constant = infoView.frame.size.height
        } else {
            tableViewTopConstant?.constant = 0
        }
    }
    
    @objc private func appEnteredForeground(){
        self.tableView.addKeyboardListeners()
    }
    @objc private func appEnteredBackground(){
        self.tableView.removeListeners()
    }
    
    public func updateTitle() {
        designNavigation()
        messageInputView.setToDefault()
        tableView.isLoading = false
        tableView.reloadChat()
    }
    
    ///hide keyboard with inputAccessoryView
    func hideAllKeyboard(){
        if self.messageInputView.inputTextView.isFirstResponder {
            self.resignFirstResponder()
            self.messageInputView.inputTextView.resignFirstResponder()
        }
    }
    
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
        view.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground
        designNavigation()
        setupTableView()
        setupInfoView()
        customiseDesign(color: PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor)
        setupScrollButton()
    }
    
    func setupTableView() {
        tableView.addKeyboardListeners()
        tableView.chatDelegate = self
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .never//.automatic
            
        }
        if #available(iOS 13.0, *) {
            tableView.automaticallyAdjustsScrollIndicatorInsets = false
        }
//        tableView.semanticContentAttribute = .forceRightToLeft

        tableView.contentInset.top = 10
        
        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        if #available(iOS 11.0, *) {
            tableViewTopConstant = tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor)
            tableView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor).isActive = true
            tableView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor).isActive = true
        } else {
            tableViewTopConstant = tableView.topAnchor.constraint(equalTo: view.layoutMarginsGuide.topAnchor)
            tableView.leadingAnchor.constraint(equalTo: view.layoutMarginsGuide.leadingAnchor).isActive = true
            tableView.trailingAnchor.constraint(equalTo: view.layoutMarginsGuide.trailingAnchor).isActive = true
        }
        tableViewTopConstant?.isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
//        tableView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -44).isActive = true
//        bottomTableView = tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -86)
//        if #available(iOS 15.0, *) {
//            bottomTableView = tableView.bottomAnchor.constraint(equalTo: view.keyboardLayoutGuide.topAnchor, constant: 0)
//        } else {
//            // Fallback on earlier versions
//        }
//        bottomTableView?.isActive = true
//        if #available(iOS 15.0, *) {
//            tableView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -44).isActive = true
//        } else {
//            // Fallback on earlier versions
//        }
        tableView.addActivityView()
    }
    
    func setupScrollButton() {
        view.addSubview(scrollButton)
        let image = UIImageView(image: UIImage.PSDImage(name: "down"))
        image.translatesAutoresizingMaskIntoConstraints = false
        scrollButton.addSubview(image)
        badgeView.addSubview(newMessageCount)
        scrollButton.addSubview(badgeView)
        
        NSLayoutConstraint.activate([
            scrollButton.heightAnchor.constraint(equalToConstant: 40),
            scrollButton.widthAnchor.constraint(equalToConstant: 40),
            scrollButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -10),
           // scrollButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -400),
            image.centerXAnchor.constraint(equalTo: scrollButton.centerXAnchor),
            image.centerYAnchor.constraint(equalTo: scrollButton.centerYAnchor),
            badgeView.bottomAnchor.constraint(equalTo: scrollButton.bottomAnchor, constant: -28),
            badgeView.trailingAnchor.constraint(equalTo: scrollButton.trailingAnchor, constant: 4),
            badgeView.heightAnchor.constraint(equalToConstant: 16),
            badgeView.widthAnchor.constraint(greaterThanOrEqualToConstant: 16),
            newMessageCount.trailingAnchor.constraint(equalTo: badgeView.trailingAnchor, constant: -4),
            newMessageCount.widthAnchor.constraint(greaterThanOrEqualToConstant: 8),
            newMessageCount.centerYAnchor.constraint(equalTo: badgeView.centerYAnchor),
            badgeView.leadingAnchor.constraint(equalTo: newMessageCount.leadingAnchor, constant: -4)
        ])
        bottomScrollButton = scrollButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -120)
        bottomScrollButton?.isActive = true
        
        scrollButton.addTarget(self, action: #selector(scrollToBottom), for: .touchUpInside)
    }
    
    @objc func scrollToBottom() {
        tableView.scrollsToBottom(animated: true)
    }
    
    func setupInfoView() {
        if let infoView = PyrusServiceDesk.mainController?.customization?.infoView, !(PSDMessagesStorage.pyrusUserDefaults()?.bool(forKey: PSD_WAS_CLOSE_INFO_KEY) ?? true) {
            view.addSubview(infoView)
            infoView.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
            infoView.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
            infoView.topAnchor.constraint(equalTo: view.topAnchor, constant: (self.navigationController?.navigationBar.frame.size.height ?? 0) +  UIApplication.shared.statusBarFrame.height).isActive = true
        }
    }
    
    //MARK : Navigation
    //Setting design to navigation bar, title and buttons
    private func designNavigation() {
        if #available(iOS 11.0, *) {
            navigationItem.largeTitleDisplayMode = .never
            
        }
        navigationController?.navigationBar.isTranslucent = true
        
        if #available(iOS 13.0, *) {
            let barAppearance = UIBarAppearance()
            barAppearance.backgroundColor = UIColor(hex: "#F9F9F9F0")
            let bigAppear = UINavigationBarAppearance(barAppearance: barAppearance)
            bigAppear.configureWithOpaqueBackground()
            bigAppear.backgroundColor = UIColor(hex: "#F9F9F9F0")
            bigAppear.backgroundEffect = nil
            navigationItem.scrollEdgeAppearance = bigAppear
            navigationItem.standardAppearance = bigAppear
        }
        self.setItems()
    }
    
    ///Set navigation items
    private func setItems() {
        if let rightBarButtonItem = PyrusServiceDesk.mainController?.customization?.customRightBarButtonItem {
            rightBarButtonItem.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor
            navigationItem.rightBarButtonItem = rightBarButtonItem
        }
        if let leftBarButtonItem =
            PyrusServiceDesk.mainController?.customization?.customLeftBarButtonItem {
            leftBarButtonItem.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor
            navigationItem.leftBarButtonItem = leftBarButtonItem
        } else {
            let item = UIBarButtonItem.init(customView: leftButton)
            navigationItem.leftBarButtonItem = item
        }
    }

    private lazy var leftButton: UIButton = {
        let button = UIButton.init(type: .custom)
        let mainColor = PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor
        button.titleLabel?.font = .backButton
        button.setTitle("Back".localizedPSD(), for: .normal)
        button.setTitleColor(mainColor, for: .normal)
        let backImage = UIImage.PSDImage(name: "Back")?.withRenderingMode(.alwaysTemplate)
        button.setImage(backImage?.imageWith(color: mainColor), for: .normal)
        button.addTarget(self, action: #selector(closeButtonAction), for: .touchUpInside)
        button.sizeToFit()
        button.tintColor = mainColor
        return button
    }()
    
    private func customiseDesign(color: UIColor) {
        self.navigationItem.leftBarButtonItem?.tintColor = color
        self.navigationItem.rightBarButtonItem?.tintColor = color
    }
    
    @objc private func closeButtonAction() {
        router.route(to: .close)
        EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "hideAllKeyboard() called after press on back button")
        UIView.performWithoutAnimation {
            hideAllKeyboard()
        }
    }
    
    //MARK: KeyBoard hiding and moving
    override var canBecomeFirstResponder: Bool {
        guard
            self.tableView.window != nil,
            !hasNoConnection(),
            self.presentedViewController == nil
        else {
            return false
        }
        return true
    }
    
    private func hasNoConnection() -> Bool {
        if self.view.subviews.contains(self.tableView.noConnectionView) {
            return true
        }
        return false
    }
    
    override var inputAccessoryView: UIView? {
        return messageInputView
    }
}

private extension PSDChatViewController {
    func needShowRate(_ showRate: Bool) {
        messageInputView.showRate = showRate
    }
    
    func dataIsShown() {
        if firstLoad {
            self.messageInputView.inputTextView.becomeFirstResponder()
            firstLoad = false
        }
    }
}

extension PSDChatViewController: PSDChatViewProtocol {
    func show(_ action: PSDChatSearchViewCommand) {
        switch action {
        case .addFakeMessage(let messageId):
            tableView.addFakeMessage(messageId: messageId)
        case .updateButtons(buttons: let buttons):
            tableView.updateButtonsView(buttons: buttons)
        case .updateRows(indexPaths: let indexPaths):
            tableView.updateRows(indexPaths: indexPaths)
        case .removeNoConnectionView:
            tableView.removeNoConnectionView()
        case .endRefreshing:
            tableView.endRefreshing()
        case .reloadChat:
            tableView.reloadChat()
        case .needShowRate(showRate: let showRate):
            needShowRate(showRate)
        case .showNoConnectionView:
            tableView.showNoConnectionView()
        case .scrollsToBottom(animated: let animated):
            tableView.scrollsToBottom(animated: animated)
        case .endLoading:
            tableView.isLoading = false
        case .dataIsShown:
            dataIsShown()
        case .drawTableWithData:
            tableView.drawTableWithData()
        case .updateTableMatrix(matrix: let matrix):
            tableView.tableMatrix = matrix
        case .addRow(index: let index, lastIndexPath: let lastIndexPath, insertSections: let insertSections, scrollsToBottom: let scrollsToBottom):
            tableView.addRow(at: index, lastIndexPath: lastIndexPath, insertSections: insertSections, scrollsToBottom: scrollsToBottom)
        case .addNewRow:
            if(tableView.numberOfRows(inSection: 0) == 0) {
                tableView.addNewRow() { [weak self] in
                    self?.interactor.doInteraction(.addNewRow)
                }
            } else {
                interactor.doInteraction(.addNewRow)
            }
        case .redrawCell(indexPath: let indexPath, message: let message):
            tableView.redrawCell(at: indexPath, with: message)
        case .insertSections(sections: let sections):
            tableView.insertSections(sections: sections)
        case .deleteSections(sections: let sections):
            tableView.deleteSections(sections: sections)
        case .moveRow(movedIndexPath: let movedIndexPath, newIndexPath: let newIndexPath):
            tableView.moveRow(movedIndexPath: movedIndexPath, newIndexPath: newIndexPath)
        case .deleteRows(indexPaths: let indexPaths, section: let section):
            tableView.deleteRows(indexPaths: indexPaths, section: section)
        case .showKeyBoard:
            messageInputView.inputTextView.becomeFirstResponder()
        case .reloadAll:
            tableView.reloadAll()
        case .updateTitle(connectionError: let connectionError):
            if !connectionError {
                if let view = PyrusServiceDesk.mainController?.customization?.chatTitleView {
                    navigationItem.titleView = view
                    view.sizeToFit()
                    navigationController?.navigationBar.layoutIfNeeded()
                } else {
                    title = CustomizationHelper.chatTitle
                }
            } else {
                let label = UILabel()
                label.text = "Waiting_For_Network".localizedPSD()
                label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
                navigationItem.titleView = label
                tableView.endRefreshing()
            }
        case .reloadTitle:
            designNavigation()
        case .updateBadge(messagesCount: let messagesCount):
            newMessageCount.text = "\(messagesCount)"
            badgeView.isHidden = false
        }
    }
}

extension PSDChatViewController: PSDMessageInputViewDelegate {
    func addButtonTapped() {
        self.becomeFirstResponder()
    }
    
    func send(_ message:String, _ attachments: [PSDAttachment]) {
        interactor.doInteraction(.send(message: message, attachments: attachments))
    }
    
    func sendRate(_ rateValue: Int) {
        interactor.doInteraction(.sendRate(rateValue: rateValue))
    }
}

extension PSDChatViewController: PSDUpdateInfo {
    func startGettingInfo() {
        interactor.doInteraction(.startGettingInfo)
    }
    
    func refreshChat(showFakeMessage: Int?) {
        interactor.doInteraction(.forceRefresh(showFakeMessage: showFakeMessage))
    }
}

extension PSDChatViewController: PSDChatTableViewDelegate {
    func updateScrollButton(isHidden: Bool) {
        scrollButton.isHidden = isHidden
        if isHidden {
            badgeView.isHidden = true
        }
        interactor.doInteraction(.scrollButtonVisibleUpdated(isHidden: isHidden))
    }
    
    func updateNoConnectionVisible(visible: Bool) {
        interactor.doInteraction(.updateNoConnectionVisible(visible: visible))
    }
    
    func reloadChat() {
        interactor.doInteraction(.reloadChat)
    }
    
    func refresh() {
        interactor.doInteraction(.refresh)
    }
    
    func sendAgainMessage(indexPath: IndexPath) {
        interactor.doInteraction(.sendAgainMessage(indexPath: indexPath))
    }
    
    func deleteMessage(indexPath: IndexPath) {
        interactor.doInteraction(.deleteMessage(indexPath: indexPath))
    }
    
    func showLinkOpenAlert(_ linkString: String) {
        router.route(to: .showLinkOpenAlert(linkString: linkString))
    }
}

extension PSDChatViewController: UIAdaptivePresentationControllerDelegate {
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
        super.present(viewControllerToPresent, animated: flag, completion: completion)
    }
}
private extension UIFont {
    static let backButton = CustomizationHelper.systemFont(ofSize: 18)
}
