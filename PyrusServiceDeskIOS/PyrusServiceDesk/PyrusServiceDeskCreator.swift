import UIKit

@objc public class PyrusServiceDesk: NSObject {
    private static let PSD_USER_START_CHAT_KEY = "PSDUserStartChat"
    public static var PSD_CLOSED_NOTIFICATION_NAME = "PyrusServiceDeskWasClosed"
    ///AppId needed for request
    static var clientId: String?

    ///UserId needed for request
    static private(set) var userId: String = "" {
        didSet(oldUserId){
            if userId.count>0 && userId != oldUserId{
                let user = PSDUser.init(personId: userId, name: PyrusServiceDesk.userName, type: .user, imagePath: "")
                PSDUsers.add(user: user)
            }
            if(userId.count > 0){
                if(oldUserId != userId && clientId != nil){
                    startGettingInfo(rightNow: true)
                }
            }
            else{
                stopGettingInfo()
            }
        }
    }
    ///User's name needed for request. If don't set used Default_User_Name
    @objc static private(set) var userName : String = "Default_User_Name".localizedPSD()
    
    
    ///A flag indicates need to show chat list or show all conversations as one. Default is false - user can create new chat and see the list of old chats. If true - all new messages will be added to open chat.
    @objc private(set) static var  oneChat:Bool = false
    
      
    @objc static let mainSession : URLSession = {
        let config = URLSessionConfiguration.default
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        config.urlCache = nil
        return URLSession(configuration: config)
    }()
    
    /**
     Send device id to server
     - parameter token: String with device id
     - parameter completion: Error. Not nil if success. See error.localizedDescription to understand why its happened
 */
    @objc public static func setPushToken(_ token:String?, completion: @escaping(Error?) -> Void){
        guard let clientId = clientId, clientId.count > 0, clientId != "0"  else{
            completion(PSDError.init(description: "AppId is invalid"))
            EventsLogger.logEvent(.emptyClientId)
            return
        }
        guard let token = token, token.count > 0 else{
            completion(PSDError.init(description: "Token is invalid"))
            EventsLogger.logEvent(.invalidPushToken)
            return
        }
        PSDPushToken.send(token, completion: {
            error in
            completion(error)
        })
    }
    
    ///Show chat
    ///- parameter viewController: ViewController that must present chat
    ///- parameter onStopCallback: OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
    @objc public static func start(on viewController:UIViewController, onStopCallback: OnStopCallback? = nil){
        start(ticketId: nil, on: viewController, configuration: nil, completion: nil, onStopCallback: onStopCallback)
    }
    ///Show chat
    ///- parameter viewController: ViewController that must present chat
    ///- parameter configuration: ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support's avatar and chat title for navigation bar title. If nil, the default design will be used.
    ///- parameter onStopCallback: OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
    @objc public static func start(on viewController:UIViewController, configuration:ServiceDeskConfiguration?, onStopCallback: OnStopCallback? = nil){
        start(ticketId: nil, on: viewController, configuration: configuration, completion: nil, onStopCallback: onStopCallback)
    }
    ///Show chat
    ///- parameter viewController: ViewController that must present chat
    ///- parameter configuration: ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support's avatar and chat title for navigation bar title. If nil, the default design will be used.
    ///- parameter completion: The block to execute after the presentation finishes. This block has no return value and takes no parameters. You may specify nil for this parameter.
    ///- parameter onStopCallback: OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
    @objc public static func start(on viewController:UIViewController, configuration:ServiceDeskConfiguration?, completion:(() -> Void)? = nil, onStopCallback: OnStopCallback? = nil){
        start(ticketId: nil, on: viewController, configuration: configuration, completion: completion, onStopCallback: onStopCallback)
    }
    ///Show chat
    /// - parameter ticketId: The id of chat that need to be opened.
    ///- parameter viewController: ViewController that must present chat
    ///- parameter onStopCallback: OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
    private static func start(ticketId: String?, on viewController:UIViewController, onStopCallback: OnStopCallback?){
        start(ticketId: nil, on: viewController, configuration: nil, completion: nil, onStopCallback: onStopCallback)
    }
    ///Show chat
    /// - parameter ticketId: The id of chat that need to be opened.
    ///- parameter viewController: ViewController that must present chat
    ///- parameter configuration: ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support's avatar and chat title for navigation bar title. If nil, the default design will be used.
    ///- parameter onStopCallback: OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
    private static func start(ticketId: String?, on viewController:UIViewController, configuration:ServiceDeskConfiguration?, completion:(() -> Void)?, onStopCallback: OnStopCallback?){
        stopCallback = onStopCallback
        if !PSDIsOpen(){
            EventsLogger.logEvent(.openPSD)
            let psd : PyrusServiceDeskController = PyrusServiceDeskController.create()
            configuration?.buildCustomization()
            psd.show(chatId: ticketId, on: viewController, completion: completion)
        }
        else{
            PyrusServiceDesk.mainController?.updateInfo()
        }
        
    }
    ///Close PyrusServiceDesk
    @objc public static func stop(){
        PyrusServiceDesk.mainController?.remove(animated: false)
    }
    ///The subscriber for new messages from support.
    weak static  private(set) var subscriber : NewReplySubscriber?
    ///The subscriber for PyrusSecviceDeskClose.
    weak static  private(set) var stopCallback : OnStopCallback?
    weak static private(set) var logEvent: LogEvents?
    ///Subscribe [subscriber] for notifications that new messages from support have appeared in the chat.
    @objc public static func subscribeToReplies(_ subscriber: NewReplySubscriber?){
        PyrusServiceDesk.subscriber = subscriber
    }
    ///Unsubscribe [subscriber] from alerts for new messages from chat support.
    @objc public static func unsubscribeFromReplies(_ subscriber: NewReplySubscriber?){
        PyrusServiceDesk.subscriber = nil
    }
    
    @objc public static func subscribeToGogEvents(_ subscriber: LogEvents){
        PyrusServiceDesk.logEvent = subscriber
    }
    
    ///Init PyrusServiceDesk with new clientId.
    ///- parameter clientId: clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
    @objc static public func createWith(_ clientId: String?)  {
        if clientId != nil && (clientId?.count ?? 0)>0 {
            PyrusServiceDesk.clientId = clientId
            PyrusServiceDesk.oneChat = true
            PyrusServiceDesk.createUserId()
        }else{
            EventsLogger.logEvent(.emptyClientId)
        }
    }
    @objc static public func refresh() {
        PyrusServiceDesk.mainController?.refreshChat()
    }
    /*
    ///Init PyrusServiceDesk with new clientId.
    ///- parameter clientId: clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
    ///- parameter userId: The id of user. Pass nil or empty string to automatic id generation.
   /// - parameter oneChat: A flag indicates need to show chat list or show all conversations as one. Default is false - user can create new chat and see the list of old chats. If true - all new messages will be added to open chat.
    @objc public init(_ clientId: String?, oneChat:Bool)  {
        if clientId != nil && (clientId?.count ?? 0)>0 {
            PyrusServiceDesk.clientId = clientId
            PyrusServiceDesk.oneChat = oneChat
        }
    }*/
    private static let PSD_USER_ID_KEY = "PSDUserId"
     private static func createUserId(_ reset: Bool = false) {
        //block changes when chat is opened now
        guard !PSDIsOpen() else {
            return
        }
        let userId : String
        if let existKey =   PSDMessagesStorage.pyrusUserDefaults()?.object(forKey: PSD_USER_ID_KEY) as? String, !reset{
            userId = existKey
        }else{
            userId = reset ? String.getUiqueString() : (UIDevice.current.identifierForVendor?.uuidString ?? String.getUiqueString())
            PSDMessagesStorage.pyrusUserDefaults()?.set(userId, forKey: PSD_USER_ID_KEY)
            PSDMessagesStorage.pyrusUserDefaults()?.synchronize()
        }
        PyrusServiceDesk.userId = userId
    }
    private static func PSDIsOpen()->Bool{
        return (UIApplication.topViewController() is PSDChatViewController) || (UIApplication.topViewController() is PSDChatsViewController) || (UIApplication.topViewController() is PSDAttachmentLoadViewController)
    }
    
    ///Setting name of user. If name is not setted it il be default ("Guest")
    ///- parameter userName: A name to display in pyrus task.
    static func setUser(_ userName:String?){
        guard let userName = userName else{
            return
        }
        if userName.count>0{
            PyrusServiceDesk.userName = userName
            if PSDUsers.user != nil{
                PSDUsers.user.name = userName
            }
            
        }
    }
    
    ///viewController with FileChooser interface. Use to add custom row in attachment-add-menu
    private(set) static var fileChooserController : (FileChooser & UIViewController)?
    /**
     Save viewController with FileChooser interface. Use to add custom row in attachment-add-menu
     - parameter chooser: (FileChooser & UIViewController) to present.
     */
    @objc public static func registerFileChooser(_ chooser: (FileChooser & UIViewController)?){
        self.fileChooserController = chooser
    }
    private static func didStartChatWithSupport() -> Bool {
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(){
            return pyrusUserDefaults.bool(forKey: userDidStartChatKey())
        }
        return false
    }
    ///Set didStartChatWithSupport to true
    static func setDidStartChatWithSupport(){
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(), !didStartChatWithSupport(){
            pyrusUserDefaults.set(true, forKey: userDidStartChatKey())
            pyrusUserDefaults.synchronize()
        }
    }
    private static func userDidStartChatKey() -> String{
        return PSD_USER_START_CHAT_KEY + "_" + userId
    }
    
    //MARK: get user info automatic
    ///The reload interval to get info from server(chats list)
    private static let reloadInterval : TimeInterval = 1800.0
    ///The timer to get info from server(chats list) with reloadInterval.
    private static var timer :Timer?
    ///Create timer to get chats list from server.
    ///- parameter rightNow: Is need to fire timer.
    private static func startGettingInfo(rightNow:Bool){
        stopGettingInfo()
        timer = Timer.scheduledTimer(timeInterval: reloadInterval, target: self, selector: #selector(updateUserInfo), userInfo:nil , repeats: didStartChatWithSupport())
        if rightNow{
            timer?.fire()
        }
    }
    ///Restart PyrusServiceDesk.timer - move next fire to reloadInterval.
    static func restartTimer(){
        PyrusServiceDesk.startGettingInfo(rightNow: false)
    }
    ///Stops PyrusServiceDesk.timer.
    private static func stopGettingInfo(){
        if timer != nil{
            timer?.invalidate()
            timer=nil
        }
    }
    ///Total value of new massages
    static var newMessagesCount : Int = 0
    ///Total value of chats
    static var chatsCount : Int = 0
    ///All of chats
    static var chats : [PSDChat] = [PSDChat]()
    ///Has some information been uploaded. False if the information has not yet been loaded and newMessagesCount and chatsCount can be equal to 0 because of this. Use to reflect the relevance of the information of newMessagesCount and chatsCount.
    static var hasInfo : Bool = false
    ///The main view controller. nil - if chat was closed.
    weak static var mainController : PyrusServiceDeskController?
    ///Updates user info - get chats list from server.
    @objc private static func updateUserInfo(){
        if(userId.count > 0){
            DispatchQueue.global().async {
                PSDGetChats.get(delegate: nil, needShowError: !hasInfo){
                    (chats:[PSDChat]?) in
                    DispatchQueue.main.async {
                        guard let chats = chats else{
                            return
                        }
                        hasInfo = true
                        if chats.count > 0{
                            for chat in chats{
                                if chat.messages.count > 0{
                                    setDidStartChatWithSupport()
                                    restartTimer()
                                    break
                                }
                            }
                            mainController?.passChanges(chats: chats)
                        }
                        
                    }
                }
            }
           
        }
        else{
            stopGettingInfo()
        }
    }
}
