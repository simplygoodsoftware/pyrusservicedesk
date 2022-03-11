import Foundation
@objc public class ServiceDeskConfiguration: NSObject{
    @objc private override init() {
        super.init()
    }
    
    ///Chat title using to show in navigation Bar title
    private(set) var chatTitle: String?
    
    /// Customize color. If not set, the application tint color or blue is used.
    private(set) var themeColor: UIColor?
    
    ///A first message that user see in new chat. If not setted - user will not see welcome message.
    private(set) public var welcomeMessage: String?
    
    ///A icon for support imageView in chat. Show when support user has no image or for welcome message. The default is DEFAULT_SUPPORT_ICON.
    private(set) var avatarForSupport: UIImage?
    
    ///A user name. The default is "Guest"
    private(set) var userName: String? {
        didSet {
            PyrusServiceDesk.setUser(userName)
        }
    }
    
    ///View to show in  chat navigation bar
    private(set) var chatTitleView: UIView?
    
    ///Custom UIBarButtonItem to show in right side of navigationBar. Default is nil.
    private(set) var customRightBarButtonItem: UIBarButtonItem?
    
    ///Custom UIBarButtonItem to show in left side of navigation Bar. Default value is nil. If nil there will be drawn back button. If specify custom left button, Pyrus ServiceDesk cannot be closed.
    private(set) var customLeftBarButtonItem: UIBarButtonItem?
    
    ///The view to show additional information under chat
    private(set) var infoView: PSDInfoView?
    
    ///The custom style of the device’s status bar.
    private(set) var statusBarStyle: UIStatusBarStyle?
    ///The custom style of the device’s status bar for dark appearance.
    private(set) var statusBarStyleDark: UIStatusBarStyle?
    
    ///The custom appearance style of the keyboard for the message input.
    private(set) var keyboardAppearance: UIKeyboardAppearance?
    ///The custom appearance style of the keyboard for the message input for dark appearance.
    private(set) var keyboardAppearanceDark: UIKeyboardAppearance?
    
    ///The custom color for message input.
    private(set) var keyboardColor: UIColor?
    
    ///The custom font name for all elements.
    private(set) var customFontName: String?
    
    ///The custom text color for user's messages.
    private(set) var userTextColor: UIColor?
    
    ///The custom background color for user's messages. The default value is equal to themeColor.
    private(set) var userMessageBackgroundColor: UIColor?
    
    ///The custom text color for support's messages. Default value is is UIColor.label.
    private(set) var supportTextColor: UIColor?
    
    ///The custom background color for support's  messages. The default value is UIColor.secondarySystemBackground.
    private(set) var supportMessageBackgroundColor: UIColor?
    
    ///The custom color for navigation title. Can be used only if chatTitleView was not setted. Otherwise chatTitleView has highest priority.
    private(set) var chatTitleColor: UIColor?
    
    ///The custom style of navigation bar.
    private(set) var barStyle: UIBarStyle?
    ///The custom style of navigation bar for dark appearance.
    private(set) var barStyleDark: UIBarStyle?
    
    ///The custom color of navigation bar.
    private(set) var customBarColor: UIColor?
    
    private(set) var barButtonTintColor: UIColor?
    
    ///The custom color for chat background.
    private(set) var customBackgroundColor: UIColor?
    
    ///The custom text color for menu for attachment choosing.
    private(set) var attachmentMenuTextColor: UIColor?
    
    ///The custom color for button for attachment choosing.
    private(set) var addAttachmentButtonColor: UIColor?
    
    ///The custom color for button for sending message.
    private(set) var sendButtonColor: UIColor?
    
    @objc(ServiceDeskConfigurationBuilder) public class Builder: NSObject {
        private var configuration = ServiceDeskConfiguration()
        
        @discardableResult
        ///Chat title using to show in navigation Bar title
        @objc public func setChatTitle(_ chatTitle: String?) -> Builder {
            configuration.chatTitle = chatTitle
            return self
        }
        
        @discardableResult
        /// Customize color. If not set, the application tint color or blue is used.
        @objc public func setThemeColor(_ themeColor: UIColor?) -> Builder {
            configuration.themeColor = themeColor
            return self
        }
        
        @discardableResult
        ///A first message that user see in new chat. If not setted - user will not see welcome message.
        @objc public func setWelcomeMessage(_ welcomeMessage: String?) -> Builder {
            configuration.welcomeMessage = welcomeMessage
            return self
        }
        
        @discardableResult
        ///A icon for support imageView in chat. Show when support user has no image or for welcome message. The default is DEFAULT_SUPPORT_ICON.
        @objc public func setAvatarForSupport(_ avatarForSupport: UIImage?) -> Builder {
            configuration.avatarForSupport = avatarForSupport
            return self
        }
        
        @discardableResult
        ///A user name. The default is "Guest"
        @objc public func setUserName(_ userName: String?) -> Builder {
            configuration.userName = userName
            return self
        }
        
        @discardableResult
        ///View to show in  chat navigation bar
        @objc public func setChatTitleView(_ chatTitleView: UIView?) -> Builder {
            configuration.chatTitleView = chatTitleView
            return self
        }
        
        @discardableResult
        ///Custom UIBarButtonItem to show in right side of navigationBar. Default is nil.
        @objc public func setCustomRightBarButtonItem(_ customRightBarButtonItem: UIBarButtonItem?) -> Builder {
            configuration.customRightBarButtonItem = customRightBarButtonItem
            return self
        }
        
        @discardableResult
        ///Custom UIBarButtonItem to show in left side of navigation Bar. Default value is nil. If nil there will be drawn back button. If specify custom left button, Pyrus ServiceDesk cannot be closed.
        @objc public func setCustomLeftBarButtonItem(_ customLeftBarButtonItem: UIBarButtonItem?) -> Builder {
            configuration.customLeftBarButtonItem = customLeftBarButtonItem
            return self
        }
        
        @discardableResult
        ///The view to show additional information under chat
        @objc public func setInfoView(_ infoView: PSDInfoView?) -> Builder {
            configuration.infoView = infoView
            return self
        }
        
        @discardableResult
        ///The custom style of the device’s status bar.
        ///- parameter barStyle The custom style of the device’s status bar for light appearance.
        ///- parameter darkBarStyle The custom style of the device’s status bar for dark appearance.
        @objc(setStatusBarStyle: darkBarStyle:) public func setStatusBarStyle(_ barStyle: UIStatusBarStyle, _ darkBarStyle: UIStatusBarStyle) -> Builder {
            configuration.statusBarStyle = barStyle
            configuration.statusBarStyleDark = darkBarStyle
            return self
        }
        
        @discardableResult
        ///The custom appearance style of the keyboard for the message input.
        ///- parameter barStyle The custom appearance style of the keyboard for the message input for light appearance.
        ///- parameter darkBarStyle The custom appearance style of the keyboard for the message input for dark appearance.
        @objc(setKeyboardAppearance: darkKeyboardAppearance:) public func setKeyboardAppearance(_ keyboardAppearance: UIKeyboardAppearance, _ darkKeyboardAppearance: UIKeyboardAppearance) -> Builder {
            configuration.keyboardAppearance = keyboardAppearance
            configuration.keyboardAppearanceDark = darkKeyboardAppearance
            return self
        }
        
        @discardableResult
        ///The custom color for message input.
        @objc public func setKeyboardColor(_ color: UIColor) -> Builder {
            configuration.keyboardColor = color
            return self
        }
        
        @discardableResult
        ///The custom font name.
        ///- parameter fontName The custom font name for elements.
        @objc public func setFontName(_ fontName: String?) -> Builder {
            guard fontName == nil || fontName?.count ?? 0 > 0 else {
                return self
            }
            configuration.customFontName = fontName
            return self
        }
        
        @discardableResult
        ///The custom text color for user's messages. Default value is is nil. If was not settled, this color will be automatically calculated according to message background view color.
        @objc public func setUserTextColor(_ color: UIColor?) -> Builder {
            configuration.userTextColor = color
            return self
        }
        
        @discardableResult
        ///The custom background color for user's messages. The default value is equal to themeColor.
        @objc public func setUserMessageBackgroundColor(_ color: UIColor?) -> Builder {
            configuration.userMessageBackgroundColor = color
            return self
        }
        
        @discardableResult
        ///The custom text color for support's messages. Default value is is UIColor.label.
        @objc public func setSupportTextColor(_ color: UIColor?) -> Builder {
            configuration.supportTextColor = color
            return self
        }
        
        @discardableResult
        ///The custom color for navigation title. Can be used only if chatTitleView was not setted. Otherwise chatTitleView has highest priority.
        ///The custom background color for support's  messages. The default value is UIColor.secondarySystemBackground.
        @objc public func setSupportMessageBackgroundColor(_ color: UIColor?) -> Builder {
            configuration.supportMessageBackgroundColor = color
            return self
        }
        
        @discardableResult
        ///The custom color for navigation title. Can be used only if chatTitleView was not setted. Otherwise chatTitleView has highest priority.
        @objc public func setChatTitleColor(_ color: UIColor?) -> Builder {
            configuration.chatTitleColor = color
            return self
        }
        
        @discardableResult
        ///The custom style of navigation bar.
        ///- parameter barStyle The custom style of navigation bar for light appearance.
        ///- parameter darkBarStyle The custom style of navigation bar for dark appearance.
        @objc(setToolbarStyle: darkBarStyle:) public func setToolbarStyle(_ barStyle: UIBarStyle, _ darkBarStyle: UIBarStyle) -> Builder {
            configuration.barStyle = barStyle
            configuration.barStyleDark = darkBarStyle
            return self
        }
        
        @discardableResult
        ///The custom color of navigation bar.
        @objc public func setToolbarColor(_ color: UIColor?) -> Builder {
            configuration.customBarColor = color
            return self
        }
        
        @discardableResult
        ///The custom color of back button tint. Can be used only if customLeftBarButtonItem was not setted. Otherwise customLeftBarButtonItem has highest priority.
        ///The custom color of navigation bar.
        @objc public func setToolbarButtonColor(_ color: UIColor?) -> Builder {
            configuration.barButtonTintColor = color
            return self
        }
        
        @discardableResult
        ///The custom color for chat background.
        @objc public func setBackgroundColor(_ color: UIColor?) -> Builder {
            configuration.customBackgroundColor = color
            return self
        }
        
        @discardableResult
        ///The custom text color for menu for attachment choosing.
        @objc public func setAttachmentMenuTextColor(_ color: UIColor?) -> Builder {
            configuration.attachmentMenuTextColor = color
            return self
        }
        
        @discardableResult
        ///The custom color for button for attachment choosing.
        @objc public func setAttachmentMenuButtonColor(_ color: UIColor?) -> Builder {
            configuration.addAttachmentButtonColor = color
            return self
        }
        
        @discardableResult
        ///The custom color for button for sending message.
        @objc public func setSendButtonColor(_ color: UIColor?) -> Builder {
            configuration.sendButtonColor = color
            return self
        }
        
        @objc public func build() -> ServiceDeskConfiguration {
            return configuration
        }
    }
}
