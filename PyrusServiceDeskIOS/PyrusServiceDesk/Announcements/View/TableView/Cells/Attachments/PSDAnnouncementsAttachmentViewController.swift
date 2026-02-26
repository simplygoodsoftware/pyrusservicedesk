// MARK: - Контейнер: UIPageViewController для нескольких вложений
final class PSDAnnouncementsAttachmentViewController: UIPageViewController {

    // MARK: - Data
    private let items: [PSDAnnouncementAttachment]
    private var currentIndex: Int

    // MARK: - UI / Nav
    private var shareItem: UIBarButtonItem!
    private var closeItem: UIBarButtonItem!

    // MARK: - Init
    init(attachments: [PSDAnnouncementAttachment], initialIndex: Int = 0) {
        self.items = attachments
        self.currentIndex = max(0, min(initialIndex, attachments.count - 1))
        super.init(transitionStyle: .scroll, navigationOrientation: .horizontal, options: nil)
    }

    convenience init(attachmentId: String, authorId: String, attachmentName: String, isMedia: Bool) {
        let single = PSDAnnouncementAttachment(
            id: attachmentId,
            name: attachmentName,
            size: 0,
            width: 0,
            height: 0,
            media: isMedia
        )
        self.init(attachments: [single], initialIndex: 0)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()

        dataSource = self
        delegate = self

        setupNavigation()
        applyAppearance(for: items[currentIndex])

        let startVC = makePageVC(for: currentIndex)
        setViewControllers([startVC], direction: .forward, animated: false)
    }

    private func setupNavigation() {
        shareItem = UIBarButtonItem(systemItem: .action, primaryAction: UIAction { [weak self] _ in
            self?.shareTapped()
        })
        shareItem.isEnabled = false
        navigationItem.rightBarButtonItem = shareItem

        let symbolCfg = UIImage.SymbolConfiguration(pointSize: 20, weight: .medium, scale: .medium)
        let closeImage = UIImage(systemName: "xmark", withConfiguration: symbolCfg)
        closeItem = UIBarButtonItem(image: closeImage,
                                    style: .plain,
                                    target: self,
                                    action: #selector(dismissOrPop))
        navigationItem.leftBarButtonItem = closeItem
    }

    private func applyAppearance(for item: PSDAnnouncementAttachment) {
        let name = item.name ?? ""
        if items.first?.media ?? false || items.first?.isVideo ?? false {
            title = "\(currentIndex + 1) \("Of".localizedPSD()) \(items.count)"
        } else {
            title = name.isEmpty ? "Attachment".localizedPSD() : name
        }

        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = item.media || item.isVideo ? .black : .psdDarkBackgroundColor
        appearance.titleTextAttributes = [.foregroundColor: item.media || item.isVideo ? UIColor.white : .label]
        navigationController?.navigationBar.standardAppearance = appearance
        navigationController?.navigationBar.scrollEdgeAppearance = appearance
        navigationController?.navigationBar.tintColor = item.media || item.isVideo ? .white : .label
        view.backgroundColor = item.media || item.isVideo ? .black : .psdDarkBackgroundColor
    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        items[currentIndex].media || items[currentIndex].isVideo ? .lightContent : .darkContent
    }

    // MARK: - Actions
    private func shareTapped() {
        guard let page = viewControllers?.first as? AttachmentPageViewController,
              let url = page.shareURL else { return }
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        activity.popoverPresentationController?.barButtonItem = navigationItem.rightBarButtonItem
        present(activity, animated: true)
    }

    @objc private func dismissOrPop() {
        if let nav = navigationController, nav.viewControllers.first != self {
            nav.popViewController(animated: true)
        } else {
            dismiss(animated: true)
        }
    }

    // MARK: - Helpers
    private func makePageVC(for index: Int) -> AttachmentPageViewController {
        let vc = AttachmentPageViewController(item: items[index])
        vc.delegate = self
        return vc
    }
}

// MARK: - UIPageViewControllerDataSource
extension PSDAnnouncementsAttachmentViewController: UIPageViewControllerDataSource {
    func pageViewController(_ pageViewController: UIPageViewController,
                            viewControllerBefore viewController: UIViewController) -> UIViewController? {
        guard let page = viewController as? AttachmentPageViewController,
              let idx = items.firstIndex(where: { $0.id == page.itemId }) else { return nil }
        let prev = idx - 1
        guard prev >= 0 else { return nil }
        return makePageVC(for: prev)
    }

    func pageViewController(_ pageViewController: UIPageViewController,
                            viewControllerAfter viewController: UIViewController) -> UIViewController? {
        guard let page = viewController as? AttachmentPageViewController,
              let idx = items.firstIndex(where: { $0.id == page.itemId }) else { return nil }
        let next = idx + 1
        guard next < items.count else { return nil }
        return makePageVC(for: next)
    }
}

// MARK: - UIPageViewControllerDelegate
extension PSDAnnouncementsAttachmentViewController: UIPageViewControllerDelegate {
    func pageViewController(_ pageViewController: UIPageViewController,
                            didFinishAnimating finished: Bool,
                            previousViewControllers: [UIViewController],
                            transitionCompleted completed: Bool) {
        guard completed,
              let page = viewControllers?.first as? AttachmentPageViewController,
              let idx = items.firstIndex(where: { $0.id == page.itemId }) else { return }
        currentIndex = idx

        applyAppearance(for: items[currentIndex])
        setNeedsStatusBarAppearanceUpdate()

        shareItem.isEnabled = page.shareURL != nil
    }
}

// MARK: - AttachmentPageViewControllerDelegate
extension PSDAnnouncementsAttachmentViewController: AttachmentPageViewControllerDelegate {
    func pageDidUpdateShareURL(_ page: AttachmentPageViewController, shareURL: URL?) {
        if viewControllers?.first === page {
            shareItem.isEnabled = (shareURL != nil)
        }
    }

    func pageDidRequestShare(_ page: AttachmentPageViewController, url: URL) {
        guard viewControllers?.first === page else { return }
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        activity.popoverPresentationController?.barButtonItem = navigationItem.rightBarButtonItem
        present(activity, animated: true)
    }

    func pageDidUpdateAppearance(_ page: AttachmentPageViewController, isMedia: Bool, title: String) {
        if viewControllers?.first === page {
            let item = PSDAnnouncementAttachment(id: page.itemId, name: title, size: 0, width: 0, height: 0, media: isMedia)
            applyAppearance(for: item)
            setNeedsStatusBarAppearanceUpdate()
        }
    }

    func pageDidRequestDismiss(_ page: AttachmentPageViewController) {
        dismissOrPop()
    }
}
