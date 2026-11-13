import UIKit

///Протокол обновления информации по заявке.
protocol PSDUpdateInfo {
    func startGettingInfo()
    func refreshChat(showFakeMessage: Int?)
}

class PSDChatViewController: PSDViewController, PSDMainController {
    var customization: ServiceDeskConfiguration?
    
    func updateInfo() {
        startGettingInfo()
    }
    
    func updateTitleChat() {
        updateTitle()
    }
    
    func closeServiceDesk() {
        if PyrusServiceDeskController.PSDIsOpen() {
            let alertAuthorized = UIAlertController(title: nil, message: "AcсessDenied".localizedPSD(), preferredStyle: .alert)
            alertAuthorized.addAction(UIAlertAction(title: "OK".localizedPSD(), style: .default, handler: { (_) in
                self.remove(animated: true)
            }))
            self.present(alertAuthorized, animated: true, completion: nil)
        }
    }
    
    func remove(animated: Bool) {
        navigationController?.popViewController(animated: true)
        PyrusLogger.shared.saveLocalLogToDisk()
        PyrusServiceDesk.stopCallback?.onStop()
        PyrusLogger.shared.logEvent(" Chat closed ")
        PyrusServiceDeskController.clean()
        PyrusServiceDesk.isStarted = false
    }
    
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
    
    private lazy var popoverContentController: PopoverContentController = PopoverContentController(ticketId: "", userName: "", createdAt: "")
    private var bottomTableView: NSLayoutConstraint?
    private var bottomScrollButton: NSLayoutConstraint?
    private lazy var scrollButton: UIButton = {
        let button = UIButton()
        button.backgroundColor = CustomizationHelper.scrollButtonColor
        button.layer.cornerRadius = 20
        button.translatesAutoresizingMaskIntoConstraints = false
        button.transform = CGAffineTransform(scaleX: 0, y: 0)
        return button
    }()
    
    ///Стеклянный круг под кнопкой скролла. Используется только в Liquid Glass оформлении.
    private lazy var scrollButtonGlass = PSDGlassBackgroundView(isInteractive: true)
    
    ///Вью, которую позиционируем, прячем и масштабируем: под флагом — стеклянный круг
    ///с кнопкой внутри, иначе — сама кнопка. Все внешние манипуляции идут через неё,
    ///чтобы стекло и кнопка не разъезжались.
    private var scrollControlView: UIView {
        PSDLiquidGlassStyle.isEnabled ? scrollButtonGlass : scrollButton
    }
    
    private var bottomStopButton: NSLayoutConstraint?
    private lazy var stopButton: UIButton = {
        let button = UIButton()
        button.backgroundColor = .clear
        button.layer.cornerRadius = 22
        button.translatesAutoresizingMaskIntoConstraints = false
        button.alpha = 0
        return button
    }()
    
    private lazy var badgeView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 8
        view.backgroundColor = CustomizationHelper.userMassageBackgroundColor
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
    
    private lazy var keyboardHandler = PSDChatKeyboardHandler(
        tableView: tableView,
        messageInputView: messageInputView,
        containerView: view
    )
    
    private lazy var closedTicketView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var infoButton: UIBarButtonItem? = {
        if PSDLiquidGlassStyle.isEnabled,
           let glassItem = PSDChatNavigationItemFactory.makeInfoItem(target: self,
                                                                     action: #selector(showPopover)) {
            return glassItem
        }
        if #available(iOS 14.0, *) {
            let button = UIBarButtonItem(image: UIImage(systemName: "info.circle"), style: .plain, target: self, action: #selector(showPopover))
            button.tintColor = .appColor
            return button
        } else {
            return nil
        }
    }()
    
    ///Заголовок чата на стеклянной капсуле. Используется только в Liquid Glass оформлении.
    private lazy var glassTitleView = PSDChatGlassTitleView()
    
    ///Подложка под навигационным баром, затухает вниз, к переписке.
    ///Используется только в Liquid Glass оформлении.
    private lazy var navigationBackdropView = PSDBlurBackdropView(fadeEdge: .bottom)
    private var navigationBackdropHeight: NSLayoutConstraint?
    
    private var firstLoad: Bool = true
    private var isActive: Bool = true
    ///Пришла ли информация по заявке — до этого кнопке «инфо» показывать нечего.
    private var isTicketInfoAvailable: Bool = false
    
    private var tableViewTopConstant: NSLayoutConstraint?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        presentationController?.delegate = self
        extendedLayoutIncludesOpaqueBars = true
        hidesBottomBarWhenPushed = true
        automaticallyAdjustsScrollViewInsets = false
        design()
        UIColor.psdBackground
        self.messageInputView.setToDefault()
        self.tableView.isLoading = true
        interactor.doInteraction(.viewDidload)
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        startGettingInfo()
        resizeTable()
        messageInputView.isHidden = false
        interactor.doInteraction(.viewWillAppear)
        
        navigationController?.navigationBar.isHidden = false
        navigationController?.setNavigationBarHidden(false, animated: false)
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appEnteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        keyboardHandler.startObserving()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tableView.isVisible = true
        if !PyrusServiceDesk.multichats {
            UIView.performWithoutAnimation {
                self.becomeFirstResponder()
                messageInputView.inputTextView.resignFirstResponder()
            }
        }
        interactor.doInteraction(.viewDidAppear)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
        keyboardHandler.stopObserving()
        if !PyrusServiceDesk.multichats {
            hideAllKeyboard()
            messageInputView.isHidden = true
        }
        
        interactor.doInteraction(.viewWillDisappear)
        
        UIView.animate(withDuration: 0.2, animations: {
            self.tabBarController?.tabBar.alpha = 1.0
        })
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        resizeTable()
        if !PSDLiquidGlassStyle.isEnabled {
            //У стекла своя объёмность, тень ему не нужна.
            scrollButton.layer.shadowColor = UIColor.black.cgColor
            scrollButton.layer.shadowOffset = CGSize(width: 0, height: 4)
            scrollButton.layer.shadowRadius = 4
            scrollButton.layer.shadowOpacity = 0.2
            scrollButton.layer.masksToBounds = false
        }
        
        if !PSDLiquidGlassStyle.isEnabled {
            messageInputView.backgroundView.backgroundColor = CustomizationHelper.colorsForInput.0
        }
    }

    private var firstLayout: Bool = true
    override func viewDidLayoutSubviews() {
        if firstLayout {
            firstLayout = false
            tableView.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
            interactor.doInteraction(.viewDidLayoutSubviews)
            let blurEffect = UIBlurEffect(style: .systemChromeMaterial)
            let blurEffectView = UIVisualEffectView(effect: blurEffect)
            blurEffectView.frame = closedTicketView.bounds
            closedTicketView.addSubview(blurEffectView)
            closedTicketView.sendSubviewToBack(blurEffectView)
        }
        keyboardHandler.logState("layoutSubviews")
        keyboardHandler.syncInsetWithInputViewIfNeeded()
    }
    
    override func recolor() {
        super.recolor()
        recolorTextInput(messageInputView)
        //PSDViewController.recolor() безусловно кладёт в titleView собственный лейбл.
        //Возвращаем оформленный заголовок, иначе стеклянная капсула живёт до первой
        //смены темы или установки title.
        guard PSDLiquidGlassStyle.isEnabled else { return }
        applyChatTitle()
        //Цвет бара берётся из кастомизации и может зависеть от темы — подложку перекрашиваем.
        updateNavigationBackdropColor()
        applyScrollButtonInterfaceStyle()
    }
    
    ///Кнопка скролла живёт у панели ввода и красится как она — по кастомизации
    ///клавиатуры, а не по теме системы. Override только на самой кнопке:
    ///trait collection контроллера остаётся системной, пересчёт не зацикливается.
    private func applyScrollButtonInterfaceStyle() {
        guard PSDLiquidGlassStyle.isEnabled else { return }
        if #available(iOS 13.0, *) {
            let systemStyle = traitCollection.userInterfaceStyle
            let desiredStyle = PSDLiquidGlassStyle.inputOverrideStyle(forSystemStyle: systemStyle)
            scrollControlView.overrideUserInterfaceStyle = desiredStyle == systemStyle ? .unspecified : desiredStyle
        }
    }
    
    func resizeTable() {
        let infoViewHeight = visibleInfoViewHeight
        guard PSDLiquidGlassStyle.isEnabled else {
            tableViewTopConstant?.constant = infoViewHeight
            return
        }
        //Таблица прижата к верху экрана — сдвигать её нельзя, иначе пропадёт уезжание под бар.
        tableViewTopConstant?.constant = 0
        navigationBackdropHeight?.constant = navigationBackdropHeightValue
        updateGlassTopContentInset(infoViewHeight: infoViewHeight)
    }
    
    ///Высота информационной вью заказчика, если она сейчас показана.
    private var visibleInfoViewHeight: CGFloat {
        guard let infoView = PyrusServiceDesk.mainController?.customization?.infoView,
              !(PSDMessagesStorage.pyrusUserDefaults()?.bool(forKey: PSD_WAS_CLOSE_INFO_KEY) ?? true)
        else {
            return 0
        }
        return infoView.frame.size.height
    }
    
    ///Отступ от навигационного бара для перевёрнутой таблицы.
    ///Таблица развёрнута на 180°, поэтому визуальный верх — это нижняя вставка.
    ///Именно вставка, а не констрейнт: контент должен подъезжать под стекло, а не обрезаться.
    ///Пишем не в `contentInset.bottom` напрямую — им владеет таблица и складывает
    ///этот отступ со своим отступом под лоадер.
    private func updateGlassTopContentInset(infoViewHeight: CGFloat) {
        tableView.additionalBottomInset = view.safeAreaInsets.top + infoViewHeight
    }
    
    @objc private func appEnteredForeground(){
    }
    @objc private func appEnteredBackground(){
    }
    
    public func updateTitle() {
        designNavigation()
        messageInputView.setToDefault()
        tableView.isLoading = false
        tableView.reloadChat()
    }
    
    ///Прячет клавиатуру вместе с inputAccessoryView.
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
        //В Liquid Glass панель прозрачная: её фон рисует блюр-подложка внутри самой панели.
        input.backgroundView.backgroundColor = PSDLiquidGlassStyle.isEnabled ? .clear : backInputColor
        input.inputTextView.textColor = textInputColor
        input.sendButton.setTitleColor(textInputColor.withAlphaComponent(PSDMessageSendButton.titleDisabledAlpha), for: .disabled)
    }
    
    /**Setting design To PyrusSupportChatViewController view, add subviews*/
    private func design() {
        view.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackgroundColor
        designNavigation()
        setupTableView()
        setupNavigationBackdropIfNeeded()
        setupInfoView()
        customiseDesign(color: PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor)
        setupScrollButton()
        setupStopButton()
        setupClosedTicketView()
    }
    
    func setupTableView() {
        tableView.chatDelegate = self
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .never
            
        }
        if #available(iOS 13.0, *) {
            tableView.automaticallyAdjustsScrollIndicatorInsets = false
        }
        tableView.semanticContentAttribute = .forceRightToLeft
        
        keyboardHandler.logState("inset=10")
        tableView.contentInset.top = 10
        
        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        if PSDLiquidGlassStyle.isEnabled {
            //Переписка уезжает под навигационный бар.
            //Отступ под баром задаётся не констрейнтом, а вставкой — см. resizeTable().
            tableViewTopConstant = tableView.topAnchor.constraint(equalTo: view.topAnchor)
            tableView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor).isActive = true
            tableView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor).isActive = true
        } else if #available(iOS 11.0, *) {
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
        tableView.addActivityView()
    }
    
    ///Подложка под навигационным баром. Добавляется поверх таблицы, чтобы переписка
    ///уезжала под неё, но остаётся ниже самого бара — его рисует навигационный контроллер.
    private func setupNavigationBackdropIfNeeded() {
        guard PSDLiquidGlassStyle.isEnabled else { return }
        if #available(iOS 26.0, *) {
            //Автоматический краевой эффект на перевёрнутой таблице гасит всю переписку.
            tableView.disableAutomaticScrollEdgeEffects()
        }
        
        updateNavigationBackdropColor()
        view.addSubview(navigationBackdropView)
        navigationBackdropView.translatesAutoresizingMaskIntoConstraints = false
        
        let heightConstraint = navigationBackdropView.heightAnchor.constraint(equalToConstant: 0)
        navigationBackdropHeight = heightConstraint
        
        NSLayoutConstraint.activate([
            navigationBackdropView.topAnchor.constraint(equalTo: view.topAnchor),
            navigationBackdropView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            navigationBackdropView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            heightConstraint
        ])
    }
    
    ///Dim-слой подложки должен совпадать с цветом бара из кастомизации, а не с темой
    ///системы: интегратор может задать тёмный бар в светлой теме и наоборот.
    private func updateNavigationBackdropColor() {
        navigationBackdropView.dimColor = CustomizationHelper.navigationBarColor
            .withAlphaComponent(PSDChatDesign.navigationBackdropDimAlpha)
    }
    
    ///Высота подложки — вся зона под баром вместе со статус-баром.
    private var navigationBackdropHeightValue: CGFloat {
        let safeAreaTop = view.safeAreaInsets.top
        guard safeAreaTop > 0 else {
            //Подстраховка на случай, если safe area ещё не посчитана:
            //берём геометрию бара напрямую, она уже включает статус-бар.
            return navigationController?.navigationBar.frame.maxY ?? 0
        }
        return safeAreaTop
    }
    
    func setupScrollButton() {
        embedScrollButtonIntoGlassIfNeeded()
        applyScrollButtonInterfaceStyle()
        view.addSubview(scrollControlView)
        let image = UIImageView(image: UIImage.PSDImage(name: "down"))
        image.translatesAutoresizingMaskIntoConstraints = false
        if PSDLiquidGlassStyle.isEnabled {
            image.image = image.image?.withRenderingMode(.alwaysTemplate)
            image.tintColor = PSDLiquidGlassStyle.iconColor
        }
        scrollButton.addSubview(image)
        badgeView.addSubview(newMessageCount)
        //Бейдж выступает за край кнопки, а стекло режет содержимое по своей форме —
        //поэтому в Liquid Glass бейдж лежит на контейнере, поверх стекла.
        scrollControlView.addSubview(badgeView)
        
        NSLayoutConstraint.activate([
            scrollControlView.heightAnchor.constraint(equalToConstant: 40),
            scrollControlView.widthAnchor.constraint(equalToConstant: 40),
            scrollControlView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -11),
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
        bottomScrollButton = scrollControlView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -110)
        bottomScrollButton?.isActive = true
        keyboardHandler.bottomScrollButton = bottomScrollButton
        
        scrollButton.addTarget(self, action: #selector(scrollToBottom), for: .touchUpInside)
        updateScrollButton(isHidden: true)
    }
    
    ///Кладёт кнопку скролла внутрь интерактивного стекла. Кнопка — внутри контента стекла,
    ///а не поверх него: так тап доходит до кнопки, а стекло анимирует нажатие.
    ///Заливку и стартовый масштаб кнопки перенимает контейнер.
    private func embedScrollButtonIntoGlassIfNeeded() {
        guard PSDLiquidGlassStyle.isEnabled else { return }
        scrollButtonGlass.translatesAutoresizingMaskIntoConstraints = false
        scrollButtonGlass.transform = scrollButton.transform
        scrollButton.transform = .identity
        scrollButton.backgroundColor = .clear
        
        let content = scrollButtonGlass.glassContentView
        content.addSubview(scrollButton)
        NSLayoutConstraint.activate([
            scrollButton.topAnchor.constraint(equalTo: content.topAnchor),
            scrollButton.bottomAnchor.constraint(equalTo: content.bottomAnchor),
            scrollButton.leadingAnchor.constraint(equalTo: content.leadingAnchor),
            scrollButton.trailingAnchor.constraint(equalTo: content.trailingAnchor)
        ])
    }
    
    func setupStopButton() {
        view.addSubview(stopButton)
        
        NSLayoutConstraint.activate([
            stopButton.heightAnchor.constraint(equalToConstant: 44),
            stopButton.widthAnchor.constraint(equalToConstant: 44),
            stopButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -11),
        ])
        bottomStopButton = stopButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -110)
        bottomStopButton?.isActive = true
        keyboardHandler.bottomStopButton = bottomStopButton
        
        stopButton.addTarget(self, action: #selector(stopRecording), for: .touchUpInside)
    }
    
    @objc func stopRecording() {
        messageInputView.stopRecord()
    }
    
    func setupClosedTicketView() {
        view.addSubview(closedTicketView)
        
        let closedTicketLabel = UILabel()
        closedTicketLabel.text = "ClosedTicketInfo".localizedPSD()
        closedTicketLabel.translatesAutoresizingMaskIntoConstraints = false
        closedTicketLabel.textColor = .lastMessageInfo
        closedTicketLabel.font = .systemFont(ofSize: 16, weight: .regular)
        closedTicketView.addSubview(closedTicketLabel)
        
        NSLayoutConstraint.activate([
            closedTicketView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            closedTicketView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            closedTicketView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            closedTicketView.heightAnchor.constraint(equalToConstant: 80),
            closedTicketLabel.topAnchor.constraint(equalTo: closedTicketView.topAnchor, constant: 8),
            closedTicketLabel.centerXAnchor.constraint(equalTo: closedTicketView.centerXAnchor)
        ])
        
        closedTicketView.isHidden = true
    }
    
    @objc func scrollToBottom() {
        tableView.scrollsToBottom(animated: true, keyBoardHeight: keyboardHandler.effectiveKeyboardHeight)
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
    //Оформление навигационного бара, заголовка и кнопок.
    private func designNavigation() {
        if #available(iOS 11.0, *) {
            navigationItem.largeTitleDisplayMode = .never
        }
        navigationController?.navigationBar.isTranslucent = true
        
        if #available(iOS 13.0, *) {
            if PSDLiquidGlassStyle.isEnabled {
                PSDNavigationBarStyler.applySystem(to: navigationItem)
            } else {
                PSDNavigationBarStyler.applyLegacy(to: navigationItem,
                                                   backgroundColor: CustomizationHelper.navigationBarColor)
            }
        }
        self.setItems()
        if PSDLiquidGlassStyle.isEnabled {
            //Вне Liquid Glass заголовок выставляется только по .updateTitle.
            applyChatTitle()
        }
        navigationController?.navigationBar.isHidden = false
    }
    
    @objc func showPopover(_ sender: UIBarButtonItem) {
        messageInputView.inputTextView.resignFirstResponder()
        if #available(iOS 15.0, *) {
            if let sheet = popoverContentController.sheetPresentationController {
                let smallId = UISheetPresentationController.Detent.Identifier("small")
                if #available(iOS 16.0, *) {
                    let smallDetent = UISheetPresentationController.Detent.custom(identifier: smallId) { context in
                        return context.maximumDetentValue * 0.3
                    }
                    sheet.detents = [smallDetent]
                } else {
                    sheet.detents = [.medium()]
                }
                
                sheet.prefersGrabberVisible = true
                sheet.preferredCornerRadius = 20
                sheet.prefersEdgeAttachedInCompactHeight = true
            }
        } else {
        }
        
        present(popoverContentController, animated: true, completion: nil)
    }
    
    ///Состав кнопок навигационного бара.
    ///
    ///Единственное место, где решается состав кнопок бара. Приоритет всегда за кастомизацией:
    ///если интегратор задал свою кнопку — ставим её, свои кнопки не подмешиваем.
    ///Правой кнопки может не быть вовсе: пока не пришла информация по заявке
    ///и своей кнопки у интегратора нет — справа пусто.
    private func setItems() {
        navigationItem.leftBarButtonItem = makeLeftBarButtonItem()
        navigationItem.rightBarButtonItem = makeRightBarButtonItem()
    }
    
    private func makeLeftBarButtonItem() -> UIBarButtonItem? {
        if let customItem = PyrusServiceDesk.mainController?.customization?.customLeftBarButtonItem {
            customItem.tintColor = customBarButtonTintColor
            return customItem
        }
        return makeDefaultBackItem()
    }
    
    private func makeRightBarButtonItem() -> UIBarButtonItem? {
        if let customItem = PyrusServiceDesk.mainController?.customization?.customRightBarButtonItem {
            customItem.tintColor = customBarButtonTintColor
            return customItem
        }
        return isTicketInfoAvailable ? infoButton : nil
    }
    
    ///В Liquid Glass кнопка возврата — иконка-шеврон, вне его — текстовая кнопка `leftButton`.
    private func makeDefaultBackItem() -> UIBarButtonItem {
        if PSDLiquidGlassStyle.isEnabled,
           let glassItem = PSDChatNavigationItemFactory.makeBackItem(target: self,
                                                                     action: #selector(closeButtonAction)) {
            return glassItem
        }
        return UIBarButtonItem(customView: leftButton)
    }
    
    ///Заголовок чата. Кастомная вью заказчика приоритетнее любого оформления.
    private func applyChatTitle() {
        if let customTitleView = PyrusServiceDesk.mainController?.customization?.chatTitleView {
            navigationItem.titleView = customTitleView
            customTitleView.sizeToFit()
            navigationController?.navigationBar.layoutIfNeeded()
            return
        }
        
        guard PSDLiquidGlassStyle.isEnabled else {
            title = CustomizationHelper.chatTitle
            return
        }
        
        let chatTitle = CustomizationHelper.chatTitle
        //Интегратор не задал ни chatTitle, ни chatTitleView: без этой проверки
        //по центру бара висела бы пустая капсула фиксированной высоты.
        guard !chatTitle.isEmpty else {
            navigationItem.titleView = nil
            return
        }
        
        glassTitleView.title = chatTitle
        glassTitleView.titleColor = CustomizationHelper.colorForChatTitle
        glassTitleView.sizeToFit()
        navigationItem.titleView = glassTitleView
        navigationController?.navigationBar.layoutIfNeeded()
    }
    
    ///Заголовок для состояния «нет сети».
    private func applyConnectionErrorTitle() {
        let errorText = "Waiting_For_Network".localizedPSD()
        
        if PSDLiquidGlassStyle.isEnabled {
            glassTitleView.title = errorText
            glassTitleView.titleColor = CustomizationHelper.colorForChatTitle
            glassTitleView.sizeToFit()
            navigationItem.titleView = glassTitleView
        } else {
            let label = UILabel()
            label.text = errorText
            label.textColor = CustomizationHelper.colorForChatTitle
            label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
            navigationItem.titleView = label
        }
        tableView.endRefreshing()
    }
    
    ///Цвет кнопок бара, заданных Service Desk.
    private var barButtonTintColor: UIColor {
        PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor
    }
    
    ///Цвет кнопок бара, заданных интегратором. Может быть `nil` — тогда кнопка
    ///остаётся с тем цветом, который выставил интегратор.
    private var customBarButtonTintColor: UIColor? {
        PyrusServiceDesk.mainController?.customization?.themeColor
        ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor
    }
    
    private lazy var leftButton: UIButton = {
        let button = UIButton.init(type: .custom)
        let mainColor = barButtonTintColor
        button.titleLabel?.font = .backButton
        button.setTitle(" " + "Back".localizedPSD(), for: .normal)
        button.setTitleColor(mainColor, for: .normal)
        let backImage = UIImage.PSDImage(name: "Back")?.withRenderingMode(.alwaysTemplate)
        button.setImage(backImage?.imageWith(color: mainColor), for: .normal)
        button.addTarget(self, action: #selector(closeButtonAction), for: .touchUpInside)
        button.sizeToFit()
        button.tintColor = mainColor
        return button
    }()
    
    private func customiseDesign(color: UIColor) {
        //В Liquid Glass цвет иконок задаёт фабрика, а кнопки интегратора
        //остаются с его цветом — перекрашивать здесь нечего.
        guard !PSDLiquidGlassStyle.isEnabled else { return }
        self.navigationItem.leftBarButtonItem?.tintColor = color
        self.navigationItem.rightBarButtonItem?.tintColor = color
    }
    
    @objc private func closeButtonAction() {
        router.route(to: .close)
        UIView.performWithoutAnimation {
            hideAllKeyboard()
        }
    }
    
    //MARK: KeyBoard hiding and moving
    override var canBecomeFirstResponder: Bool {
        guard
            isActive
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
        //Команда показа может прийти раньше, чем загрузятся значения текстового рейтинга, —
        //тогда показывать нечего. Состояние не трогаем: повторная команда с данными
        //пройдёт как первый показ и зарезервирует место под фактическую высоту.
        if showRate, !messageInputView.hasRateContent {
            return
        }
        let wasShown = messageInputView.showRate
        //Сначала установка: showRate пересчитывает фактическую высоту рейтинга.
        messageInputView.showRate = showRate
        if showRate && !wasShown {
            //Именно фактическая высота, а не RATE_HEIGHT: у текстового рейтинга она
            //зависит от числа плашек.
            keyboardHandler.applyRateInset(messageInputView.currentRateHeight)
        }
    }
    
    func dataIsShown() {
        if firstLoad {
            firstLoad = false
        }
    }
}

extension PSDChatViewController: PSDChatViewProtocol {
    func show(_ action: PSDChatSearchViewCommand) {
        switch action {
        case .updateButtons(buttons: let buttons):
            tableView.updateButtonsView(buttons: buttons)
        case .updateRows:
            tableView.updateRows(keyboardHeight: keyboardHandler.effectiveKeyboardHeight)
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
            tableView.scrollsToBottom(animated: animated, keyBoardHeight: keyboardHandler.effectiveKeyboardHeight)
        case .endLoading:
            tableView.isLoading = false
        case .dataIsShown:
            dataIsShown()
        case .drawTableWithData:
            tableView.drawTableWithData()
        case .updateTableMatrix(matrix: let matrix):
            tableView.tableMatrix = matrix
        case .addRow(scrollsToBottom: let scrollsToBottom):
            tableView.addRow(scrollsToBottom: scrollsToBottom, keyBoardHeight: keyboardHandler.effectiveKeyboardHeight)
        case .addNewRow:
            self.deletedAllAttachments()
            if(tableView.numberOfRows(inSection: 0) == 0) {
                tableView.addNewRow() { [weak self] in
                    self?.interactor.doInteraction(.addNewRow)
                }
            } else {
                interactor.doInteraction(.addNewRow)
            }
        case .redrawCell(indexPath: let indexPath, message: let message):
            tableView.redrawCell(at: indexPath, with: message)
        case .showKeyBoard:
            self.messageInputView.inputTextView.becomeFirstResponder()
        case .reloadAll(animated: let animated):
            tableView.reloadAll(animated: animated)
        case .updateTitle(connectionError: let connectionError):
            if !connectionError {
                applyChatTitle()
            } else {
                applyConnectionErrorTitle()
            }
        case .reloadTitle:
            designNavigation()
        case .updateBadge(messagesCount: let messagesCount):
            newMessageCount.text = "\(messagesCount)"
            badgeView.isHidden = false
        case .scrollToRow(indexPath: let indexPath):
            tableView.scrollToRow(at: indexPath, at: .middle, animated: false)
            tableView.setNeedsLayout()
            tableView.layoutIfNeeded()
        case .updateActive(isActive: let isActive):
            self.isActive = isActive
            keyboardHandler.isChatActive = isActive
            if !isActive {
                closedTicketView.isHidden = false
                keyboardHandler.applyInactiveInset()
                self.resignFirstResponder()
                self.messageInputView.inputTextView.resignFirstResponder()
            }
        case .updateInfo(ticketId: let ticketId, userName: let userName, createdAt: let createdAt):
            popoverContentController = PopoverContentController(ticketId: ticketId, userName: userName, createdAt: createdAt)
            isTicketInfoAvailable = true
            navigationItem.rightBarButtonItem = makeRightBarButtonItem()
        case .showRatingComment(ratingText: let ratingText, rating: let rating):
            messageInputView.inputTextView.resignFirstResponder()
            DispatchQueue.main.async { [weak self] in
                self?.router.route(to: .ratingComment(ratingText: ratingText, rating: rating))
            }
        case .updateOperatorTime(timeMessage: let timeMessage):
            tableView.updateOperatorTimeLabel(time: timeMessage)
        }
    }
}

extension PSDChatViewController: PSDMessageInputViewDelegate {
    func deletedAllAttachments() {
        keyboardHandler.handleAllAttachmentsDeleted()
    }
    
    func recordStop() {
        stopButton.alpha = 0
    }
    
    func recordStart() {
        stopButton.alpha = 1
    }
    
    func addAttachment() {
        keyboardHandler.handleAttachmentAdded()
    }
    
    func addButtonTapped() {
        keyboardHandler.markAddButtonTapped()
        messageInputView.inputTextView.resignFirstResponder()
    }
    
    func presenterForInputMenus() -> UIViewController? {
        return self
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
    func resignFirstResponderFromInputView() {
        messageInputView.inputTextView.resignFirstResponder()
    }
    
    func updateScrollButton(isAtBottom: Bool, isDragging: Bool) {
        if !isAtBottom && scrollControlView.isHidden == true && isDragging {
            UIView.animate(withDuration: 0.2) {
                self.scrollControlView.isHidden = false
                self.scrollControlView.transform = CGAffineTransform(scaleX: 1, y: 1)
            }
        } else if isAtBottom && scrollControlView.isHidden == false {
            badgeView.isHidden = true
            UIView.animate(withDuration: 0.2) {
                self.scrollControlView.transform = CGAffineTransform(scaleX: 0, y: 0)
            } completion: { _ in
                self.scrollControlView.isHidden = true
            }
        }
    }
    
    func updateScrollButton(isHidden: Bool) {
        if !isHidden && scrollControlView.isHidden == true {
            UIView.animate(withDuration: 0.2) {
                self.scrollControlView.isHidden = false
                self.scrollControlView.transform = CGAffineTransform(scaleX: 1, y: 1)
            }
        } else if isHidden && scrollControlView.isHidden == false {
            badgeView.isHidden = true
            UIView.animate(withDuration: 0.2) {
                self.scrollControlView.transform = CGAffineTransform(scaleX: 0, y: 0)
            } completion: { _ in
                self.scrollControlView.isHidden = true
            }
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

extension PSDChatViewController: RatingCommentDelegate {
    func sendRatingComment(comment: String?, rating: Int) {
        interactor.doInteraction(.sendRatingComment(comment: comment, rating: rating))
    }
}

extension PSDChatViewController: UIAdaptivePresentationControllerDelegate {
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
        super.present(viewControllerToPresent, animated: flag, completion: completion)
    }
}

extension PSDChatViewController: UIPopoverPresentationControllerDelegate {
    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }
}

private extension UIFont {
    static let backButton = CustomizationHelper.systemFont(ofSize: 18)
}

///Настройки Liquid Glass оформления экрана чата.
private enum PSDChatDesign {
    ///Плотность цветовой подмешки подложки под навигационным баром.
    static let navigationBackdropDimAlpha: CGFloat = 0.45
}

private extension UIColor {
    static let lastMessageInfo = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFFE5") ?? .white
        default:
            return UIColor(hex: "#60666C") ?? .systemGray
        }
    }
}
