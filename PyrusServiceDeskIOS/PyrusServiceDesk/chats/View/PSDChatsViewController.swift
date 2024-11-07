import UIKit

@available(iOS 13.0, *)
class PSDChatsViewController: UIViewController {
    private let interactor: ChatsInteractorProtocol
    private let  router: ChatsRouterProtocol?
    
    required init(interactor: ChatsInteractorProtocol, router: ChatsRouterProtocol) {
        self.interactor = interactor
        self.router = router
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private lazy var tableView: UITableView = {
        let table = UITableView()
        return table
    }()
    
    private var cellConfigurator: PSDChatsCellConfigurator?
    private var diffabledDataSource: UITableViewDiffableDataSource<PSDChatsSectionModel, AnyHashable>?
    
    private lazy var customRefresh: UIRefreshControl = {
        let refreshControl = UIRefreshControl()
        refreshControl.transform = CGAffineTransformMakeScale(0.8, 0.8)
        refreshControl.isOpaque = false
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()
    
    private var chats : [ChatViewModel] = [] {
        didSet {
            reloadDiffable(animated: true)
            emptyChatsView.isHidden = chats.count > 0
        }
    }
    
    private var isFiltered = false {
        didSet {
            let image = isFiltered ? UIImage(named: "fillFilter") : UIImage(named: "filter")
            filterImage.image = image?.imageWith(color: customization?.themeColor ?? .darkAppColor)
        }
    }
    
    private var timer: Timer?
    private var customization: ServiceDeskConfiguration?
    
    private lazy var filterInfoView: UIView = {
        let view = UIView()
        view.backgroundColor = customization?.themeColor
        return view
    }()
    
    private lazy var filterCross: UIButton = {
        let button = UIButton()
        let crossImage = UIImage(named: "Close")?.imageWith(color: .white)
        button.setImage(crossImage, for: .normal)
        button.setImage(crossImage?.imageWith(color: .white.withAlphaComponent(0.2)), for: .highlighted)
        button.isHidden = true
        return button
    }()
    
    private lazy var filterLabel: UILabel = {
        let label = UILabel()
        label.textColor = .white
        label.font = .systemFont(ofSize: 17)
        label.isHidden = true
        return label
    }()
    
    private lazy var plusView = UIButton()
    private lazy var emptyChatsView = UIView(frame: self.view.bounds)
    private lazy var filterImage = UIImageView()
    private lazy var filterButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        customization = PyrusServiceDesk.mainController?.customization
        designNavigation()
        design()
        startGettingInfo()
        interactor.doInteraction(.viewDidload)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        interactor.doInteraction(.viewWillAppear)
    }
    
    private var isFirstLayout = true
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        if isFirstLayout {
            filterInfoView.frame = CGRect(x: 0, y: Int(self.view.safeAreaInsets.top), width: Int(self.view.frame.width), height: 0)
            isFirstLayout = false
        }
    }
}

@available(iOS 13.0, *)
private extension PSDChatsViewController {
    /**Setting design To PSDChatsViewController view, add subviews*/
    func design() {
        view.backgroundColor = UIColor.psdBackground
        view.addSubview(tableView)
        view.addSubview(emptyChatsView)
        view.addSubview(filterInfoView)
        view.addSubview(plusView)

        setupEmptyChats()
        setupTableView()
        setupFilterInfoView()
        setupPlusView()
    }
    
    func setupEmptyChats() {
        let chatsImage = UIImageView(image: UIImage(named: "chats"))
        let openNewButton = UIButton(type: .system)
        emptyChatsView.isHidden = true
        let mainColor = customization?.barButtonTintColor ?? .darkAppColor
       
        openNewButton.setTitle("Создать обращение", for: .normal)
        openNewButton.titleLabel?.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        openNewButton.setTitleColor(mainColor, for: .normal)
        
        emptyChatsView.addSubview(chatsImage)
        emptyChatsView.addSubview(openNewButton)

        chatsImage.translatesAutoresizingMaskIntoConstraints = false
        openNewButton.translatesAutoresizingMaskIntoConstraints = false
      
        NSLayoutConstraint.activate([
            chatsImage.heightAnchor.constraint(equalToConstant: 90),
            chatsImage.widthAnchor.constraint(equalToConstant: 90),
            chatsImage.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            chatsImage.bottomAnchor.constraint(equalTo: openNewButton.topAnchor, constant: -16),
            openNewButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            openNewButton.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
        
        openNewButton.isUserInteractionEnabled = true
        openNewButton.addTarget(self, action: #selector(openNewChat), for: .touchUpInside)
    }
    
    func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        tableView.delegate = self
        tableView.backgroundColor = .psdBackground
        tableView.estimatedRowHeight = UITableView.automaticDimension
        tableView.rowHeight = UITableView.automaticDimension
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 0)
        tableView.keyboardDismissMode = .onDrag
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 100, right: 0)
        tableView.contentInsetAdjustmentBehavior = .automatic

        
        tableView.addSubview(customRefresh)
        
        cellConfigurator = PSDChatsCellConfigurator(tableView: tableView)
        setupDataSource()
    }
    
    @objc func refresh(sender: AnyObject) {
        interactor.doInteraction(.reloadChats)
    }
    
    func setupFilterInfoView() {
        filterInfoView.addSubview(filterCross)
        filterInfoView.addSubview(filterLabel)
        filterLabel.translatesAutoresizingMaskIntoConstraints = false
        filterCross.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            filterCross.centerYAnchor.constraint(equalTo: filterInfoView.centerYAnchor),
            filterCross.trailingAnchor.constraint(equalTo: filterInfoView.trailingAnchor, constant: -16),
            
            filterLabel.centerYAnchor.constraint(equalTo: filterInfoView.centerYAnchor),
            filterLabel.leadingAnchor.constraint(equalTo: filterInfoView.leadingAnchor, constant: 16),
            filterLabel.trailingAnchor.constraint(lessThanOrEqualTo: filterCross.leadingAnchor, constant: -16),
            
            filterCross.heightAnchor.constraint(lessThanOrEqualToConstant: 40),
            filterLabel.heightAnchor.constraint(lessThanOrEqualToConstant: 40)
        ])
        
        filterCross.addTarget(self, action: #selector(deleteFilter), for: .touchUpInside)
    }
        
    func setFilter() {
        isFiltered = true
        UIView.animate(withDuration: 0.2, animations: {
            self.tableView.contentInset.top = 50
            self.filterInfoView.frame.size.height = 50
            self.view.setNeedsLayout()
            self.tableView.layoutIfNeeded()
            self.filterLabel.isHidden = false
            self.filterCross.isHidden = false
            if self.chats.count > 0 {
                self.tableView.scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: false)
            }
        })
    }
    
    @objc func deleteFilter() {
        interactor.doInteraction(.deleteFilter)
        isFiltered = false
        filterLabel.isHidden = true
        filterCross.isHidden = true
        UIView.animate(withDuration: 0.2, animations: {
            self.tableView.contentInset.top = 0
            self.filterInfoView.frame.size.height = 0
            self.view.setNeedsLayout()
            self.tableView.layoutIfNeeded()
        })
    }
    
    func setupPlusView() {
        let plus = UIImageView(image: UIImage(systemName: "plus"))
        plus.tintColor = .white
        plusView.addSubview(plus)
        plusView.backgroundColor = customization?.themeColor
        plusView.layer.cornerRadius = 28
        plusView.translatesAutoresizingMaskIntoConstraints = false
        plus.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            plus.heightAnchor.constraint(equalToConstant: 28),
            plus.widthAnchor.constraint(equalToConstant: 28),
            plus.centerXAnchor.constraint(equalTo: plusView.centerXAnchor),
            plus.centerYAnchor.constraint(equalTo: plusView.centerYAnchor),
            
            plusView.heightAnchor.constraint(equalToConstant: 56),
            plusView.widthAnchor.constraint(equalToConstant: 56),
            plusView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            plusView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -68)
        ])
        
        plusView.addTarget(self, action: #selector(openNewChat), for: .touchUpInside)
    }
    
    @objc func openNewChat() {
        interactor.doInteraction(.newChat)
    }

    //*Setting design to navigation bar, title and buttons*/
    func designNavigation() {
        customization?.setCustomLeftBarButtonItem(backBarButtonItem())
        let bigAppear = UINavigationBarAppearance(barAppearance: UIBarAppearance())
        bigAppear.configureWithOpaqueBackground()
        bigAppear.backgroundColor = UIColor(hex: "#F9F9F9F0")
        navigationItem.scrollEdgeAppearance = bigAppear
        navigationItem.standardAppearance = bigAppear
        navigationItem.rightBarButtonItem = customization?.chatsRightBarButtonItem
        setupNavTitle()
        setupFilterButton()
    }
    
    func setupFilterButton() {
        let mainColor = customization?.themeColor ?? .darkAppColor
        filterImage = UIImageView(image: UIImage(named: "filter")?.imageWith(color: mainColor))
        filterButton.addSubview(filterImage)
        filterButton.bounds = CGRect(x: 0, y: -1, width: 28, height: 28)
        filterImage.center = filterButton.center
        filterImage.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            filterImage.heightAnchor.constraint(equalToConstant: 30),
            filterImage.widthAnchor.constraint(equalToConstant: 30),
        ])
        filterImage.sizeToFit()
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: filterButton)
        filterButton.isHidden = true
        if #available(iOS 14.0, *) {
            filterButton.showsMenuAsPrimaryAction = true
        }
    }
    
    func setupNavTitle() {
        let titleView = UIView()
        let title = UILabel()
        let icon = UIImageView(image: UIImage(named: "iiko"))
        titleView.addSubview(title)
        titleView.addSubview(icon)
        titleView.translatesAutoresizingMaskIntoConstraints = false
        title.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: titleView.leadingAnchor),
            icon.heightAnchor.constraint(equalToConstant: 24),
            icon.widthAnchor.constraint(equalToConstant: 24),
            title.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 6),
            title.centerYAnchor.constraint(equalTo: icon.centerYAnchor),
            titleView.heightAnchor.constraint(equalToConstant: 24),
            titleView.trailingAnchor.constraint(equalTo: title.trailingAnchor)
        ])
        
        title.text = PyrusServiceDesk.clientName
        title.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        icon.layer.cornerRadius = 12
        icon.clipsToBounds = true
        icon.contentMode = .scaleAspectFill
        navigationItem.titleView = titleView
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(titleViewTapped))
        titleView.addGestureRecognizer(tapGesture)
    }
    
    @objc func titleViewTapped() {
        if let action = customization?.titleHandler {
            action()
        }
    }
    
    func backBarButtonItem() -> UIBarButtonItem {
        let mainColor = customization?.barButtonTintColor ?? .darkAppColor
        let button = UIButton()
        button.titleLabel?.font = CustomizationHelper.systemFont(ofSize: 18)
        button.setTitle(" " + "Back".localizedPSD(), for: .normal)
        button.setTitleColor(mainColor, for: .normal)
        button.setTitleColor(mainColor.withAlphaComponent(0.2), for: .highlighted)
        let backImage = UIImage(systemName: "chevron.left", withConfiguration: UIImage.SymbolConfiguration(pointSize: 18, weight: .semibold, scale: .large))
        button.setImage(backImage?.imageWith(color: mainColor), for: .normal)
        button.setImage(backImage?.imageWith(color: mainColor.withAlphaComponent(0.2)), for: .highlighted)
        button.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        button.sizeToFit()
        return UIBarButtonItem(customView: button)
    }
    
    @objc func goBack() {
        router?.route(to: .goBack)
    }
    
    func openChat(_ chat: PSDChat) {
        let label = UILabel()
        label.isUserInteractionEnabled = true
        label.textAlignment = .center
        label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        label.text = chat.subject?.count ?? 0 > 0 ? chat.subject : "Новое обращение"
        label.translatesAutoresizingMaskIntoConstraints = false
        label.widthAnchor.constraint(equalToConstant: 200).isActive = true
        
        customization?.setChatTitileView(label)
        router?.route(to: .chat(chat: chat))
    }

    func setupDataSource() {
        let newDataSource = PSDChatsDiffableDataSource.createDataSource(for: tableView, cellCreator: self)
        tableView.dataSource = newDataSource
        diffabledDataSource = newDataSource
    }

    func reloadDiffable(animated: Bool) {
        guard let diffabledDataSource = diffabledDataSource else { return }
        var snapshot = NSDiffableDataSourceSnapshot<PSDChatsSectionModel, AnyHashable>()
        let section = PSDChatsSectionModel()
        snapshot.appendSections([section])
        snapshot.appendItems(chats, toSection: section)
        
        diffabledDataSource.apply(snapshot, animatingDifferences: animated)
        self.diffabledDataSource = diffabledDataSource
    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: UITableViewDelegate, UITableViewDataSource {
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return chats.count;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = cellConfigurator?.getCell(model: chats[indexPath.row], indexPath: indexPath) ?? PSDChatInfoTableViewCell()
        return cell
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 80
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        interactor.doInteraction(.selectChat(index: indexPath.row))

    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: ChatsViewProtocol {
    func show(_ action: ChatsSearchViewCommand) {
        switch action {
        case .updateChats(let chats):
            self.chats = chats
        case .openChat(let chat):
            openChat(chat)
        case .deleteFilter:
            deleteFilter()
        case .setFilter(let userName):
            filterLabel.text = userName
            setFilter()
        case .updateMenus(let filterActions, let openNewActions, let menuVisible):
            if #available(iOS 14.0, *) {
                filterButton.menu = UIMenu(children: filterActions)
                filterButton.isHidden = !menuVisible
                plusView.menu = UIMenu(children: openNewActions)
                plusView.showsMenuAsPrimaryAction = menuVisible
            }
        case .endRefresh:
            customRefresh.endRefreshing()
            tableView.sendSubviewToBack(customRefresh)
        }
    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: PSDUpdateInfo {
    func startGettingInfo() {
        interactor.doInteraction(.reloadChats)
    }
    
    func refreshChat(showFakeMessage: Int?) { }
}
