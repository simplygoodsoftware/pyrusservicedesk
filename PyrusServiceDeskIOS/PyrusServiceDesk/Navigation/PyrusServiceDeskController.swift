
import UIKit

///The main service desk controller.
class PyrusServiceDeskController: PSDNavigationController {
    let customization: ServiceDeskConfiguration?
    required init(_ customization: ServiceDeskConfiguration?, customPresent: Bool) {
        self.customization = customization
        if(PyrusServiceDesk.clientId != nil){
            let pyrusChat = PSDChatViewController()
            super.init(nibName: nil, bundle: nil)
            pushViewController(pyrusChat, animated: false)
            if !customPresent {
                self.transitioningDelegate  = self
                self.isModalInPopover = true
                self.modalPresentationStyle = .overFullScreen
            }
        }else{
            EventsLogger.logEvent(.emptyClientId)
            super.init(nibName: nil, bundle: nil)
            pushViewController(UIViewController(), animated: false)
        }
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    func show(on viewController: UIViewController, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async  {
            viewController.present(self, animated:  true, completion: completion)
        }
    }
    override public func viewDidLoad() {
        super.viewDidLoad()
        PyrusServiceDesk.mainController = self
        recolor()
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
    
    
    func updateTitleChat() {
        for vc in viewControllers{
            if let vcChat =  vc as? PSDChatViewController{
                vcChat.updateTitle()
                break
            }
        }
    }
    
    func closeServiceDesk() {
        if PyrusServiceDeskController.PSDIsOpen() {
            let alertAuthorized = UIAlertController(title: nil, message: "AcсessDenied".localizedPSD(), preferredStyle: .alert)
            alertAuthorized.addAction(UIAlertAction(title: "OK".localizedPSD(), style: .default, handler: { (_) in
                self.remove()
            }))
            self.present(alertAuthorized, animated: true, completion: nil)
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
        PyrusLogger.shared.saveLocalLogToDisk()
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
    
    public static func PSDIsOpen() -> Bool {
        return PyrusServiceDesk.mainController != nil
    }
}
