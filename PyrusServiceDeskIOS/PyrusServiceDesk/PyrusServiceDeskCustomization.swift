import Foundation
@objc public class ServiceDeskConfiguration: NSObject{
    ///Chat title using to show in navigation Bar title
    @objc public var chatTitle : String?
    /// Customize color. If not set, the application tint color or blue is used.
    @objc public var themeColor : UIColor?
    ///A first message that user see in new chat. If not setted - user will not see welcome message.
    @objc public var welcomeMessage : String?
    
    ///A icon for support imageView in chat. Show when support user has no image or for welcome message. The default is DEFAULT_SUPPORT_ICON.
    @objc public var avatarForSupport : UIImage?
    
    ///A user name. The default is "Guest"
    @objc public var userName : String?
    
    ///View to show in  chat navigation bar
    @objc public var chatTitleView :UIView?
    
    ///Custom UIBarButtonItem to show in right side of navigationBar. Default is nil.
    @objc public var customRightBarButtonItem: UIBarButtonItem?
    
    ///Custom UIBarButtonItem to show in left side of navigation Bar. Default value is nil. If nil there will be drawn back button. If specify custom left button, Pyrus ServiceDesk cannot be closed.
    @objc public var customLeftBarButtonItem: UIBarButtonItem?
    ///The view to show additional information under chat
    @objc public var infoView: PSDInfoView?

    @objc public var userId: String?

    @objc public var secretKey: String?

    @objc public var onAuthorizationFailed :  (() -> Void)?


    func buildCustomization(){
        if avatarForSupport != nil{
            PyrusServiceDesk.mainController?.customization.customAvatar = avatarForSupport!
        }
        if welcomeMessage != nil && welcomeMessage!.count>1{
             PyrusServiceDesk.mainController?.customization.welcomeMessage = welcomeMessage!
        }
        if themeColor != nil{
            PyrusServiceDesk.mainController?.customization.customColor = themeColor
        }
        if chatTitle != nil && chatTitle!.count>1{
            PyrusServiceDesk.mainController?.customization.chatTitle = chatTitle!
        }
        
        PyrusServiceDesk.mainController?.customization.chatTitleView = chatTitleView
        
        PyrusServiceDesk.mainController?.customization.customRightBarButtonItem = customRightBarButtonItem
        
        PyrusServiceDesk.mainController?.customization.customLeftBarButtonItem = customLeftBarButtonItem

        PyrusServiceDesk.mainController?.customization.infoView = infoView

        if(userName != nil && userName!.count>1){
            PyrusServiceDesk.setUser(userName)
        }
        PyrusServiceDesk.onAuthorizationFailed = onAuthorizationFailed
        PyrusServiceDesk.secretKey = secretKey
        PyrusServiceDesk.customUserId = userId
        
    }
    
}
class PyrusServiceDeskCustomization {
    var chatTitle : String = ""
    var customColor : UIColor?
    var welcomeMessage : String = ""
    var customAvatar : UIImage?
    var chatTitleView : UIView?{
        didSet(oldValue){
            oldValue?.removeFromSuperview()
        }
    }
    var customRightBarButtonItem: UIBarButtonItem?
    var customLeftBarButtonItem: UIBarButtonItem?
    var infoView: PSDInfoView?
}
class PyrusServiceDeskLogs{
    var logPath : String?
    var additionalText : String = ""
    var needDeleteAfter : Bool = false
}

func PSD_ChatTitleView() -> UIView? {
    return PyrusServiceDesk.mainController?.customization.chatTitleView
}

func PSD_ChatTitle()->String{
    return PyrusServiceDesk.mainController?.customization.chatTitle ?? ""
}
func PSD_CustomColor()->UIColor?{
    return PyrusServiceDesk.mainController?.customization.customColor
}

func PSD_WelcomeMessage()->String{
    return PyrusServiceDesk.mainController?.customization.welcomeMessage ?? ""
}
func PSD_CustomAvatar()->UIImage?{
    return PyrusServiceDesk.mainController?.customization.customAvatar
}
func PSD_СustomRightBarButtonItem() -> UIBarButtonItem?{
    return PyrusServiceDesk.mainController?.customization.customRightBarButtonItem
}
func PSD_СustomLeftBarButtonItem() -> UIBarButtonItem?{
    return PyrusServiceDesk.mainController?.customization.customLeftBarButtonItem
}
func PSD_InfoView() -> PSDInfoView?{
    return PyrusServiceDesk.mainController?.customization.infoView
}
