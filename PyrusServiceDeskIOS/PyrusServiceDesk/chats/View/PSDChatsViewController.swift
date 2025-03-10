import UIKit

@available(iOS 13.0, *)
class PSDChatsViewController: UIViewController {
    private let interactor: ChatsInteractorProtocol
    private let router: ChatsRouterProtocol?
    
    let section1 = PSDChatsSectionModel()
    let section2 = PSDChatsSectionModel()
    
    private var isClosedTicketsOpened: Bool = false
    
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
    
    private var bottomNavigationView: NSLayoutConstraint?
    
    private lazy var navigationView: UIView = {
        let view = UIView()
        view.backgroundColor = .navBarColor
        view.layer.borderColor = UIColor.black.withAlphaComponent(0.3).cgColor
        view.layer.borderWidth = 0.5
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var navTitle = UILabel()
    private lazy var icon = UIImageView()
    private lazy var activityIndicator = UIActivityIndicatorView()

    private var heightSegmentControl: NSLayoutConstraint?
    
    private lazy var segmentControl: UnderlineSegmentController = {
        let segment = UnderlineSegmentController(frame: .zero)
        segment.translatesAutoresizingMaskIntoConstraints = false
        return segment
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
    
    private var clearTable = false
    private var chats: [[PSDChatsViewModel]] = [[], []] {
        didSet {
            reloadDiffable(animated: true)
            emptyChatsView.isHidden = !(chats[0].count == 0 && chats[1].count == 0) || clearTable
        }
    }
    
    private var isFiltered = false {
        didSet {
            let image = isFiltered ? UIImage.PSDImage(name: "fillFilter") : UIImage.PSDImage(name: "filter")
            filterImage.image = image?.imageWith(color: customization?.themeColor ?? .darkAppColor)
        }
    }
    
    private var timer: Timer?
    private var customization: ServiceDeskConfiguration?
    
    private var originYFilterConstraint: NSLayoutConstraint?
    private var heightFilterConstraint: NSLayoutConstraint?
    
    private var searchViewController: SearchViewController?

    private lazy var filterInfoView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = customization?.themeColor
        return view
    }()
    
    private lazy var filterCross: UIButton = {
        let button = UIButton()
        let crossImage = UIImage.PSDImage(name: "Close")?.imageWith(color: .white)
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
    
    
    // не забыть добавить Child'a (контроллер)
    private lazy var searchBar: UISearchBar = {
        let searchView = UISearchBar()
        searchView.placeholder = "PSDSearch".localizedPSD()
        searchView.backgroundColor = .navBarColor
        searchView.backgroundImage = nil
         searchView.translatesAutoresizingMaskIntoConstraints = false
        searchView.barStyle = .default
        searchView.searchBarStyle = .minimal
         searchView.showsCancelButton = true
         return searchView
     }()
    
    private lazy var searchButton: UIButton = {
        let button = UIButton(type: .system)
        //button.setImage(UIImage(systemName: "magnifyingglass"), for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var searchNavigationView: UIView = {
        let view = UIView()
        view.backgroundColor = .navBarColor
        view.layer.borderColor = UIColor.black.withAlphaComponent(0.3).cgColor
        view.layer.borderWidth = 0.5
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationController?.delegate = self
        activityIndicator.startAnimating()
        customization = PyrusServiceDesk.mainController?.customization
        design()
        designNavigation()
        startGettingInfo()
        interactor.doInteraction(.viewDidload)
        view.backgroundColor = .psdBackgroundColor
        tabBarController?.delegate = self
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tabBarController?.delegate = self
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        interactor.doInteraction(.viewWillAppear)
//        hidesBottomBarWhenPushed = false
//        tabBarController?.tabBar.isHidden = false
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
//        hidesBottomBarWhenPushed = true
    }
}

@available(iOS 13.0, *)
private extension PSDChatsViewController {
    /**Setting design To PSDChatsViewController view, add subviews*/
    
    func design() {
        view.backgroundColor = UIColor.psdBackgroundColor
        view.addSubview(tableView)
        tableView.backgroundView = emptyChatsView

        view.addSubview(navigationView)
        view.addSubview(searchBar)
        view.addSubview(segmentControl)
        view.addSubview(filterInfoView)
        view.addSubview(plusView)
        view.addSubview(activityIndicator)
        
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        activityIndicator.style = .large

        setupEmptyChats()
        setupTableView()
        setupFilterInfoView()
        setupSegmentControl()
        setupNavigationView()
        setupPlusView()
        setupSearchNavigationView()
    }
    
    func setupEmptyChats() {
        let chatsImage = UIImageView(image: UIImage.PSDImage(name: "chats"))
        let openNewButton = UIButton(type: .system)
        emptyChatsView.isHidden = true
        let mainColor = customization?.barButtonTintColor ?? .darkAppColor
       
        openNewButton.setTitle("CreatTicket".localizedPSD(), for: .normal)
        openNewButton.titleLabel?.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        openNewButton.setTitleColor(mainColor, for: .normal)
        
        emptyChatsView.addSubview(chatsImage)
        emptyChatsView.addSubview(openNewButton)

        chatsImage.translatesAutoresizingMaskIntoConstraints = false
        openNewButton.translatesAutoresizingMaskIntoConstraints = false
      
        NSLayoutConstraint.activate([
            chatsImage.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            chatsImage.bottomAnchor.constraint(equalTo: openNewButton.topAnchor, constant: -16),
            openNewButton.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            openNewButton.centerYAnchor.constraint(equalTo: tableView.centerYAnchor, constant: -100),
        ])
        
        openNewButton.isUserInteractionEnabled = true
        openNewButton.addTarget(self, action: #selector(openNewChat), for: .touchUpInside)
    }
    
    func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: navigationView.bottomAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        tableView.delegate = self
        tableView.backgroundColor = .psdBackgroundColor
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
            filterLabel.trailingAnchor.constraint(lessThanOrEqualTo: filterInfoView.trailingAnchor, constant: -60),
            
            filterCross.heightAnchor.constraint(lessThanOrEqualToConstant: 40),
            filterLabel.heightAnchor.constraint(lessThanOrEqualToConstant: 40)
        ])
        
        filterCross.addTarget(self, action: #selector(deleteFilter), for: .touchUpInside)
        filterInfoView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        filterInfoView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        originYFilterConstraint = filterInfoView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 56)
        originYFilterConstraint?.isActive = true
        
        heightFilterConstraint = filterInfoView.heightAnchor.constraint(equalToConstant: 0)
        heightFilterConstraint?.isActive = true
    }
        
    func setFilter() {
        isFiltered = true
        UIView.animate(withDuration: 0.2, animations: {
            self.tableView.contentInset.top = 50
            self.heightFilterConstraint?.constant = 50
            self.view.layoutIfNeeded()
            self.tableView.layoutIfNeeded()
            self.filterLabel.isHidden = false
            self.filterCross.isHidden = false
            if self.chats[0].count > 0 || self.chats[1].count > 0 {
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
            self.heightFilterConstraint?.constant = 0
            self.view.layoutIfNeeded()
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
            plusView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16)
        ])
        plusView.isUserInteractionEnabled = true
        plusView.addTarget(self, action: #selector(openNewChat), for: .touchUpInside)
    }
    
    func setupSegmentControl() {
        segmentControl.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        segmentControl.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        segmentControl.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 44).isActive = true
        heightSegmentControl =  segmentControl.heightAnchor.constraint(equalToConstant: 0)
        heightSegmentControl?.constant = 0
        heightSegmentControl?.isActive = true
    }
    
    func setupNavigationView() {
        navigationView.leadingAnchor.constraint(equalTo: view.leadingAnchor,constant: -1).isActive = true
        navigationView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        navigationView.topAnchor.constraint(equalTo: view.topAnchor, constant: -1).isActive = true
        
        bottomNavigationView = navigationView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 56)
        bottomNavigationView?.isActive = true
        
        navigationView.addSubview(searchButton)
        searchButton.tintColor = customization?.themeColor
        searchButton.setImage(UIImage.PSDImage(name: "search")?.withTintColor(customization?.themeColor ?? .blue), for: .normal)

        NSLayoutConstraint.activate([
            searchButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            searchButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 13),
            searchButton.heightAnchor.constraint(equalToConstant: 32),
            searchButton.widthAnchor.constraint(equalToConstant: 32)
        ])
        searchButton.addTarget(self, action: #selector(openSearch), for: .touchUpInside)
        
        let presenter = SearchPresenter()
        let interactor = SearchInteractor(presenter: presenter)
        let router = SearchRouter()
        let controller = SearchViewController(interactor: interactor, router: router)
        presenter.view = controller
        router.controller = controller
        controller.view.alpha = 0.0
        self.view.addSubview(controller.view)
        searchViewController = controller
        addChild(controller)
    }
    
    @objc func openSearch() {
        UIView.animate(withDuration: 0.3, animations: {
            self.searchViewController?.view.alpha = 1.0
            self.searchViewController?.searchBar.becomeFirstResponder()
        })
    }
    
    func setupSearchNavigationView() {
        view.addSubview(searchNavigationView)
        searchNavigationView.leadingAnchor.constraint(equalTo: view.leadingAnchor,constant: -1).isActive = true
        searchNavigationView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        searchNavigationView.topAnchor.constraint(equalTo: view.topAnchor, constant: -1).isActive = true
        searchNavigationView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 60).isActive = true
        
        searchNavigationView.addSubview(searchBar)
        searchBar.translatesAutoresizingMaskIntoConstraints = false
        searchBar.tintColor = customization?.themeColor
        NSLayoutConstraint.activate([
            searchBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            searchBar.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -8),
            searchBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 0)
        ])
        searchNavigationView.alpha = 0.0
    }
    
    @objc func openNewChat() {
        interactor.doInteraction(.newChat)
    }

    //*Setting design to navigation bar, title and buttons*/
    func designNavigation() {
        let bigAppear = UINavigationBarAppearance(barAppearance: UIBarAppearance())
        bigAppear.configureWithOpaqueBackground()
        bigAppear.backgroundColor = .navBarColor
        
        navigationItem.scrollEdgeAppearance = bigAppear
        navigationItem.standardAppearance = bigAppear
        
//        if let button = customization?.chatsRightBarButtonItem {
//            navigationView.addSubview(button)
//            button.isUserInteractionEnabled = true
//            button.translatesAutoresizingMaskIntoConstraints = false
//            NSLayoutConstraint.activate([
//                button.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
//                button.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 14),
//                button.heightAnchor.constraint(equalToConstant: 28),
//                button.widthAnchor.constraint(equalToConstant: 28)
//            ])
//        }
        
        setupNavTitle()
        setupFilterButton()
        segmentControl.delegate = self
    }
    
    func setupFilterButton() {
        let mainColor = customization?.themeColor ?? .darkAppColor
        filterImage = UIImageView(image: UIImage.PSDImage(name: "filter")?.imageWith(color: mainColor))
        filterButton.addSubview(filterImage)
        navigationView.addSubview(filterButton)
        filterImage.translatesAutoresizingMaskIntoConstraints = false
        filterButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            filterButton.heightAnchor.constraint(equalToConstant: 28),
            filterButton.widthAnchor.constraint(equalToConstant: 28),
            filterImage.heightAnchor.constraint(equalToConstant: 28),
            filterImage.widthAnchor.constraint(equalToConstant: 28),
            filterImage.centerXAnchor.constraint(equalTo: filterButton.centerXAnchor),
            filterImage.centerYAnchor.constraint(equalTo: filterButton.centerYAnchor),
        ])
        filterImage.sizeToFit()
        filterButton.isHidden = true
        if #available(iOS 14.0, *) {
            filterButton.showsMenuAsPrimaryAction = true
        }
        
        NSLayoutConstraint.activate([
            filterButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 15),
            filterButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16)
        ])
    }
    
    func setupNavTitle() {
        let titleView = UIView()
        titleView.addSubview(navTitle)
        titleView.addSubview(icon)
        navigationView.addSubview(titleView)
        titleView.translatesAutoresizingMaskIntoConstraints = false
        navTitle.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: titleView.leadingAnchor),
            icon.heightAnchor.constraint(equalToConstant: 24),
            icon.widthAnchor.constraint(equalToConstant: 24),
            navTitle.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 6),
            navTitle.centerYAnchor.constraint(equalTo: icon.centerYAnchor),
            //navTitle.widthAnchor.constraint(lessThanOrEqualToConstant: 200),
            navTitle.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -52),
            titleView.heightAnchor.constraint(equalToConstant: 28),
            titleView.trailingAnchor.constraint(equalTo: navTitle.trailingAnchor),
            titleView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            titleView.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
        
        navTitle.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        icon.layer.cornerRadius = 12
        icon.clipsToBounds = true
        icon.contentMode = .scaleAspectFill
    }
    
    @objc func titleViewTapped() {
        if let action = customization?.titleHandler {
            action()
        }
    }
    
    func openChat(_ chat: PSDChat, fromPush: Bool) {
        let label = UILabel()
        label.isUserInteractionEnabled = true
        label.textAlignment = .center
        label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        label.text = chat.subject?.count ?? 0 > 0 ? chat.subject : "NewTicket".localizedPSD()
        
        customization?.setChatTitileView(label)
        router?.route(to: .chat(chat: chat, fromPush: fromPush))
    }

    func setupDataSource() {
        let newDataSource = PSDChatsDiffableDataSource.createDataSource(for: tableView, cellCreator: self)
        tableView.dataSource = newDataSource
        diffabledDataSource = newDataSource
    }

    func reloadDiffable(animated: Bool) {
        guard let diffabledDataSource = diffabledDataSource else { return }
        var snapshot = NSDiffableDataSourceSnapshot<PSDChatsSectionModel, AnyHashable>()
        snapshot.deleteAllItems()
        snapshot.appendSections([section1, section2])
        
        snapshot.appendItems(chats[0], toSection: section1)
        snapshot.appendItems(chats[1], toSection: section2)
        
        diffabledDataSource.apply(snapshot, animatingDifferences: animated)
        self.diffabledDataSource = diffabledDataSource
    }
    
    func showAccessDeniedAlert(userNames: String, okAction: UIAlertAction) {
        let alert = UIAlertController(title: "AcсessDenied".localizedPSD(), message: String(format: "AccessDeniedInfo".localizedPSD(), userNames), preferredStyle: .alert)
        alert.addAction(okAction)
        present(alert, animated: true)
    }
    
    func scrollToClosedTickets() {
        let indexPath = IndexPath(row: chats[0].count - 1, section: 0)
        tableView.scrollToRow(at: indexPath, at: .top, animated: true)
    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: UITableViewDelegate, UITableViewDataSource {
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return chats.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return chats[section].count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = cellConfigurator?.getCell(model: chats[indexPath.section][indexPath.row], indexPath: indexPath) ?? PSDChatInfoTableViewCell()
        return cell
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        switch chats[indexPath.section][indexPath.row].type {
        case .chat:
            return 80
        case .header:
            return 48
        }
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if chats[indexPath.section][indexPath.row].type == .chat {
            tableView.deselectRow(at: indexPath, animated: true)
            let index = indexPath.section == 0 ? indexPath.row : indexPath.row + chats[0].count - 1
            interactor.doInteraction(.selectChat(index: index))
        }

    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: ChatsViewProtocol {
    func show(_ action: ChatsSearchViewCommand) {
        switch action {
        case .updateChats(let chats):
            clearTable = false
            self.chats = chats
        case .openChat(let chat, let fromPush):
            openChat(chat, fromPush: fromPush)
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
                plusView.showsMenuAsPrimaryAction = menuVisible && !isFiltered
            }
        case .endRefresh:
            customRefresh.endRefreshing()
            activityIndicator.stopAnimating()
            tableView.sendSubviewToBack(customRefresh)
        case .updateTitle(title: let title):
            navTitle.text = title
        case .updateTitles(titles: let titles, selectedIndex: let selectedIndex):
            UIView.animate(withDuration: 0.3, animations: {
                self.heightSegmentControl?.constant = 40
                self.bottomNavigationView?.constant = 84.5
                self.originYFilterConstraint?.constant = 84.5
                self.view.layoutIfNeeded()
                self.segmentControl.updateTitle(titles: titles, selectIndex: selectedIndex)
            })
        case .updateSelected(index: let index):
            segmentControl.selectIndex(index)
        case .updateIcon(image: let image):
            icon.image = image
        case .showAccessDeniedAlert(userNames: let userNames, okAction: let okAction):
            showAccessDeniedAlert(userNames: userNames, okAction: okAction)
        case .deleteSegmentControl:
                self.originYFilterConstraint?.constant = 56//43
               // self.heightFilterConstraint?.constant = 0
                self.heightSegmentControl?.constant = 0
                self.bottomNavigationView?.constant = 56
                self.view.layoutIfNeeded()
                self.segmentControl.updateTitle(titles: [], selectIndex: 0)
        case .startRefresh:
            clearTable = true
            chats = [[], []]
            activityIndicator.startAnimating()
        case .connectionError:
            navTitle.text = "Waiting_For_Network".localizedPSD()
            if icon.image == nil {
                icon.image = UIImage(named: "iiko")
            }
            customRefresh.endRefreshing()
            tableView.sendSubviewToBack(customRefresh)
        case .scrollToClosedTickets:
            scrollToClosedTickets()
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

@available(iOS 13.0, *)
extension PSDChatsViewController: UnderlineSegmentControllerDelegate {
    func didSelectSegment(_ index: Int) { 
        interactor.doInteraction(.updateSelected(index: index))
    }
}

@available(iOS 13.0, *)
extension PSDChatsViewController: UINavigationControllerDelegate {
    func navigationController(_ navigationController: UINavigationController, willShow viewController: UIViewController, animated: Bool) {
        if viewController == self {
            self.navigationController?.setNavigationBarHidden(true, animated: animated)
        } else {
            self.navigationController?.setNavigationBarHidden(false, animated: animated)
        }
    }
}

extension PSDChatsViewController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        if tabBarController.selectedIndex == 1 && self.chats[0].count > 0 || self.chats[1].count > 0 {
            tableView.scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: true)
        }
    }
}
