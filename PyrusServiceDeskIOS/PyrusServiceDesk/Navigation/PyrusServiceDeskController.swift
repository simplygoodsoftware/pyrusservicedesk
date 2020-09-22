
import UIKit

///The main service desk controller.
class PyrusServiceDeskController: PSDNavigationController {
    let customization : PyrusServiceDeskCustomization = PyrusServiceDeskCustomization()
    convenience init() {
        if(PyrusServiceDesk.clientId != nil){
            if(PyrusServiceDesk.oneChat){
                let pyrusChat : PSDChatViewController = PSDChatViewController(nibName:nil, bundle:nil)
                self.init(rootViewController: pyrusChat)
            }
            else{
                EventsLogger.logEvent(.emptyClientId)
                let openedPyrusChats : PSDChatsViewController = PSDChatsViewController(nibName:nil, bundle:nil)
                self.init(rootViewController: openedPyrusChats)
            }
            self.transitioningDelegate  = self
            self.isModalInPopover = true
            self.modalPresentationStyle = .overFullScreen
        }else{
             self.init(rootViewController: UIViewController())
        }
    }
    func show(chatId:String?, on viewController: UIViewController, completion:(() -> Void)? = nil){
        DispatchQueue.main.async  {
            if self.viewControllers.first is PSDChatsViewController && chatId != nil && (chatId?.count ?? 0)>0 && chatId != DEFAULT_CHAT_ID{
                (self.viewControllers.first as! PSDChatsViewController).customFirstChatId = chatId
            }
            if UIDevice.current.userInterfaceIdiom != .pad{
                viewController.present(self, animated:  true, completion: completion)
            }
            else{
                let fake : PSDFakeController = PSDFakeController.init()
                fake.transitioningDelegate  = fake
                fake.isModalInPopover = true
                fake.modalPresentationStyle = .overFullScreen
                self.add(to: fake, inContainer: fake.view)
                viewController.present(fake, animated: true, completion: completion)
            }
        }
    }
    
    public override func popViewController(animated: Bool) -> UIViewController? {
        super.popViewController(animated: animated)
        let vc = self.topViewController
        if vc is PSDChatsViewController{
            let table = (vc as! PSDChatsViewController).tableView
            table.deselectRow()
        }
        return vc
    }
    ///Create PyrusServiceDeskController.
    class func create()->PyrusServiceDeskController
    {
        return PyrusServiceDeskController.init()
    }
    override public func viewDidLoad() {
        super.viewDidLoad()
        PyrusServiceDesk.mainController = self
    }
    //reload current top viewControllers
    func updateInfo(){
        (UIApplication.topViewController() as? PSDUpdateInfo)?.startGettingInfo()
    }
    ///reload chat with showing refresh ui
    func refreshChat(showFakeMessage: Int?) {
        for viewController in self.viewControllers{
            guard let chatController = viewController as? PSDUpdateInfo else{
                continue
            }
            chatController.refreshChat(showFakeMessage: showFakeMessage)
            break
        }
    }
    func passChanges(chats:[PSDChat]){
        if self.viewControllers[0] is PSDChatsViewController{
            (self.viewControllers[0] as! PSDChatsViewController).tableView.chats = chats
        }
    }
    ///Clean all saved data
    private func clean(){
        //clean main controller
        PyrusServiceDesk.mainController = nil
        //remove all saved users, exept owner
        PSDUsers.supportUsers.removeAll()
        //remove all saved users images
        PSDSupportImageSetter.clean()
        //remove all info about attachment preview download
        PSDPreviewSetter.clean()
        //stop all loading exept chat list
        PSDMessageSend.stopAll()
        PSDGetChat.remove()
    }
    //
    //MARK: IPad
    //
    ///Using only on iPadView. A chat on right side of container.
    static var pyrusChat : PSDChatViewController? = nil
    ///On iPadView list of chats is in left side, and chat in right side.
    static var iPadView : Bool = false
    /**
     Using to create iPadView in container. On iPadView list of chats is in left side, and chat in right side.
    - parameter viewController: viewController who become a parent.
    - parameter inContainer: UIView where service desk is drawing.
     */
    func add(to viewController:UIViewController,inContainer:UIView){
        PyrusServiceDeskController.iPadView = true
        
        viewController.addChild(self)
        inContainer.addSubview(self.view)
        self.didMove(toParent: viewController)
        
        if(!PyrusServiceDesk.oneChat){
            inContainer.addSubview(separatorView)
            
            PyrusServiceDeskController.pyrusChat = PSDChatViewController(nibName:nil, bundle:nil)
            let navigationChat :UINavigationController = UINavigationController.init(rootViewController: PyrusServiceDeskController.pyrusChat!)
            viewController.addChild(navigationChat)
            inContainer.addSubview(navigationChat.view)
            navigationChat.didMove(toParent: viewController)
            self.addConstraints(to: navigationChat.view, and: self.view, separator:separatorView, inContainer:inContainer)
        }
        else{
            self.view.autoresizingMask = [.flexibleWidth,.flexibleHeight]
        }
    }
    
    func updateTitleChat() {
        for vc in viewControllers{
            if let vcChat =  vc as? PSDChatViewController{
                vcChat.updateTitle()
                break
            }
        }
    }
    
    ///Vertical right separator view for iPad
    private lazy var separatorView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.psdGray
        return view
    }()
    /**
     Remove Pyrus Service Desk Controllers and clean saved data
 */
    func remove(animated: Bool = true){
        if self.parent == nil{
            self.dismiss(animated: animated, completion: {
                PyrusServiceDesk.stopCallback?.onStop()
                self.clean()
            })
            
        }
        else{
            let fake = self.parent
            fake?.dismiss(animated: animated, completion: {
                PyrusServiceDesk.stopCallback?.onStop()
                self.clean()
            })
            
        }
    }
    private static func sendCloseMessageToNotificationCenter(){
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: PyrusServiceDesk.PSD_CLOSED_NOTIFICATION_NAME), object: nil)
    }
    ///
    private func addConstraints(to rightView:UIView, and leftView:UIView, separator:UIView, inContainer:UIView){
        rightView.translatesAutoresizingMaskIntoConstraints = false
        leftView.translatesAutoresizingMaskIntoConstraints = false
        separator.translatesAutoresizingMaskIntoConstraints = false
        
        rightView.addZeroConstraint([.top,.bottom,.trailing])
        leftView.addZeroConstraint([.top,.bottom,.leading])
        separator.addZeroConstraint([.top,.bottom])
        separator.addSizeConstraint([.width], constant: 0.5)
        
        inContainer.addConstraint(NSLayoutConstraint(
            item: leftView,
            attribute: .trailing,
            relatedBy: .equal,
            toItem: separator,
            attribute: .leading,
            multiplier: 1,
            constant:0))
        inContainer.addConstraint(NSLayoutConstraint(
            item: separator,
            attribute: .trailing,
            relatedBy: .equal,
            toItem: rightView,
            attribute: .leading,
            multiplier: 1,
            constant:0))
        
        inContainer.addConstraint(NSLayoutConstraint(
            item: leftView,
            attribute: .width,
            relatedBy: .greaterThanOrEqual,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:150))
        inContainer.addConstraint(NSLayoutConstraint(
            item: leftView,
            attribute: .width,
            relatedBy: .lessThanOrEqual,
            toItem: inContainer,
            attribute: .width,
            multiplier: 0.4,
            constant:0))
        
    }
    

}
