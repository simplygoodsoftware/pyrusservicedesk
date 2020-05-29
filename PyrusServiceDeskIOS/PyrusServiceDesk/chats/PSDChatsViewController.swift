
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
        
        
        if PyrusServiceDeskController.iPadView{
            self.tableView.addKeyboardListeners()
        }
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .automatic
        } 
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if(!PyrusServiceDeskController.iPadView){
            self.tableView.deselectRow()
        }
        startGettingInfo()

    }
    func startGettingInfo(){
        tableView.reloadChats(needProgress:true)
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
        if(PyrusServiceDeskController.iPadView){
            rightButtonItem = UIBarButtonItem.init(
                image: nil,
                style: .plain,
                target: self,
                action: #selector(openNewButtonAction))
            rightButtonItem.title = "New_Conversation_Short".localizedPSD()
        }
        else{
            rightButtonItem = CloseButtonItem.init(self)
        }
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
        if(PyrusServiceDesk.chats.first?.chatId?.count ?? 0) > 0 && PyrusServiceDesk.chats.first?.chatId != DEFAULT_CHAT_ID{
            let lastChat = PyrusServiceDesk.chats.first
            if !(lastChat?.isRead ?? true) {
                self.removeOneNewMessage()
            }
            self.openChat((lastChat?.chatId)!,animated:false)
        }
        else{
            self.openChat("",animated:false)
        }
        
    }
    //PSDChatsTableViewDelegate
    func openChat(_ id : String, animated: Bool)
    {
        //pyrusChat is on screan just pass value
        if(PyrusServiceDeskController.pyrusChat != nil){
            PyrusServiceDeskController.pyrusChat?.chatId = id
            PyrusServiceDeskController.pyrusChat?.openChat()
        }
        else{//if it does not exist create and open
            let pyrusChat = PSDChatViewController(nibName:nil, bundle:nil)
            pyrusChat.chatId = id
            self.navigationController?.pushViewController(pyrusChat, animated: animated)
        }
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
