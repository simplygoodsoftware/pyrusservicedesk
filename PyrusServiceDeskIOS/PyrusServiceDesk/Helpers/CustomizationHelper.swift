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
func PSD_KeyboardStyle(for controller: UIViewController) -> UIKeyboardAppearance {
    if #available(iOS 12.0, *) {
        switch controller.traitCollection.userInterfaceStyle {
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
func prepareWithCustomizationAlert(_ alert: UIAlertController) {
    alert.view.tintColor = PyrusServiceDesk.mainController?.customization?.attachmentMenuTextColor ?? UIColor.darkAppColor
}
