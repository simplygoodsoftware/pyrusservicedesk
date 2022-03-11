import Foundation
///The class in which contains helpful functions for customizing the interface.
///Returns colors and customization settings based on PyrusServiceDeskController values
class CustomizationHelper {
    static var chatTitle: String {
        return PyrusServiceDesk.mainController?.customization?.chatTitle ?? ""
    }
    static var welcomeMessage: String {
        return PyrusServiceDesk.mainController?.customization?.welcomeMessage ?? ""
    }
    static func statusBarStyle(for controller: UIViewController) -> UIStatusBarStyle? {
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
    static var keyboardStyle: UIKeyboardAppearance {
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
    static func systemFont(ofSize: CGFloat) -> UIFont {
        guard let fontName = PyrusServiceDesk.mainController?.customization?.customFontName else {
            return UIFont.systemFont(ofSize: ofSize)
        }
        return UIFont(name: fontName, size: ofSize) ?? UIFont.systemFont(ofSize: ofSize)
    }
    static func systemBoldFont(ofSize: CGFloat) -> UIFont {
        guard let fontName = PyrusServiceDesk.mainController?.customization?.customFontName else {
            return UIFont.boldSystemFont(ofSize: ofSize)
        }
        guard let font = UIFont(name: fontName, size: ofSize) else {
            return UIFont.boldSystemFont(ofSize: ofSize)
        }
        return font.bold() ?? UIFont.boldSystemFont(ofSize: ofSize)
    }
    static var userMassageBackgroundColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.userMessageBackgroundColor else {
            return UIColor.appColor
        }
        return color
    }
    static var userMassageTextColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.userTextColor else {
            guard let backColor = PyrusServiceDesk.mainController?.customization?.userMessageBackgroundColor else {
                return UIColor.appTextColor
            }
            return UIColor.getTextColor(for: backColor)
        }
        return color
    }
    static var supportMassageBackgroundColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.supportMessageBackgroundColor else {
            return UIColor.psdLightGray
        }
        return color
    }
    static var supportMassageTextColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.supportTextColor else {
            guard let backColor = PyrusServiceDesk.mainController?.customization?.supportMessageBackgroundColor else {
                return UIColor.psdLabel
            }
            return UIColor.getTextColor(for: backColor)
        }
        return color
    }
    static var barStyle: UIBarStyle? {
        if #available(iOS 13.0, *) {
            switch UITraitCollection.current.userInterfaceStyle {
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
    static var textColorForTable: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
            return .psdLabel
        }
        return UIColor.getTextColor(for: color)
    }
    static var textColorForInput: UIColor {
        let (_, textColor) = colorsForInput
        return textColor
    }
    ///Returns back color and text color fror message input
    static var colorsForInput: (UIColor, UIColor) {
        let style = keyboardStyle
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
    static func prepareWithCustomizationAlert(_ alert: UIAlertController) {
        alert.view.tintColor = PyrusServiceDesk.mainController?.customization?.attachmentMenuTextColor ?? UIColor.darkAppColor
    }
    static var grayViewColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
            return grayDefaultColor
        }
        return color.isDarkColor ? UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA)
    }
    static var lightGrayViewColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor else {
            return lightGrayDefaultColor
        }
        return color.isDarkColor ? UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA)
    }

    static var grayInputColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.keyboardColor else {
            return grayDefaultColor
        }
        return color.isDarkColor ? UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA)
    }
    static var lightGrayInputColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.keyboardColor else {
            return lightGrayDefaultColor
        }
        return color.isDarkColor ? UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA) : UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA)
    }

    private static var grayDefaultColor: UIColor {
        return UIColor.themedColor(lightColor: UIColor.black.withAlphaComponent(GRAY_VIEW_ALPHA),darkColor: UIColor.white.withAlphaComponent(GRAY_VIEW_ALPHA))
    }
    private static var lightGrayDefaultColor: UIColor {
        return UIColor.themedColor(lightColor: UIColor.black.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA),darkColor: UIColor.white.withAlphaComponent(LIGHT_GRAY_VIEW_ALPHA))
    }
}
private let GRAY_VIEW_ALPHA: CGFloat = 0.1
private let LIGHT_GRAY_VIEW_ALPHA: CGFloat = 0.05
