import UIKit

final class AnnouncementsViewController: UIViewController {

    // MARK: - Dependencies

    private let interactor: AnnouncementsInteractorProtocol

    // MARK: - State

    private var isVisible = true
    private var isRefreshing = false
    /// Лента закрыта модальным просмотрщиком вложений — это не уход с экрана:
    /// цикл «пометить прочитанным / перечитать» при возврате запускать не нужно.
    private var isCoveredByModal = false

    /// Айтемы, реально применённые к таблице. Диф для reconfigure считается
    /// против них, а не против oldValue — так корректно доезжают изменения,
    /// пришедшие пока экран был скрыт (apply при isVisible == false пропускается).
    private var appliedItems: [AnnouncementsViewModel] = []

    private var items: [AnnouncementsViewModel] = [] {
        didSet {
            applySnapshot(animated: true)
            updateEmptyState()
        }
    }

    /// Задачи прогрева миниатюр по indexPath (см. UITableViewDataSourcePrefetching).
    private var prefetchTasks: [IndexPath: Task<Void, Never>] = [:]

    // MARK: - UI

    private lazy var tableView = UITableView()

    private var navigationViewBottom: NSLayoutConstraint?
    private lazy var navigationView: UIView = {
        let view = UIView()
        view.backgroundColor = .navBarColor
        view.layer.borderColor = UIColor.black.withAlphaComponent(Layout.navBorderAlpha).cgColor
        view.layer.borderWidth = Layout.navBorderWidth
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private lazy var navTitle = UILabel()
    private lazy var icon = UIImageView()
    private lazy var activityIndicator = UIActivityIndicatorView()

    private var segmentControlHeight: NSLayoutConstraint?
    private lazy var segmentControl: UnderlineSegmentController = {
        let segment = UnderlineSegmentController(frame: .zero)
        segment.translatesAutoresizingMaskIntoConstraints = false
        return segment
    }()

    private var cellConfigurator: PSDAnnouncementsCellConfigurator?
    private var diffableDataSource: PSDAnnouncementsDiffableDataSource?

    private lazy var refreshControl: UIRefreshControl = {
        let refreshControl = UIRefreshControl()
        refreshControl.transform = CGAffineTransform(scaleX: Layout.refreshControlScale, y: Layout.refreshControlScale)
        refreshControl.isOpaque = false
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()

    private lazy var emptyAnnouncementsView = UIView(frame: view.bounds)

    // MARK: - Init

    required init(interactor: AnnouncementsInteractorProtocol) {
        self.interactor = interactor
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationController?.delegate = self
        activityIndicator.startAnimating()
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
        isVisible = true
        if isCoveredByModal {
            // Возврат из просмотрщика вложений: lastReadIds не трогаем,
            // иначе isRead ячеек меняется и вся видимая область перерисовывается.
            isCoveredByModal = false
        } else {
            interactor.doInteraction(.viewWillAppear)
        }
        applySnapshot(animated: false)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        // present(_:) выставляет presentedViewController до appearance-колбэков,
        // поэтому здесь надёжно различаем «накрыли модалкой» и «ушли с экрана».
        isCoveredByModal = presentedViewController != nil
        if !isCoveredByModal {
            interactor.doInteraction(.viewWillDisappear)
        }
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        isVisible = false
    }
}

// MARK: - Setup

private extension AnnouncementsViewController {

    enum Layout {
        static let navigationBarHeight: CGFloat = 56
        static let navigationBarHeightWithSegment: CGFloat = 84.5
        static let segmentControlTop: CGFloat = 44
        static let segmentControlHeight: CGFloat = 40
        static let titleTop: CGFloat = 16
        static let titleHeight: CGFloat = 28
        static let titleTrailingInset: CGFloat = 52
        static let iconCornerRadius: CGFloat = 12
        static let tableBottomInset: CGFloat = 20
        static let emptyViewImageSpacing: CGFloat = 16
        static let emptyViewTitleOffset: CGFloat = -100
        static let emptyViewSubtitleSpacing: CGFloat = 8
        static let emptyViewHorizontalInset: CGFloat = 16
        static let navBorderAlpha: CGFloat = 0.3
        static let navBorderWidth: CGFloat = 0.5
        static let refreshControlScale: CGFloat = 0.8
        static let estimatedAnnouncementRowHeight: CGFloat = 140
        static let readMarkerRowHeight: CGFloat = 78
        static let segmentAnimationDuration: TimeInterval = 0.3
        static let attachmentPresentFadeDuration: TimeInterval = 0.2
    }

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

        setupEmptyView()
        setupTableView()
        setupSegmentControl()
        setupNavigationView()
    }

    func setupEmptyView() {
        let emptyImage = UIImageView(image: UIImage.PSDImage(name: "noAnnouncements"))
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
        emptyAnnouncementsView.addSubview(emptyImage)
        emptyAnnouncementsView.addSubview(titleLabel)
        emptyAnnouncementsView.addSubview(subtitleLabel)

        emptyImage.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            emptyImage.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            emptyImage.bottomAnchor.constraint(equalTo: titleLabel.topAnchor, constant: -Layout.emptyViewImageSpacing),
            titleLabel.centerXAnchor.constraint(equalTo: tableView.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: tableView.centerYAnchor, constant: Layout.emptyViewTitleOffset),
            subtitleLabel.centerXAnchor.constraint(equalTo: titleLabel.centerXAnchor),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: Layout.emptyViewSubtitleSpacing),
            subtitleLabel.leadingAnchor.constraint(equalTo: tableView.leadingAnchor, constant: Layout.emptyViewHorizontalInset),
            subtitleLabel.trailingAnchor.constraint(equalTo: tableView.trailingAnchor, constant: -Layout.emptyViewHorizontalInset)
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
        tableView.prefetchDataSource = self
        tableView.backgroundColor = .psdDarkBackgroundColor
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = Layout.estimatedAnnouncementRowHeight
        tableView.separatorStyle = .none
        tableView.keyboardDismissMode = .onDrag
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: Layout.tableBottomInset, right: 0)
        tableView.contentInsetAdjustmentBehavior = .automatic

        tableView.addSubview(refreshControl)

        let configurator = PSDAnnouncementsCellConfigurator(tableView: tableView)
        cellConfigurator = configurator
        diffableDataSource = PSDAnnouncementsDiffableDataSource.createDataSource(
            for: tableView,
            cellConfigurator: configurator,
            attachmentsDelegate: self
        )
    }

    @objc func refresh(sender: AnyObject) {
        interactor.doInteraction(.reloadAnnouncements)
    }

    func setupSegmentControl() {
        segmentControl.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        segmentControl.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        segmentControl.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: Layout.segmentControlTop).isActive = true
        segmentControlHeight = segmentControl.heightAnchor.constraint(equalToConstant: 0)
        segmentControlHeight?.isActive = true
    }

    func setupNavigationView() {
        navigationView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: -1).isActive = true
        navigationView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        navigationView.topAnchor.constraint(equalTo: view.topAnchor, constant: -1).isActive = true

        navigationViewBottom = navigationView.bottomAnchor.constraint(
            equalTo: view.safeAreaLayoutGuide.topAnchor,
            constant: Layout.navigationBarHeight
        )
        navigationViewBottom?.isActive = true
    }

    func designNavigation() {
        let appearance = UINavigationBarAppearance(barAppearance: UIBarAppearance())
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = .navBarColor

        navigationItem.scrollEdgeAppearance = appearance
        navigationItem.standardAppearance = appearance

        setupNavTitle()
        segmentControl.delegate = self
    }

    func setupNavTitle() {
        let titleView = UIView()
        titleView.addSubview(navTitle)
        navigationView.addSubview(titleView)
        titleView.translatesAutoresizingMaskIntoConstraints = false
        navTitle.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            navTitle.leadingAnchor.constraint(equalTo: titleView.leadingAnchor),
            navTitle.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -Layout.titleTrailingInset),
            titleView.heightAnchor.constraint(equalToConstant: Layout.titleHeight),
            titleView.trailingAnchor.constraint(equalTo: navTitle.trailingAnchor),
            titleView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: Layout.titleTop),
            titleView.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])

        navTitle.font = CustomizationHelper.systemBoldFont(ofSize: 17)
        icon.layer.cornerRadius = Layout.iconCornerRadius
        icon.clipsToBounds = true
        icon.contentMode = .scaleAspectFill
    }

    // MARK: - Snapshot

    func applySnapshot(animated: Bool) {
        guard isVisible, let diffableDataSource else { return }

        var snapshot = NSDiffableDataSourceSnapshot<AnnouncementsSection, AnnouncementsViewModel>()
        snapshot.appendSections([.main])
        snapshot.appendItems(items, toSection: .main)

        // Идентичность айтемов — по id: изменившийся контент (isRead, правки)
        // доставляем через reconfigure, а не через delete+insert с миганием.
        let appliedById = Dictionary(
            appliedItems.map { ($0.itemIdentity, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        let changedItems = items.filter { item in
            guard let applied = appliedById[item.itemIdentity] else { return false }
            return !applied.hasSameContent(as: item)
        }

        // Ничего не изменилось (типично при viewWillAppear после возврата
        // из модалки) — не дёргаем UIKit вообще: даже «пустой» apply
        // на старых iOS эквивалентен reloadData.
        let identityUnchanged = items.count == appliedItems.count
            && zip(items, appliedItems).allSatisfy { $0.itemIdentity == $1.itemIdentity }
        if identityUnchanged && changedItems.isEmpty {
            return
        }

        if !changedItems.isEmpty {
            if #available(iOS 15.0, *) {
                snapshot.reconfigureItems(changedItems)
            } else {
                snapshot.reloadItems(changedItems)
            }
        }

        diffableDataSource.apply(snapshot, animatingDifferences: animated)
        appliedItems = items
    }

    func updateEmptyState() {
        emptyAnnouncementsView.isHidden = !items.isEmpty || isRefreshing
    }
}

// MARK: - UITableViewDataSourcePrefetching

extension AnnouncementsViewController: UITableViewDataSourcePrefetching {

    /// Греет миниатюры медиа-вложений рядов, к которым приближается скролл:
    /// загрузка и декод происходят до появления ряда на экране, ячейка
    /// подставляет готовую миниатюру из кеша синхронно. Это разгружает
    /// кадры прокрутки — основная причина «заеданий» при появлении картинок.
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        let scale = max(view.traitCollection.displayScale, 1)

        for indexPath in indexPaths {
            guard
                prefetchTasks[indexPath] == nil,
                let item = diffableDataSource?.itemIdentifier(for: indexPath),
                let model = item.data as? PSDAnnouncementCellModel
            else { continue }

            let mediaAttachments = model.announcement.attachments.filter { $0.media }
            guard !mediaAttachments.isEmpty else { continue }

            prefetchTasks[indexPath] = Task { [weak self] in
                await AnnouncementThumbnailPrefetcher.warm(attachments: mediaAttachments, scale: scale)
                self?.prefetchTasks[indexPath] = nil
            }
        }
    }

    func tableView(_ tableView: UITableView, cancelPrefetchingForRowsAt indexPaths: [IndexPath]) {
        for indexPath in indexPaths {
            prefetchTasks[indexPath]?.cancel()
            prefetchTasks[indexPath] = nil
        }
    }
}

// MARK: - UITableViewDelegate

extension AnnouncementsViewController: UITableViewDelegate {

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        switch diffableDataSource?.itemIdentifier(for: indexPath)?.type {
        case .announcementsRead:
            return Layout.readMarkerRowHeight
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        switch diffableDataSource?.itemIdentifier(for: indexPath)?.type {
        case .announcementsRead:
            return Layout.readMarkerRowHeight
        default:
            return Layout.estimatedAnnouncementRowHeight
        }
    }
}

// MARK: - AnnouncementsViewProtocol

extension AnnouncementsViewController: AnnouncementsViewProtocol {
    func show(_ action: AnnouncementsViewCommand) {
        switch action {
        case .updateAnnouncements(let announcements):
            isRefreshing = false
            items = announcements
        case .endRefresh:
            refreshControl.endRefreshing()
            activityIndicator.stopAnimating()
            tableView.sendSubviewToBack(refreshControl)
        case .updateTitle(let title):
            navTitle.text = title
        case .updateTitles(let titles, let selectedIndex):
            UIView.animate(withDuration: Layout.segmentAnimationDuration) {
                self.segmentControlHeight?.constant = Layout.segmentControlHeight
                self.navigationViewBottom?.constant = Layout.navigationBarHeightWithSegment
                self.view.layoutIfNeeded()
                self.segmentControl.updateTitle(titles: titles, selectIndex: selectedIndex)
            }
        case .updateSelected(let index):
            UIView.performWithoutAnimation {
                segmentControl.selectIndex(index)
            }
        case .updateIcon(let image):
            icon.image = image
        case .deleteSegmentControl:
            segmentControlHeight?.constant = 0
            navigationViewBottom?.constant = Layout.navigationBarHeight
            view.layoutIfNeeded()
            segmentControl.updateTitle(titles: [], selectIndex: 0)
        case .startRefresh:
            isRefreshing = true
            items = []
            activityIndicator.startAnimating()
        case .connectionError:
            navTitle.text = "Waiting_For_Network".localizedPSD()
            if icon.image == nil {
                icon.image = UIImage(named: "iiko")
            }
            refreshControl.endRefreshing()
            tableView.sendSubviewToBack(refreshControl)
        }
    }
}

// MARK: - UnderlineSegmentControllerDelegate

extension AnnouncementsViewController: UnderlineSegmentControllerDelegate {
    func didSelectSegment(_ index: Int) {
        interactor.doInteraction(.updateSelected(index: index))
    }
}

// MARK: - UINavigationControllerDelegate

extension AnnouncementsViewController: UINavigationControllerDelegate {
    func navigationController(_ navigationController: UINavigationController, willShow viewController: UIViewController, animated: Bool) {
        navigationController.setNavigationBarHidden(viewController == self, animated: animated)
    }
}

// MARK: - UITabBarControllerDelegate

extension AnnouncementsViewController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        let isCurrentTab = viewController === self || viewController === navigationController
        guard isCurrentTab, !items.isEmpty else { return }
        tableView.scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: true)
    }
}

// MARK: - AnnouncementsAttachmentsDelegate

extension AnnouncementsViewController: AnnouncementsAttachmentsDelegate {
    func selectAttachment(cell: UITableViewCell, index: Int) {
        guard
            let indexPath = tableView.indexPath(for: cell),
            let item = diffableDataSource?.itemIdentifier(for: indexPath),
            let announcement = (item.data as? PSDAnnouncementCellModel)?.announcement,
            announcement.attachments.indices.contains(index)
        else { return }

        let selectedAttachment = announcement.attachments[index]
        let isMedia = selectedAttachment.media //|| selectedAttachment.isVideo

        let attachments: [PSDAnnouncementAttachment]
        if isMedia {
            // Порядок листания: сначала медиа в том же порядке, что и в сетке,
            // затем — видео из списка файлов (media == false), в своём порядке.
            let gridMedia = announcement.attachments.filter { $0.media }
//            let fileVideos = announcement.attachments.filter { !$0.media && $0.isVideo }
            attachments = gridMedia //+ fileVideos
        } else {
            attachments = announcement.attachments.filter { !$0.media }
        }

        let initialIndex = attachments.firstIndex(of: selectedAttachment) ?? 0
        let attachmentsController = PSDAnnouncementsAttachmentViewController(attachments: attachments, initialIndex: initialIndex)
        let navController = PSDNavigationController(rootViewController: attachmentsController)

        if isMedia {
            navController.modalPresentationStyle = .fullScreen
            navController.view.alpha = 0
            present(navController, animated: false) {
                UIView.animate(withDuration: Layout.attachmentPresentFadeDuration) {
                    navController.view.alpha = 1
                }
            }
        } else {
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
