import UIKit

@objc public class PyrusServiceDesk: NSObject {
    private static let PSD_USER_START_CHAT_KEY = "PSDUserStartChat"
    public static var PSD_CLOSED_NOTIFICATION_NAME = "PyrusServiceDeskWasClosed"
    private static let SET_PUSH_TIME_INTEVAL = TimeInterval(5*60)
    private static let REFRESH_TIME_INTEVAL = TimeInterval(1*60)
    private static let REFRESH_MAX_COUNT = 20
    
    ///AppId needed for request
    static var clientId: String?
    private static var lastSetPushToken: Date?
    private static var lastRefreshes = [Date]()

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
    @objc private(set) static var  oneChat:Bool = true
    
      
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
        if let lastSetPushToken = lastSetPushToken{
            let difference = Date().timeIntervalSince(lastSetPushToken)
            if difference < SET_PUSH_TIME_INTEVAL{
                completion(PSDError.init(description: "Too many requests"))
                return
            }
        }
        guard let clientId = clientId, clientId.count > 0, clientId != "0"  else{
            completion(PSDError.init(description: "AppId is invalid"))
            EventsLogger.logEvent(.emptyClientId)
            return
        }
        PSDPushToken.send(token, completion: {
            error in
            completion(error)
            lastSetPushToken = Date()
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
        if !PyrusServiceDeskController.PSDIsOpen(){
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
    
    @objc public static var onAuthorizationFailed :  (() -> Void)?
    
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
    
    static var customUserId: String?
    static var securityKey: String?
    static var loggingEnabled: Bool = false
    
    ///Init PyrusServiceDesk with new clientId.
    ///- parameter clientId: clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
    ///- parameter loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
    @objc static public func createWith(_ clientId: String?, loggingEnabled: Bool = false)  {
        createWith(clientId, userId: nil, securityKey: nil, reset: false, loggingEnabled: loggingEnabled)
    }
    
    ///Init PyrusServiceDesk with new clientId.
    ///- parameter clientId: clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
    ///- parameter reset: If true, user will be reseted
    ///- parameter loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
    @objc static public func createWith(_ clientId: String?, reset: Bool, loggingEnabled: Bool = false) {
        createWith(clientId, userId: nil, securityKey: nil, reset: reset, loggingEnabled: loggingEnabled)
    }
    
    ///Init PyrusServiceDesk with new clientId.
    ///- parameter clientId: clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
    ///- parameter userId: userId of the user who is initializing service desk
    ///- parameter securityKey: security key of the user for safe initialization
    ///- parameter loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
    @objc static public func createWith(_ clientId: String?, userId: String?, securityKey: String?, loggingEnabled: Bool = false) {
        createWith(clientId, userId: userId, securityKey: securityKey, reset: false, loggingEnabled: loggingEnabled)
    }
    private static func createWith(_ clientId: String?, userId: String?, securityKey: String?, reset: Bool, loggingEnabled: Bool = false) {
        PyrusServiceDesk.loggingEnabled = loggingEnabled
        guard let clientId = clientId, clientId.count > 0 else {
            EventsLogger.logEvent(.emptyClientId)
            return
        }
        PyrusServiceDesk.clientId = clientId
        PyrusServiceDesk.securityKey = securityKey
        let needReloadUI = PyrusServiceDesk.customUserId != userId
        PyrusServiceDesk.customUserId = userId
        PyrusServiceDesk.createUserId(reset)
        if needReloadUI {
            PyrusServiceDesk.mainController?.updateTitleChat()
        }
    }
    
    @objc static public func refresh(onError: ((Error?) -> Void)? = nil) {
        if lastRefreshes.count >= REFRESH_MAX_COUNT{
            let lastRefresh = lastRefreshes[0]
            let difference = Date().timeIntervalSince(lastRefresh)
            if difference < REFRESH_TIME_INTEVAL{
                EventsLogger.logEvent(.tooManyRefresh)
                onError?(PSDError.init(description: "Too many requests"))
                return
            }
        }
        lastRefreshes.append(Date())
        if lastRefreshes.count > REFRESH_MAX_COUNT{
            lastRefreshes.remove(at: 0)
        }
        PyrusServiceDesk.mainController?.refreshChat(showFakeMessage: 0)
    }
    
    ///Scrolls chat to bottom, starts refreshing chat and shows fake message from support is psd is open.
    @objc static public func refreshFromPush(messageId: Int){
        guard PyrusServiceDeskController.PSDIsOpen() else{
            return
        }
        PyrusServiceDesk.mainController?.refreshChat(showFakeMessage: messageId)
    }
    @objc static public func present(_ viewController: UIViewController, animated: Bool, completion: (() -> Void)?){
        guard PyrusServiceDeskController.PSDIsOpen() else{
            return
            
        }
        if let topViewController = UIApplication.topViewController() as? PSDChatViewController{
            viewController.presentationController?.delegate = topViewController
        }
        UIApplication.topViewController()?.present(viewController, animated: animated, completion: completion)
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
        guard !PyrusServiceDeskController.PSDIsOpen() else {
            return
        }
        let userId : String
        if let existKey =   PSDMessagesStorage.pyrusUserDefaults()?.object(forKey: PSD_USER_ID_KEY) as? String, !reset{
            userId = existKey
        }else{
            userId = reset ? String.getUiqueString() : (UIDevice.current.identifierForVendor?.uuidString ?? String.getUiqueString())
            PSDMessagesStorage.pyrusUserDefaults()?.set(userId, forKey: PSD_USER_ID_KEY)
            PSDMessagesStorage.pyrusUserDefaults()?.set(false, forKey: PSD_WAS_CLOSE_INFO_KEY)
            PSDMessagesStorage.pyrusUserDefaults()?.synchronize()
        }
        PyrusServiceDesk.userId = userId
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

    private static func userDidStartChatKey() -> String{
        return PSD_USER_START_CHAT_KEY + "_" + userId
    }
    
    //MARK: get user info automatic
    ///The timer to get info from server(chats list) with reloadInterval.
    private static var timer :Timer?
    ///Create timer to get chats list from server.
    ///- parameter rightNow: Is need to fire timer.
    private static func startGettingInfo(rightNow:Bool){
        stopGettingInfo()
        if rightNow {
            updateUserInfo()
        }else{
            if let interval = getTimerInerval() {
                timer = Timer.scheduledTimer(timeInterval: interval, target: self, selector: #selector(updateUserInfo), userInfo:nil , repeats: false)
                if rightNow{
                    timer?.fire()
                }
            }
        }
    }
    private static func getTimerInerval() -> TimeInterval?{
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(), let date = pyrusUserDefaults.object(forKey: PSDChatViewController.userLastActivityKey()) as? Date{
            let difference = Date().timeIntervalSince(date)
            if difference <= PSDChatViewController.PSD_LAST_ACTIVITY_INTEVAL_MINUTE{
                return PSDChatViewController.REFRESH_TIME_INTEVAL_5_SECONDS
            } else if difference <=  PSDChatViewController.PSD_LAST_ACTIVITY_INTEVAL_5_MINUTES{
                return  PSDChatViewController.REFRESH_TIME_INTEVAL_15_SECONDS
            } else if difference <=  PSDChatViewController.PSD_LAST_ACTIVITY_INTEVAL_HOUR{
                return  PSDChatViewController.REFRESH_TIME_INTEVAL_1_MINUTE
            } else if difference <=  PSDChatViewController.PSD_LAST_ACTIVITY_INTEVAL_3_DAYS{
                return  PSDChatViewController.REFRESH_TIME_INTEVAL_3_MINUTES
            }
        }
        return nil
    }
    
    ///Set last user acivity date to NOW if date paramemeter is nil, returns true if setted
    static func setLastActivityDate(_ date: Date? = nil) -> Bool{
        print ("\(date)")
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(){
            if let newDate = date, let oldDate = pyrusUserDefaults.object(forKey: PSDChatViewController.userLastActivityKey()) as? Date{
                if oldDate.compare(newDate) == .orderedDescending || oldDate.compare(newDate) == .orderedSame{
                    return false
                }
            }
            pyrusUserDefaults.set(date ?? Date(), forKey: PSDChatViewController.userLastActivityKey())
            pyrusUserDefaults.synchronize()
            return true
        }
        return false
    }
    ///Restart PyrusServiceDesk.timer - move next fire to reloadInterval.
    static func restartTimer(){
        DispatchQueue.main.async {
            PyrusServiceDesk.startGettingInfo(rightNow: false)
        }
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
            restartTimer()
            DispatchQueue.global().async {
                PSDGetChats.get(delegate: nil, needShowError: !hasInfo){
                    (chats:[PSDChat]?) in
                    DispatchQueue.main.async {
                        guard let chats = chats else{
                            return
                        }
                        hasInfo = true
                        if chats.count > 0{
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
