
import UIKit

class PSDChatsViewController: UIViewController, PSDUpdateInfo {
    lazy var tableView: PSDChatsTableView = {
        let table = PSDChatsTableView(frame: self.view.bounds)
        table.chatsDelegate = self
        table.setupTableView()
        return table
    }()
    private var timer: Timer?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        design()
        designNavigation()
        PyrusServiceDesk.mainController?.customization?.setCustomLeftBarButtonItem(backBarButtonItem())
        
        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .automatic
        } 
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.tableView.deselectRow()
        startGettingInfo()

    }
    
    func startGettingInfo() {
        tableView.reloadChats(needProgress:true)
    }
    
    func refreshChat(showFakeMessage: Int?) {
    }
    
    /**Setting design To PSDChatsViewController view, add subviews*/
    func design() {
        self.view.backgroundColor = UIColor.psdBackground
        self.view.addSubview(tableView)
    }
    
    @objc func closeButtonAction() {
        PyrusServiceDesk.mainController?.remove()
    }
    
    @objc func openNewButtonAction() {
        tableView.deselectRow()
        self.openChat(0, subject: "Новое обращение", animated: true)
    }

    //*Setting design to navigation bar, title and buttons*/
    func designNavigation() {
        title = "All_Conversations".localizedPSD()
        if #available(iOS 13.0, *) {
            let rightBarButtonItem = UIBarButtonItem(
                image: UIImage(systemName: "plus"),
                style: .plain, 
                target: self,
                action: #selector(openNewButtonAction)
            )
            navigationItem.rightBarButtonItem = rightBarButtonItem
        }
        navigationItem.rightBarButtonItem?.tintColor = .darkAppColor
        navigationItem.leftBarButtonItem = PyrusServiceDesk.mainController?.customization?.chatsLeftBarButtonItem
    }
    
    func backBarButtonItem() -> UIBarButtonItem {
        let mainColor = PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor
        let button = UIButton()
        button.titleLabel?.font = CustomizationHelper.systemFont(ofSize: 18)
        button.setTitle(" " + "Back".localizedPSD(), for: .normal)
        button.setTitleColor(mainColor, for: .normal)
        button.setTitleColor(mainColor.withAlphaComponent(0.2), for: .highlighted)
        if #available(iOS 13.0, *) {
            let backImage = UIImage(systemName: "chevron.left", withConfiguration: UIImage.SymbolConfiguration(pointSize: 18, weight: .semibold, scale: .large))
            button.setImage(backImage?.imageWith(color: mainColor), for: .normal)
            button.setImage(backImage?.imageWith(color: mainColor.withAlphaComponent(0.2)), for: .highlighted)
        }
        button.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        button.sizeToFit()
        return UIBarButtonItem(customView: button)
    }
    
    @objc func goBack() {
        navigationController?.popViewController(animated: true)
    }
}

extension PSDChatsViewController: PSDChatsTableViewDelegate {
    func openChat(_ id: Int, subject: String?, animated: Bool) {
        let config = PyrusServiceDesk.mainController?.customization
        config?.setChatTitile(subject)
        let pyrusChat = PSDChatViewController()
        pyrusChat.ticketId = id
        navigationController?.pushViewController(pyrusChat, animated: false)
    }

    func updateLoading(loading: Bool) {
        title = loading ? "Uploading".localizedPSD() : "All_Conversations".localizedPSD()
    }
    
    ///Remove from new messages counter 1
    func removeOneNewMessage(){
        //PSDGetChats.refreshNewMessagesCount(PyrusServiceDesk.newMessagesCount-1)
    }
}
