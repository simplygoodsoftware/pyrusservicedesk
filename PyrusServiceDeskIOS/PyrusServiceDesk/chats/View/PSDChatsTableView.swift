
//import UIKit
//
//protocol PSDChatsTableViewDelegate: AnyObject {
//    func openChat(_ chat: PSDChat, subject: String?, animated: Bool)
//    func removeOneNewMessage()
//    func updateLoading(loading: Bool)
//    func setEmptyChatsVisible(visible: Bool)
//}
//
//class PSDChatsTableView: PSDTableView, PSDGetDelegate, PSDNoConnectionViewDelegate {
//    weak var chatsDelegate: PSDChatsTableViewDelegate?
//    private var cellConfigurator: PSDChatsCellConfigurator?
//    private var diffabledDataSource: UITableViewDiffableDataSource<PSDChatsSectionModel, AnyHashable>?
//    
//    private lazy var customRefresh: UIRefreshControl = {
//        let refreshControl = UIRefreshControl()
//        refreshControl.transform = CGAffineTransformMakeScale(0.8, 0.8)
//        refreshControl.isOpaque = false
//        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
//        return refreshControl
//    }()
//    
//    var chats : [PSDChat] = [] {
//        didSet {
//            reloadDiffable(animated: true)
//        }
//    }
//    
//    override init(frame: CGRect, style: UITableView.Style) {
//        super.init(frame: frame, style: style)
//    }
//    
//    deinit {
//        PSDGetChats.remove()
//        self.customRefresh.endRefreshing()
//        self.customRefresh.removeFromSuperview()
//    }
//    
//    required init?(coder aDecoder: NSCoder) {
//        fatalError("init(coder:) has not been implemented")
//    }
//    
//    override func didMoveToSuperview() {
//        super.didMoveToSuperview()
//        if let superview {
//            self.setupAutoLayout(view: superview)
//        }
//    }
//    
//    func setupTableView() {
//        self.delegate = self
//        self.backgroundColor = .psdBackground
//        self.estimatedRowHeight = UITableView.automaticDimension
//        self.rowHeight = UITableView.automaticDimension
//        self.separatorInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 0)
//        self.keyboardDismissMode = .onDrag
//        self.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 100, right: 0)
//        
//        addSubview(customRefresh)
//        
//        cellConfigurator = PSDChatsCellConfigurator(tableView: self)
//        setupDataSource()
//        
//        //NotificationCenter.default.addObserver(self, selector: #selector(updateChats), name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
//    }
//    
//    func setupAutoLayout(view: UIView) {
////        self.translatesAutoresizingMaskIntoConstraints = false
////        if #available(iOS 11.0, *) {
////            let guide = view.safeAreaLayoutGuide
////            self.trailingAnchor.constraint(equalTo: guide.trailingAnchor).isActive = true
////            self.leadingAnchor.constraint(equalTo: guide.leadingAnchor).isActive = true
////            self.topAnchor.constraint(equalTo: guide.topAnchor).isActive = true
////            self.bottomAnchor.constraint(equalTo: guide.bottomAnchor).isActive = true
////        } else {
////            self.leftAnchor.constraint(equalTo: view.layoutMarginsGuide.leftAnchor).isActive = true
////            self.rightAnchor.constraint(equalTo: view.layoutMarginsGuide.rightAnchor).isActive = true
////            self.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
////            self.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
////        }
//    }
//    
//    func updateChats() {
//        DispatchQueue.main.async {
//            self.customRefresh.endRefreshing()
//            self.chatsDelegate?.setEmptyChatsVisible(visible: PyrusServiceDesk.chats.count == 0)
//            if self.chats != PyrusServiceDesk.chats {
//                self.chats = PyrusServiceDesk.chats
//            }
//        }
//    }
//    
//    @objc func refresh(sender: AnyObject) {
//       reloadChats(needProgress: true)
//    }
//    
//    /**
//     Deselect selected row, if has one
//     */
//    func deselectRow(){
//        if let selectedItems = indexPathForSelectedRow {
//            deselectRow(at: selectedItems, animated: false)
//        }
//    }
//    
//    func reloadChats(needProgress: Bool) {
//        chatsDelegate?.updateLoading(loading: needProgress)
//        PyrusServiceDesk.restartTimer()
//        
//        DispatchQueue.global().async { [weak self] in
//            PSDGetChats.get() { [weak self] chats in
//                DispatchQueue.main.async {
//                    self?.chatsDelegate?.updateLoading(loading: false)
//                    self?.customRefresh.endRefreshing()
//                    self?.sendSubviewToBack(self?.customRefresh ?? UIView())
//                }
//            }
//        }
//    }
//    
//    private lazy var noConnectionView : PSDNoConnectionView  = {
//        let view = PSDNoConnectionView.init(frame: self.frame)
//        view.delegate = self
//        return view
//    }()
//    
//    //MARK: PSDGetDelegate method
//    func showNoConnectionView(){
//        if !(self.superview?.subviews.contains(noConnectionView) ?? false) {
//            self.superview?.addSubview(noConnectionView)
//        }
//    }
//    
//    //MARK: PSDNoConnectionViewDelegate method
//    func retryPressed(){
//        reloadChats(needProgress: true)
//    }
//    
//    private func setupDataSource() {
//        let newDataSource = PSDChatsDiffableDataSource.createDataSource(for: self, cellCreator: self)
//        //newDataSource.defaultRowAnimation = .fade
//        dataSource = newDataSource
//        diffabledDataSource = newDataSource
//    }
//    
//    private func reloadDiffable(animated: Bool) {
//        guard let diffabledDataSource = diffabledDataSource else { return }
//        var snapshot = NSDiffableDataSourceSnapshot<PSDChatsSectionModel, AnyHashable>()
//        let section = PSDChatsSectionModel()
//        snapshot.appendSections([section])
//        snapshot.appendItems(chats, toSection: section)
//        
//        diffabledDataSource.apply(snapshot, animatingDifferences: animated)
//        self.diffabledDataSource = diffabledDataSource
//    }
//}
//
//extension PSDChatsTableView: UITableViewDelegate, UITableViewDataSource {
//    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
//        return 1
//    }
//    
//    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
//        return chats.count;
//    }
//    
//    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
//       // let cell = cellConfigurator?.getCell(model: chats[indexPath.row], indexPath: indexPath) ?? PSDChatInfoTableViewCell()
//        return cell
//    }
//    
//    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
//        return 80//UITableView.automaticDimension
//    }
//    
//    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
//        return UITableView.automaticDimension
//    }
//    
//    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
//        let chat = chats[indexPath.row]
//        if !(chats[indexPath.row].isRead) {
//            self.chatsDelegate?.removeOneNewMessage()
//        }
//        deselectRow(at: indexPath, animated: true)
//        self.chatsDelegate?.openChat(chat, subject: chats[indexPath.row].subject, animated: true)
//
//    }
//    
//    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
//        return UIView()
//    }
//    
//    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
//        return 1.0
//    }
//}
//
//private extension UIFont {
//    static let newConversation = CustomizationHelper.systemFont(ofSize: 17.0)
//}
