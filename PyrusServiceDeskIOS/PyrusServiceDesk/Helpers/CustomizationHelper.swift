import Foundation
func PSD_ChatTitle() -> String {
    return PyrusServiceDesk.mainController?.customization?.chatTitle ?? ""
}
func PSD_WelcomeMessage() -> String {
    return PyrusServiceDesk.mainController?.customization?.welcomeMessage ?? ""
}
func PSD_StatusBarStyle(for controller: UIViewController) -> UIStatusBarStyle? {
    guard PyrusServiceDesk.mainController?.customization?.statusBarStyle != nil || PyrusServiceDesk.mainController?.customization?.statusBarStyleDark != nil  else {
        return nil
    }
    if #available(iOS 12.0, *) {
        switch controller.traitCollection.userInterfaceStyle {
        case .light, .unspecified:
            return PyrusServiceDesk.mainController?.customization?.statusBarStyle
        case .dark:
            return PyrusServiceDesk.mainController?.customization?.statusBarStyleDark
        @unknown default:
            return PyrusServiceDesk.mainController?.customization?.statusBarStyle
        }
    } else {
        return PyrusServiceDesk.mainController?.customization?.statusBarStyle
    }
}
func PSD_KeyboardStyle() -> UIKeyboardAppearance {
    if #available(iOS 13.0, *) {
        switch UITraitCollection.current.userInterfaceStyle {
        case .light, .unspecified:
            return PyrusServiceDesk.mainController?.customization?.keyboardAppearance ?? .default
        case .dark:
            return PyrusServiceDesk.mainController?.customization?.keyboardAppearanceDark ?? .default
        @unknown default:
            return PyrusServiceDesk.mainController?.customization?.keyboardAppearance ?? .default
        }
    } else {
        return .default
    }
}
func PSD_SystemFont(ofSize: CGFloat) -> UIFont {
    guard let fontName = PyrusServiceDesk.mainController?.customization?.customFontName else {
        return UIFont.systemFont(ofSize: ofSize)
    }
    return UIFont(name: fontName, size: ofSize) ?? UIFont.systemFont(ofSize: ofSize)
}
func PSD_SystemBoldFont(ofSize: CGFloat) -> UIFont {
    guard let fontName = PyrusServiceDesk.mainController?.customization?.customFontName else {
        return UIFont.boldSystemFont(ofSize: ofSize)
    }
    guard let font = UIFont(name: fontName, size: ofSize) else {
        return UIFont.boldSystemFont(ofSize: ofSize)
    }
    return font.bold() ?? UIFont.boldSystemFont(ofSize: ofSize)
}
func PSD_UserMassageBackgroundColor() -> UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.userMessageBackgroundColor else {
        return UIColor.appColor
    }
    return color
}
func PSD_UserMassageTextColor() -> UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.userTextColor else {
        guard let backColor = PyrusServiceDesk.mainController?.customization?.userMessageBackgroundColor else {
            return UIColor.appTextColor
        }
        return UIColor.getTextColor(for: backColor)
    }
    return color
}
func PSD_SupportMassageBackgroundColor() -> UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.supportMessageBackgroundColor else {
        return UIColor.psdLightGray
    }
    return color
}
func PSD_SupportMassageTextColor() -> UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.supportTextColor else {
        guard let backColor = PyrusServiceDesk.mainController?.customization?.supportMessageBackgroundColor else {
            return UIColor.psdLabel
        }
        return UIColor.getTextColor(for: backColor)
    }
    return color
}
func PSD_BarStyle(for controller: UIViewController) -> UIBarStyle? {
    if #available(iOS 12.0, *) {
        switch controller.traitCollection.userInterfaceStyle {
        case .light, .unspecified:
            return PyrusServiceDesk.mainController?.customization?.barStyle
        case .dark:
            return PyrusServiceDesk.mainController?.customization?.barStyleDark
        @unknown default:
            return PyrusServiceDesk.mainController?.customization?.barStyle
        }
    } else {
        return nil
    }
}
func getTextColorForTable() -> UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
        return .psdLabel
    }
    return UIColor.getTextColor(for: color)
}
func PSD_TextColorForInput() -> UIColor {
    let (_, textColor) = PSD_colorForInput()
    return textColor
}
///Returns back color and text color fror message input
func PSD_colorForInput() -> (UIColor, UIColor) {
    let style = PSD_KeyboardStyle()
    if let color = PyrusServiceDesk.mainController?.customization?.keyboardColor {
        return (color, UIColor.getTextColor(for: color))
    }
    
    switch style {
    case .dark:
        return (.black, .white)
    case .light:
        return (.white, .black)
    default:
        return (.psdBackground, .psdLabel)
    }
}
func prepareWithCustomizationAlert(_ alert: UIAlertController) {
    alert.view.tintColor = PyrusServiceDesk.mainController?.customization?.attachmentMenuTextColor ?? UIColor.darkAppColor
}
var PSD_grayViewColor: UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
        return PSD_grayDefaultColor
    }
    return color.isDarkColor ? UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA)
}
var PSD_lightGrayViewColor: UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
        return PSD_lightGrayDefaultColor
    }
    return color.isDarkColor ? UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA)
}

var PSD_grayInputColor: UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.keyboardColor else {
        return PSD_grayDefaultColor
    }
    return color.isDarkColor ? UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA)
}
var PSD_lightGrayInputColor: UIColor {
    guard let color = PyrusServiceDesk.mainController?.customization?.keyboardColor else {
        return PSD_lightGrayDefaultColor
    }
    return color.isDarkColor ? UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA)
}

private var PSD_grayDefaultColor: UIColor {
    return UIColor.themedColor(lightColor: UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA),darkColor: UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA))
}
private var PSD_lightGrayDefaultColor: UIColor {
    return UIColor.themedColor(lightColor: UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA),darkColor: UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA))
}
private let GRAY_VIEW_ALPHA: CGFloat = 0.1
private let LIGHT_GRAY_VIEW_ALPHA: CGFloat = 0.05
