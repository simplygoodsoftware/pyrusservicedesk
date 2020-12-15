
import UIKit

protocol PSDChatsTableViewDelegate: class {
    func openChat(_ id : String, animated: Bool)
    func removeOneNewMessage()
}

class PSDChatsTableView: PSDTableView,UITableViewDelegate,UITableViewDataSource,PSDGetDelegate,PSDNoConnectionViewDelegate {
    weak var chatsDelegate: PSDChatsTableViewDelegate?
    let chatCellId = "ChatCell"
    var progressView : PSDProgressView?
    private lazy var customRefresh: PSDRefreshControl = {
        let refreshControl = PSDRefreshControl.init(frame: self.bounds)
        refreshControl.position = .top
        refreshControl.addTarget(self, action: #selector(refresh), for: .valueChanged)
        return refreshControl
    }()
    var chats : [PSDChat] = []{
        didSet{
            self.reloadData()
        }
    }
    func setupTableView() {
        self.delegate=self
        self.dataSource=self
        self.backgroundColor = .psdBackground
        self.estimatedRowHeight = UITableView.automaticDimension
        
        self.rowHeight = UITableView.automaticDimension
        self.register(PSDChatInfoTableViewCell.self, forCellReuseIdentifier: chatCellId)
        
        self.separatorColor = .psdGray
        self.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        self.keyboardDismissMode = .onDrag
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
    @objc func refresh(sender:AnyObject) {
       reloadChats(needProgress:false)
    }
    override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: style)
        self.contentInset = UIEdgeInsets(top: self.contentInset.top, left: self.contentInset.left, bottom: newConversation.frame.size.height, right: self.contentInset.right)
        
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if self.superview != nil {
            self.superview?.addSubview(newConversation)
        }
        else{
            newConversation.removeFromSuperview()
        }
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
    func reloadChats(needProgress:Bool)
    {
        customRefresh.insetHeight = 0.0
        self.chats = PyrusServiceDesk.chats
        self.reloadData()
        
        PyrusServiceDesk.restartTimer()
        if needProgress{
            self.progressView?.startAnimate()
        }
        DispatchQueue.global().async {
            [weak self] in
            PSDGetChats.get(delegate:self, needShowError: !PyrusServiceDesk.hasInfo){
                (chats:[PSDChat]?) in
                DispatchQueue.main.async {
                    if chats != nil && (chats?.count ?? 0)>0{
                        self?.chats = chats!
                        self?.reloadData()
                        if(self != nil){
                            self!.insertSubview(self!.customRefresh, at: 0)
                        }
                        
                    }
                    if needProgress{
                        self?.progressView?.progress = 1.0
                    }
                    else{
                        self?.customRefresh.endRefreshing()
                    }
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
        if !(self.superview?.subviews.contains(noConnectionView) ?? false){
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
        if chats[indexPath.row].messages.count>1{
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
        let chatId :String = chats[indexPath.row].chatId ?? ""
        if !(chats[indexPath.row].isRead){
            self.chatsDelegate?.removeOneNewMessage()
        }
        self.chatsDelegate?.openChat(chatId,animated: true)
    }
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1.0
    }
    private let footerHeight: CGFloat = 50
    ///New conversation view with tapGestureRecognizer.
    lazy var newConversation: UIView = {
        let footer :UIView = UIView ()
        var addditionalHeight : CGFloat = 0.0
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.keyWindow
            addditionalHeight = window?.safeAreaInsets.bottom ?? 0.0
        }
        let label = UILabel()
        label.textColor = .darkAppColor
        label.text = "New_Conversation".localizedPSD()
        label.font = .newConversation
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.preferredMaxLayoutWidth = self.frame.size.width
        label.textAlignment = .center
        label .sizeToFit()
        label.frame = CGRect(x: 0, y: 0, width: self.frame.size.width, height: label.frame.size.height)
        
        footer.frame = CGRect(x: 0, y: self.frame.size.height-(footerHeight+addditionalHeight), width: self.frame.size.width, height: footerHeight+addditionalHeight)

        label.center = CGPoint(x: footer.frame.size.width/2, y: footerHeight/2)
        footer.autoresizingMask = [.flexibleWidth,.flexibleTopMargin]
        label.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
        footer .addSubview(createFooterLine(atY: 0))
        footer .addSubview(label)
        footer.backgroundColor = UIColor.psdBackground
        
        let gesture = UITapGestureRecognizer(target: self, action: #selector(footerTapped))
        footer.addGestureRecognizer(gesture)

        return footer
    }()
    
    ///Pass empty id to chatsDelegate
    @objc func footerTapped(sender: UITapGestureRecognizer) {
        self.chatsDelegate?.openChat("",animated: true)
    }
    
    ///Creates thin line
    ///- parameter y: y - float y position for line's frame
    func createFooterLine (atY y:CGFloat)->UIView{
        let line : UIView = UIView ()
        line.frame = CGRect(x: 0, y: y, width: self.frame.size.width, height: 0.5)
        line.backgroundColor = .psdGray
        line.autoresizingMask = [.flexibleWidth,.flexibleBottomMargin]
        return line
    }

}
private extension UIFont {
    static let newConversation = CustomizationHelper.systemFont(ofSize: 17.0)
}
