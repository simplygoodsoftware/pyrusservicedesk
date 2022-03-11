
import UIKit

class PSDChatsViewController: UIViewController,PSDChatsTableViewDelegate,CloseButtonItemDelegate,PSDUpdateInfo {
    ///Chat id that need to be open when viewcontroller appear.
    ///If nil will be opened last chat or new chat.
    var customFirstChatId :String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        design()
        designNavigation()
        if customFirstChatId != nil{
            self.openChat(customFirstChatId!,animated:false)
        }
        else{
            openLastChat()
        }
        
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .automatic
        } 
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.tableView.deselectRow()
        startGettingInfo()

    }
    func startGettingInfo(){
        tableView.reloadChats(needProgress:true)
    }
    func refreshChat(showFakeMessage: Int?) {
    }
    lazy var progressView: PSDProgressView = {
        let view = PSDProgressView()
        return view
    }()
    /**Setting design To PSDChatsViewController view, add subviews*/
    func design() {
        self.view.backgroundColor = UIColor.psdBackground
        self.view.addSubview(tableView)
        self.setCloseButton()
        
        progressView.draw(at: self.view)
        tableView.progressView = progressView
    }
    func setCloseButton()
    {
        let rightButtonItem : UIBarButtonItem
        rightButtonItem = CloseButtonItem.init(self)
        self.navigationItem.rightBarButtonItem = rightButtonItem
        self.navigationItem.rightBarButtonItem?.tintColor = .darkAppColor
        
    }
    @objc func closeButtonAction(){
        PyrusServiceDesk.mainController?.remove()
    }
    @objc func openNewButtonAction(){
        tableView.deselectRow()
        self.openChat("",animated: true)
    }
    func openLastChat(){
    }
    //PSDChatsTableViewDelegate
    func openChat(_ id : String, animated: Bool)
    {
    }
    ///Remove from new messages counter 1
    func removeOneNewMessage(){
        PSDGetChats.refreshNewMessagesCount(PyrusServiceDesk.newMessagesCount-1)
    }
    //*Setting design to navigation bar, title and buttons*/
    func designNavigation()
    {
        self.title = "All_Conversations".localizedPSD()
    }
    lazy var tableView: PSDChatsTableView = {
        let table = PSDChatsTableView(frame: self.view.bounds)
        table.chatsDelegate = self
        table.setupTableView()
        return table
    }()
}
