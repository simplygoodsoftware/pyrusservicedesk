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
    
    ///Chat title using to show in navigation Bar title (like attributedString)
    @objc public var chatAttributedTitle : NSAttributedString?

    @objc public var hideLeftBarButton = false
    
    @objc public var settingsViewController: UIViewController?
    
    @objc public var chooseCafeViewController: UIViewController?

    
    @objc public var infoTitle: String?
    
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
        
        if chatAttributedTitle != nil && chatAttributedTitle!.string.count>0{
            PyrusServiceDesk.mainController?.customization.chatAttributedTitle = chatAttributedTitle
        }
        
        PyrusServiceDesk.mainController?.customization.settingsViewController = settingsViewController
        
        PyrusServiceDesk.mainController?.customization.chooseCafeViewController = chooseCafeViewController
        
        PyrusServiceDesk.mainController?.customization.hideLeftBarButton = hideLeftBarButton
        
        PyrusServiceDesk.mainController?.customization.infoTitle = infoTitle
        
        if(userName != nil && userName!.count>1){
            PyrusServiceDesk.setUser(userName)
        }
        
        
    }
    
}
class PyrusServiceDeskCustomization {
    var chatTitle : String = ""
    var customColor : UIColor?
    var welcomeMessage : String = ""
    var customAvatar : UIImage?
    var chatAttributedTitle : NSAttributedString?
    var hideLeftBarButton = false
    var settingsViewController: UIViewController?
    var infoTitle: String?
    var chooseCafeViewController: UIViewController?
}
class PyrusServiceDeskLogs{
    var logPath : String?
    var additionalText : String = ""
    var needDeleteAfter : Bool = false
}

func PSD_ChatAttribitesTitle() -> NSAttributedString?{
    return PyrusServiceDesk.mainController?.customization.chatAttributedTitle
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

func PSD_HideLeftBarButton() -> Bool{
    return PyrusServiceDesk.mainController?.customization.hideLeftBarButton ?? false
}
func PSD_SettingsViewController() -> UIViewController?{
    return PyrusServiceDesk.mainController?.customization.settingsViewController
}

func PSD_ChooseCafeViewController() -> UIViewController?{
    return PyrusServiceDesk.mainController?.customization.chooseCafeViewController
}

func PSD_InfoTitle() -> String?{
    return PyrusServiceDesk.mainController?.customization.infoTitle
}


