
import UIKit

protocol PSDChatsTableViewDelegate: AnyObject {
    func openChat(_ id : Int, subject: String?, animated: Bool)
    func removeOneNewMessage()
    func updateLoading(loading: Bool)
}

class PSDChatsTableView: PSDTableView, UITableViewDelegate, UITableViewDataSource, PSDGetDelegate, PSDNoConnectionViewDelegate {
    weak var chatsDelegate: PSDChatsTableViewDelegate?
    let chatCellId = "ChatCell"
    
    private lazy var customRefresh: UIRefreshControl = {
        let refreshControl = UIRefreshControl()
        refreshControl.transform = CGAffineTransformMakeScale(0.8, 0.8)
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()
    
    var chats : [PSDChat] = []{
        didSet{
            self.reloadData()
        }
    }
    
    func setupTableView() {
        self.delegate = self
        self.dataSource = self
        self.backgroundColor = .psdBackground
        self.estimatedRowHeight = UITableView.automaticDimension
        
        self.rowHeight = UITableView.automaticDimension
        self.register(PSDChatInfoTableViewCell.self, forCellReuseIdentifier: chatCellId)
        
        self.separatorColor = .psdGray
        self.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        self.keyboardDismissMode = .onDrag
        
        if #available(iOS 10.0, *) {
            self.refreshControl = customRefresh
        }
    }
    
    func setupAutoLayout(view:UIView) {
        self.translatesAutoresizingMaskIntoConstraints = false
        if #available(iOS 11.0, *) {
            let guide = view.safeAreaLayoutGuide
            self.trailingAnchor.constraint(equalTo: guide.trailingAnchor).isActive = true
            self.leadingAnchor.constraint(equalTo: guide.leadingAnchor).isActive = true
            self.topAnchor.constraint(equalTo: guide.topAnchor).isActive = true
            self.bottomAnchor.constraint(equalTo: guide.bottomAnchor).isActive = true
        } else {
            self.leftAnchor.constraint(equalTo: view.layoutMarginsGuide.leftAnchor).isActive = true
            self.rightAnchor.constraint(equalTo: view.layoutMarginsGuide.rightAnchor).isActive = true
            self.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
            self.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        }
    }
    
    @objc func refresh(sender: AnyObject) {
       reloadChats(needProgress: true)
    }
    
    override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: style)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if self.superview != nil{
            self.setupAutoLayout(view: self.superview!)
        }
    }
    /**
     Deselect selected row, if has one
     */
    func deselectRow(){
        let selectedItems = self.indexPathForSelectedRow
        if(selectedItems != nil){
            self.deselectRow(at: selectedItems!, animated: false)
        }
    }
    
    func reloadChats(needProgress: Bool) {
        chatsDelegate?.updateLoading(loading: needProgress)
        self.chats = PyrusServiceDesk.chats
        self.reloadData()
        
        PyrusServiceDesk.restartTimer()
        
        DispatchQueue.global().async { [weak self] in
            PSDGetChats.get() { [weak self] chats in
                DispatchQueue.main.async {
                    if chats != nil && (chats?.count ?? 0) > 0 {
                        self?.chats = chats!
                        self?.reloadData()
                    }
                    self?.chatsDelegate?.updateLoading(loading: false)
                    self?.customRefresh.endRefreshing()
                }
            }
        }
    }
    
    deinit {
        PSDGetChats.remove()
        
        self.customRefresh.endRefreshing()
        self.customRefresh.removeFromSuperview()
    }
    
    private lazy var noConnectionView : PSDNoConnectionView  = {
        let view = PSDNoConnectionView.init(frame: self.frame)
        view.delegate = self
        return view
    }()
    
    //PSDGetDelegate method
    func showNoConnectionView(){
        if !(self.superview?.subviews.contains(noConnectionView) ?? false) {
            self.superview?.addSubview(noConnectionView)
        }
    }
    
    //PSDNoConnectionViewDelegate method
    func retryPressed(){
        reloadChats(needProgress: true)
    }
    
    //MARK: for table
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return chats.count;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: chatCellId, for: indexPath) as! PSDChatInfoTableViewCell
        cell.time = chats[indexPath.row].date!.timeIntervalString()
        cell.notifacationNumber = !(chats[indexPath.row].isRead) as NSNumber
        cell.firstMessageText = chats[indexPath.row].messages[0].text
        cell.selectionStyle = .none
        if chats[indexPath.row].messages.count > 1 {
            cell.lastMessageText = chats[indexPath.row].messages[1].text
        }
        else{
            cell.lastMessageText = "Last_Message".localizedPSD()
        }
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }
    
    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let chatId = chats[indexPath.row].chatId ?? 0
        if !(chats[indexPath.row].isRead) {
            self.chatsDelegate?.removeOneNewMessage()
        }

        self.chatsDelegate?.openChat(chatId, subject: chats[indexPath.row].subject, animated: true)

    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1.0
    }
}

private extension UIFont {
    static let newConversation = CustomizationHelper.systemFont(ofSize: 17.0)
}
