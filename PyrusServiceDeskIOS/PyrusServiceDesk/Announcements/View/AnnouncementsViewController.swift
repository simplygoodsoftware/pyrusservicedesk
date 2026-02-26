import UIKit

class AnnouncementsViewController: UIViewController {
    private let interactor: AnnouncementsInteractorProtocol
    private var isVisible: Bool = true

    let section1 = AnnouncementsSectionModel()
    
    private var isClosedTicketsOpened: Bool = false
    
    required init(interactor: AnnouncementsInteractorProtocol) {
        self.interactor = interactor
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
    
    private var cellConfigurator: PSDAnnouncementsCellConfigurator?
    private var diffabledDataSource: UITableViewDiffableDataSource<AnnouncementsSectionModel, AnnouncementsViewModel>?
    
    private lazy var customRefresh: UIRefreshControl = {
        let refreshControl = UIRefreshControl()
        refreshControl.transform = CGAffineTransformMakeScale(0.8, 0.8)
        refreshControl.isOpaque = false
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()
    
    private var clearTable = false
    private var announcements: [[AnnouncementsViewModel]] = [[]] {
        didSet {
            reloadDiffable(animated: true)
            emptyAnnouncementsView.isHidden = !(announcements[0].count == 0) || clearTable
        }
    }
    
    private var timer: Timer?
    private var customization: ServiceDeskConfiguration?
    
    private lazy var emptyAnnouncementsView = UIView(frame: self.view.bounds)
    
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
        interactor.doInteraction(.viewDidload)
        view.backgroundColor = .psdDarkBackgroundColor
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tabBarController?.delegate = self
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        interactor.doInteraction(.viewWillAppear)
        isVisible = true
        reloadDiffable(animated: false)
        self.navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        interactor.doInteraction(.viewWillDisappear)
        self.navigationController?.setNavigationBarHidden(false, animated: animated)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        isVisible = false
    }
}

private extension AnnouncementsViewController {
    /**Setting design To PSDChatsViewController view, add subviews*/
    
    func design() {
        view.backgroundColor = .psdDarkBackgroundColor
        view.addSubview(tableView)
        tableView.backgroundView = emptyAnnouncementsView

        view.addSubview(navigationView)
        view.addSubview(segmentControl)
        view.addSubview(activityIndicator)
        
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        activityIndicator.style = .large

        setupEmptyChats()
        setupTableView()
        setupSegmentControl()
        setupNavigationView()
    }
    
    func setupEmptyChats() {
        let chatsImage = UIImageView(image: UIImage.PSDImage(name: "noAnnouncements"))
        let titleLabel = UILabel()
        let subtitleLabel = UILabel()
        
        titleLabel.font = .boldSystemFont(ofSize: 22)
        titleLabel.textColor = .label
        titleLabel.text = "NoAnnouncements".localizedPSD()
        
        subtitleLabel.font = .systemFont(ofSize: 18, weight: .regular)
        subtitleLabel.textColor = .secondaryLabel
        subtitleLabel.text = "NoAnnouncementsDescr".localizedPSD()
        subtitleLabel.numberOfLines = 0
        subtitleLabel.textAlignment = .center
        
        emptyAnnouncementsView.isHidden = true
        emptyAnnouncementsView.addSubview(chatsImage)
        emptyAnnouncementsView.addSubview(titleLabel)
        emptyAnnouncementsView.addSubview(subtitleLabel)

        chatsImage.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
      
        NSLayoutConstraint.activate([
            chatsImage.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            chatsImage.bottomAnchor.constraint(equalTo: titleLabel.topAnchor, constant: -16),
            titleLabel.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: tableView.centerYAnchor, constant: -100),
            subtitleLabel.centerXAnchor.constraint(equalTo: titleLabel.centerXAnchor),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8),
            subtitleLabel.leadingAnchor.constraint(equalTo: tableView.leadingAnchor, constant: 16),
            subtitleLabel.trailingAnchor.constraint(equalTo: tableView.trailingAnchor, constant: -16)
        ])
        
    }
    
    func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: navigationView.bottomAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        tableView.delegate = self
        tableView.backgroundColor = .psdDarkBackgroundColor
        tableView.estimatedRowHeight = UITableView.automaticDimension
        tableView.rowHeight = UITableView.automaticDimension
        tableView.separatorStyle = .none
        tableView.keyboardDismissMode = .onDrag
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 20, right: 0)
        tableView.contentInsetAdjustmentBehavior = .automatic
        
        tableView.addSubview(customRefresh)
        
        cellConfigurator = PSDAnnouncementsCellConfigurator(tableView: tableView)
        setupDataSource()
    }
    
    @objc func refresh(sender: AnyObject) {
        interactor.doInteraction(.reloadAnnouncements)
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
    }

    //*Setting design to navigation bar, title and buttons*/
    func designNavigation() {
        let bigAppear = UINavigationBarAppearance(barAppearance: UIBarAppearance())
        bigAppear.configureWithOpaqueBackground()
        bigAppear.backgroundColor = .navBarColor
        
        navigationItem.scrollEdgeAppearance = bigAppear
        navigationItem.standardAppearance = bigAppear
        
        setupNavTitle()
        segmentControl.delegate = self
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

    func setupDataSource() {
        let newDataSource = PSDAnnouncementsDiffableDataSource.createDataSource(for: tableView, cellCreator: self)
        tableView.dataSource = newDataSource
        diffabledDataSource = newDataSource
    }

    func reloadDiffable(animated: Bool) {
        guard isVisible else {
            return
        }
        guard let diffabledDataSource = diffabledDataSource else { return }
        var snapshot = NSDiffableDataSourceSnapshot<AnnouncementsSectionModel, AnnouncementsViewModel>()
        snapshot.deleteAllItems()
        snapshot.appendSections([section1])
        snapshot.appendItems(announcements[0], toSection: section1)
        diffabledDataSource.apply(snapshot, animatingDifferences: animated)
        self.diffabledDataSource = diffabledDataSource
    }
    
    func showAccessDeniedAlert(userNames: String, okAction: UIAlertAction) {
        let alert = UIAlertController(title: "AcсessDenied".localizedPSD(), message: String(format: "AccessDeniedInfo".localizedPSD(), userNames), preferredStyle: .alert)
        alert.addAction(okAction)
        present(alert, animated: true)
    }
}

@available(iOS 13.0, *)
extension AnnouncementsViewController: UITableViewDelegate, UITableViewDataSource {
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return announcements.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return announcements[section].count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = cellConfigurator?.getCell(model: announcements[indexPath.section][indexPath.row], indexPath: indexPath, delegate: self) ?? PSDAnnouncementCell()
        cell.isHidden = false
//        cell.selectionStyle = .none
        return cell
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        switch announcements[indexPath.section][indexPath.row].type {
        case .announcement:
            return UITableView.automaticDimension
        case .announcementsRead:
            return 78
        }
    }
}

extension AnnouncementsViewController: AnnouncementsViewProtocol {
    func show(_ action: AnnouncementsViewCommand) {
        switch action {
        case .updateAnnouncements(let announcements):
            clearTable = false
            self.announcements = announcements
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
                self.view.layoutIfNeeded()
                self.segmentControl.updateTitle(titles: titles, selectIndex: selectedIndex)
            })
        case .updateSelected(index: let index):
            UIView.performWithoutAnimation {
                segmentControl.selectIndex(index)
            }
        case .updateIcon(image: let image):
            icon.image = image
        case .deleteSegmentControl:
                self.heightSegmentControl?.constant = 0
                self.bottomNavigationView?.constant = 56
                self.view.layoutIfNeeded()
                self.segmentControl.updateTitle(titles: [], selectIndex: 0)
        case .startRefresh:
            clearTable = true
            announcements = [[]]
            activityIndicator.startAnimating()
        case .connectionError:
            navTitle.text = "Waiting_For_Network".localizedPSD()
            if icon.image == nil {
                icon.image = UIImage(named: "iiko")
            }
            customRefresh.endRefreshing()
            tableView.sendSubviewToBack(customRefresh)
        }
    }
}

extension AnnouncementsViewController: UnderlineSegmentControllerDelegate {
    func didSelectSegment(_ index: Int) {
        interactor.doInteraction(.updateSelected(index: index))
    }
}

extension AnnouncementsViewController: UINavigationControllerDelegate {
    func navigationController(_ navigationController: UINavigationController, willShow viewController: UIViewController, animated: Bool) {
        if viewController == self {
            self.navigationController?.setNavigationBarHidden(true, animated: animated)
        } else {
            self.navigationController?.setNavigationBarHidden(false, animated: animated)
        }
    }
}

extension AnnouncementsViewController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        if tabBarController.selectedIndex == 2 && (self.announcements[0].count > 0) {
            tableView.scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: true)
        }
    }
}

extension AnnouncementsViewController: AnnouncementsAttachmentsDelegate {
    func selectAttachment(cell: UITableViewCell, index: Int) {
        
        guard let indexPath = tableView.indexPath(for: cell),
        let announcement = announcements[indexPath.section][indexPath.row].data as? PSDAnnouncement,
            announcement.attachments.count > 0
        else { return }
        let selectedAttachment = announcement.attachments[index]
        if selectedAttachment.media || selectedAttachment.isVideo {
            let attachments = announcement.attachments.filter({ $0.media || $0.isVideo }).sorted { a, b in
                // сначала те, у кого isVideo == false
                (!a.isVideo && b.isVideo)
            }
            let initialIndex = attachments.firstIndex(of: selectedAttachment) ?? 0
            let vc = PSDAnnouncementsAttachmentViewController(attachments: attachments, initialIndex: initialIndex)
            let navController = PSDNavigationController(rootViewController: vc)
            navController.modalPresentationStyle = .fullScreen
            navController.view.alpha = 0
            present(navController, animated: false) {
                UIView.animate(withDuration: 0.2) {
                    navController.view.alpha = 1
                }
            }
        } else {
            let attachments = announcement.attachments.filter({ !$0.media && !$0.isVideo })
            let initialIndex = attachments.firstIndex(of: selectedAttachment) ?? 0
            let vc = PSDAnnouncementsAttachmentViewController(attachments: attachments, initialIndex: initialIndex)
            let navController = PSDNavigationController(rootViewController: vc)
            present(navController, animated: true)
        }
    }
    
}

extension UIColor {
    static let psdDarkBackgroundColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "1C1C1E") ?? .black
        default:
            return .white
        }
    }
}
