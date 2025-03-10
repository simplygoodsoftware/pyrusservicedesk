import UIKit

@available(iOS 13.0, *)
class SearchViewController: UIViewController {
    private let interactor: SearchInteractorProtocol
    private let router: SearchRouterProtocol?
    
    let section = PSDChatsSectionModel()
        
    required init(interactor: SearchInteractorProtocol, router: SearchRouterProtocol) {
        self.interactor = interactor
        self.router = router
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private lazy var emptyChatsInfo = UIButton()

    private lazy var tableView: UITableView = {
        let table = UITableView()
        return table
    }()

    private lazy var activityIndicator = UIActivityIndicatorView()
    
    private var cellConfigurator: SearchChatCellConfigurator?
    private var diffabledDataSource: UITableViewDiffableDataSource<PSDChatsSectionModel, AnyHashable>?
    
    private lazy var customRefresh: UIRefreshControl = {
        let refreshControl = UIRefreshControl()
        refreshControl.transform = CGAffineTransformMakeScale(0.8, 0.8)
        refreshControl.isOpaque = false
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()
    
    private var clearTable = false
    private var chats: [[SearchChatViewModel]] = [[]] {
        didSet {
            reloadDiffable(animated: true)
            emptyChatsView.isHidden = chats[0].count > 0 || clearTable
        }
    }
    
    private var customization: ServiceDeskConfiguration?
    
    private lazy var emptyChatsView = UIView(frame: self.view.bounds)
    
    lazy var searchBar: UISearchBar = {
        let searchView = UISearchBar()
        searchView.placeholder = "PSDSearch".localizedPSD()
        searchView.backgroundColor = .clear
        searchView.backgroundImage = nil
        searchView.translatesAutoresizingMaskIntoConstraints = false
        searchView.barStyle = .default
        searchView.searchBarStyle = .minimal
        return searchView
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
        customization = PyrusServiceDesk.mainController?.customization
        design()
        interactor.doInteraction(.viewDidload)
        view.backgroundColor = .psdBackgroundColor
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if view.alpha > 0 {
            interactor.doInteraction(.viewWillAppear)
        }
    }
}

@available(iOS 13.0, *)
private extension SearchViewController {
    /**Setting design To PSDChatsViewController view, add subviews*/
    
    func design() {
        view.backgroundColor = UIColor.psdBackgroundColor
        view.addSubview(tableView)
        tableView.backgroundView = emptyChatsView
        view.addSubview(searchNavigationView)
        view.addSubview(searchBar)
        view.addSubview(activityIndicator)
        
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        activityIndicator.style = .large

        setupEmptyChats()
        setupTableView()
        setupSearchNavigationView()
    }
    
    func setupEmptyChats() {
        let chatsImage = UIImageView(image: UIImage.PSDImage(name: "search"))
        emptyChatsView.isHidden = true
        let mainColor = customization?.barButtonTintColor ?? .darkAppColor
       
        emptyChatsInfo.setTitle("SearchChats".localizedPSD(), for: .normal)
        emptyChatsInfo.titleLabel?.font = CustomizationHelper.systemFont(ofSize: 17)
        emptyChatsInfo.setTitleColor(.secondaryLabel, for: .normal)
        
        emptyChatsView.addSubview(chatsImage)
        emptyChatsView.addSubview(emptyChatsInfo)

        chatsImage.translatesAutoresizingMaskIntoConstraints = false
        emptyChatsInfo.translatesAutoresizingMaskIntoConstraints = false
      
        NSLayoutConstraint.activate([
            chatsImage.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            chatsImage.bottomAnchor.constraint(equalTo: emptyChatsInfo.topAnchor, constant: -8),
            emptyChatsInfo.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            emptyChatsInfo.centerYAnchor.constraint(equalTo: tableView.centerYAnchor, constant: -100),
        ])
    }
    
    func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: searchNavigationView.bottomAnchor),
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
        
//        tableView.addSubview(customRefresh)
        
        cellConfigurator = SearchChatCellConfigurator(tableView: tableView)
        setupDataSource()
    }
    
    @objc func refresh(sender: AnyObject) {
        interactor.doInteraction(.reloadChats)
    }
    
    func setupSearchNavigationView() {
        view.addSubview(searchNavigationView)
        searchNavigationView.leadingAnchor.constraint(equalTo: view.leadingAnchor,constant: -1).isActive = true
        searchNavigationView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        searchNavigationView.topAnchor.constraint(equalTo: view.topAnchor, constant: -1).isActive = true
        searchNavigationView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 56).isActive = true
        
        searchNavigationView.addSubview(searchBar)
        searchBar.translatesAutoresizingMaskIntoConstraints = false
        searchBar.tintColor = customization?.themeColor
        searchBar.delegate = self
//        searchBar.showsCancelButton = true
        searchBar.isUserInteractionEnabled = true
        
        let cancelButton = UIButton(type: .system)
        cancelButton.tintColor = customization?.themeColor
        cancelButton.setTitle("Cancel".localizedPSD(), for: .normal)
        cancelButton.translatesAutoresizingMaskIntoConstraints = false
        cancelButton.titleLabel?.font = .systemFont(ofSize: 17)
        searchNavigationView.addSubview(cancelButton)
        
        NSLayoutConstraint.activate([
            cancelButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            cancelButton.centerYAnchor.constraint(equalTo: searchBar.centerYAnchor),
            searchBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            searchBar.trailingAnchor.constraint(equalTo: cancelButton.leadingAnchor, constant: -2),
            searchBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 0)
        ])
        
        cancelButton.addTarget(self, action: #selector(cancelButtonTapped), for: .touchUpInside)
    }
    
    @objc func cancelButtonTapped() {
        searchBar.resignFirstResponder()
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        })
    }
    
    func openChat(_ chat: PSDChat, messageId: String) {
        let label = UILabel()
        label.isUserInteractionEnabled = true
        label.textAlignment = .center
        label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        label.text = chat.subject?.count ?? 0 > 0 ? chat.subject : "NewTicket".localizedPSD()
        
        customization?.setChatTitileView(label)
        router?.route(to: .chat(chat: chat, messageId: messageId))
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
        snapshot.appendSections([section])
        
        snapshot.appendItems(chats[0], toSection: section)
        
        diffabledDataSource.apply(snapshot, animatingDifferences: false)
        self.diffabledDataSource = diffabledDataSource
    }
}

@available(iOS 13.0, *)
extension SearchViewController: UITableViewDelegate, UITableViewDataSource {
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
        return 80
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        interactor.doInteraction(.selectChat(index: indexPath.row))
    }
}

@available(iOS 13.0, *)
extension SearchViewController: UISearchBarDelegate {
    func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        searchBar.resignFirstResponder()
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        })
    }
    
    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
        interactor.doInteraction(.search(text: searchText))
        emptyChatsInfo.setTitle(searchText.count > 0 ? "NoMatchesFound".localizedPSD() : "SearchChats".localizedPSD(), for: .normal)
    }
}

@available(iOS 13.0, *)
extension SearchViewController: SearchViewProtocol {
    func show(_ action: SearchViewCommand) {
        switch action {
        case .updateChats(let chats):
            clearTable = false
            self.chats = chats
        case .openChat(let chat, let messageId):
            UIView.animate(withDuration: 0.1, animations: {
                self.searchBar.resignFirstResponder()
            }) {_ in 
                self.openChat(chat, messageId: messageId)
            }
        case .endRefresh:
            customRefresh.endRefreshing()
            activityIndicator.stopAnimating()
            tableView.sendSubviewToBack(customRefresh)
        case .startRefresh:
            clearTable = true
            chats = [[]]
            activityIndicator.startAnimating()
        }
    }
}

@available(iOS 13.0, *)
extension SearchViewController: UINavigationControllerDelegate {
    func navigationController(_ navigationController: UINavigationController, willShow viewController: UIViewController, animated: Bool) {
        if viewController == self {
            self.navigationController?.setNavigationBarHidden(true, animated: animated)
        } else {
            self.navigationController?.setNavigationBarHidden(false, animated: animated)
        }
    }
}

