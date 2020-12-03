import Foundation
@objc public class ServiceDeskConfiguration: NSObject{
    
    ///Chat title using to show in navigation Bar title
    @objc public var chatTitle: String?
    
    /// Customize color. If not set, the application tint color or blue is used.
    @objc public var themeColor: UIColor?
    
    ///A first message that user see in new chat. If not setted - user will not see welcome message.
    @objc public var welcomeMessage: String?
    
    ///A icon for support imageView in chat. Show when support user has no image or for welcome message. The default is DEFAULT_SUPPORT_ICON.
    @objc public var avatarForSupport: UIImage?
    
    ///A user name. The default is "Guest"
    @objc public var userName: String? {
        didSet {
            PyrusServiceDesk.setUser(userName)
        }
    }
    ///View to show in  chat navigation bar
    @objc public var chatTitleView: UIView?
    
    ///Custom UIBarButtonItem to show in right side of navigationBar. Default is nil.
    @objc public var customRightBarButtonItem: UIBarButtonItem?
    
    ///Custom UIBarButtonItem to show in left side of navigation Bar. Default value is nil. If nil there will be drawn back button. If specify custom left button, Pyrus ServiceDesk cannot be closed.
    @objc public var customLeftBarButtonItem: UIBarButtonItem?
    
    ///The view to show additional information under chat
    @objc public var infoView: PSDInfoView?
    
    ///The custom style of the device’s status bar.
    ///- parameter barStyle The custom style of the device’s status bar for light appearance.
    ///- parameter darkBarStyle The custom style of the device’s status bar for dark appearance.
    @objc public func setStatusBarStyle(_ barStyle: UIStatusBarStyle, _ darkBarStyle: UIStatusBarStyle) {
        statusBarStyle = barStyle
        statusBarStyleDark = darkBarStyle
    }
    ///The custom style of the device’s status bar.
    private(set) var statusBarStyle: UIStatusBarStyle?
    ///The custom style of the device’s status bar for dark appearance.
    private(set) var statusBarStyleDark: UIStatusBarStyle?
    
    ///The custom appearance style of the keyboard for the message input.
    ///- parameter barStyle The custom appearance style of the keyboard for the message input for light appearance.
    ///- parameter darkBarStyle The custom appearance style of the keyboard for the message input for dark appearance.
    @objc public func setKeyboardAppearance(_ keyboardAppearance: UIKeyboardAppearance, _ darkKeyboardAppearance: UIKeyboardAppearance) {
        self.keyboardAppearance = keyboardAppearance
        keyboardAppearanceDark = darkKeyboardAppearance
    }
    ///The custom appearance style of the keyboard for the message input.
    private(set) var keyboardAppearance: UIKeyboardAppearance?
    ///The custom appearance style of the keyboard for the message input for dark appearance.
    private(set) var keyboardAppearanceDark: UIKeyboardAppearance?
    
    ///The custom color for message input.
    @objc public func setKeyboardColor(_ color: UIColor) {
        keyboardColor = color
    }
    ///The custom color for message input.
    private(set) var keyboardColor: UIColor?
    
    ///The custom font name.
    ///- parameter fontName The custom font name for elements.
    @objc public func setFontName(_ fontName: String?) {
        guard fontName == nil || fontName?.count ?? 0 > 0 else {
            return
        }
        customFontName = fontName
    }
    ///The custom font name for all elements.
    private(set) var customFontName: String?
    
    ///The custom text color for user's messages. Default value is is nil. If was not settled, this color will be automatically calculated according to message background view color.
    @objc public func setUserTextColor(_ color: UIColor?) {
        userTextColor = color
    }
    ///The custom text color for user's messages.
    private(set) var userTextColor: UIColor?
    
    ///The custom background color for user's messages. The default value is equal to themeColor.
    @objc public func setUserMessageBackgroundColor(_ color: UIColor?) {
        userMessageBackgroundColor = color
    }
    ///The custom background color for user's messages. The default value is equal to themeColor.
    private(set) var userMessageBackgroundColor: UIColor?
    
    ///The custom text color for support's messages. Default value is is UIColor.label.
    @objc public func setSupportTextColor(_ color: UIColor?) {
        supportTextColor = color
    }
    ///The custom text color for support's messages. Default value is is UIColor.label.
    private(set) var supportTextColor: UIColor?
    
    ///The custom background color for support's  messages. The default value is UIColor.secondarySystemBackground.
    @objc public func setSupportMessageBackgroundColor(_ color: UIColor?) {
        supportMessageBackgroundColor = color
    }
    ///The custom background color for support's  messages. The default value is UIColor.secondarySystemBackground.
    private(set) var supportMessageBackgroundColor: UIColor?
    
    ///The custom color for navigation title. Can be used only if chatTitleView was not setted. Otherwise chatTitleView has highest priority.
    @objc public func setChatTitleColor(_ color: UIColor?) {
        chatTitleColor = color
    }
    ///The custom color for navigation title. Can be used only if chatTitleView was not setted. Otherwise chatTitleView has highest priority.
    private(set) var chatTitleColor: UIColor?
    
    ///The custom style of navigation bar.
    ///- parameter barStyle The custom style of navigation bar for light appearance.
    ///- parameter darkBarStyle The custom style of navigation bar for dark appearance.
    @objc public func setBarStyle(_ barStyle: UIBarStyle, _ darkBarStyle: UIBarStyle) {
        self.barStyle = barStyle
        barStyleDark = darkBarStyle
    }
    ///The custom style of navigation bar.
    private(set) var barStyle: UIBarStyle?
    ///The custom style of navigation bar for dark appearance.
    private(set) var barStyleDark: UIBarStyle?
    
    ///The custom color of navigation bar.
    @objc public func setBarColor(_ color: UIColor?) {
        customBarColor = color
    }
    ///The custom color of navigation bar.
    private(set) var customBarColor: UIColor?
    
    ///The custom color of back button tint. Can be used only if customLeftBarButtonItem was not setted. Otherwise customLeftBarButtonItem has highest priority.
    ///The custom color of navigation bar.
    @objc public func setBarButtonColor(_ color: UIColor?) {
        barButtonTintColor = color
    }
    private(set) var barButtonTintColor: UIColor?
    
    ///The custom color for chat background.
    @objc public func setBackgroundColor(_ color: UIColor?) {
        customBackgroundColor = color
    }
    ///The custom color for chat background.
    private(set) var customBackgroundColor: UIColor?
    
    ///The custom text color for menu for attachment choosing.
    @objc public func setAttachmentMenuTextColor(_ color: UIColor?) {
        attachmentMenuTextColor = color
    }
    ///The custom text color for menu for attachment choosing.
    var attachmentMenuTextColor: UIColor?
    
    ///The custom color for button for attachment choosing.
    @objc public func setAttachmentButtonColor(_ color: UIColor?) {
        addAttachmentButtonColor = color
    }
    ///The custom color for button for attachment choosing.
    var addAttachmentButtonColor: UIColor?
    
    ///The custom color for button for sending message.
    @objc public func setSendButtonColor(_ color: UIColor?) {
        sendButtonColor = color
    }
    ///The custom color for button for sending message.
    var sendButtonColor: UIColor?
}
