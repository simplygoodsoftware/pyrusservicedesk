import Foundation
///The class in which contains helpful functions for customizing the interface.
///Returns colors and customization settings based on PyrusServiceDeskController values
class CustomizationHelper {
    static var chatTitle: String {
        return PyrusServiceDesk.mainController?.customization?.chatTitle ?? ""
    }
    static var welcomeMessage: String {
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        if let welomeMessage = PyrusServiceDesk.clients.first(where: { $0.clientId == clientId })?.welcomeMessage {
            return welomeMessage
        }
        return PyrusServiceDesk.mainController?.customization?.welcomeMessage ?? ""
    }
    
    static var ratingSettings: PSDRatingSettings {
        let clientId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        if let ratingSettings = PyrusServiceDesk.clients.first(where: { $0.clientId == clientId })?.ratingSettings {
            return ratingSettings
        }
        return PSDRatingSettings(
            size: 5,
            type: RatingType.smile.rawValue,
            ratingTextValues: RatingType.smile.rateArray(size: 5)
        )
    }
    
    static var customLocale: String? {
        return PyrusServiceDesk.mainController?.customization?.customLocale
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
    
    static var supportTrackColor: UIColor {
        return supportMassageBackgroundColor.isDarkColor ? (UIColor(hex: "#E3E5E84D") ?? .white).withAlphaComponent(0.3) : UIColor.trackColor
    }
    
    static var userTrackColor: UIColor {
        return userMassageBackgroundColor.isDarkColor ? (UIColor(hex: "#E3E5E84D") ?? .white).withAlphaComponent(0.3) : UIColor.trackColor
    }
    
    static var sendButtonColor: UIColor {
        if let color = PyrusServiceDesk.mainController?.customization?.sendButtonColor {
            return color
        } else if let color = PyrusServiceDesk.mainController?.customization?.barButtonTintColor {
            return color
        }
        return UIColor.appColor
    }
    
    static var navigationBarColor: UIColor {
        if let color = PyrusServiceDesk.mainController?.customization?.customBarColor {
            return color
        } else if let color = PyrusServiceDesk.mainController?.customization?.customBackgroundColor {
            return color
        }
        return .navBarColor
    }
    
    static var scrollButtonColor: UIColor {
        return colorsForInput.0
    }
    
    static var colorForChatTitle: UIColor {
        if let textColor = PyrusServiceDesk.mainController?.customization?.chatTitleColor {
            return textColor
        }
        if let color = PyrusServiceDesk.mainController?.customization?.customBarColor {
            return UIColor.getTextColor(for: color)
        }
        return UIColor.getTextColor(for: navigationBarColor)
    }
    
    static var previewBakcgroundColor: UIColor {
        return CustomizationHelper.userMassageBackgroundColor.mixed(with: CustomizationHelper.userMassageTextColor, amount: 0.1)
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
    
    static var recordImagesColors: UIColor {
        return PyrusServiceDesk.mainController?.customization?.themeColor ?? PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? CustomizationHelper.colorsForInput.1
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

private extension UIColor {
    static let trackColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#FFFFFF4D")?.withAlphaComponent(0.3) ?? .white
        default:
            return UIColor(hex: "#0000001A")?.withAlphaComponent(0.1) ?? .darkAppColor
        }
    }
}


extension UIColor {
    /// Смешивает текущий цвет с другим (`other`) в заданной доле (`amount`).
    /// `amount = 0` — берём только `self`, `amount = 1` — только `other`.
    func mixed(with other: UIColor, amount: CGFloat) -> UIColor {
        let t = max(0, min(1, amount))
        
        var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
        var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
        
        // Приводим оба цвета к sRGB и извлекаем компоненты
        guard self.getRed(&r1, green: &g1, blue: &b1, alpha: &a1),
              other.getRed(&r2, green: &g2, blue: &b2, alpha: &a2) else {
            return self
        }
        
        // Линейная интерполяция
        let r = r1 * (1 - t) + r2 * t
        let g = g1 * (1 - t) + g2 * t
        let b = b1 * (1 - t) + b2 * t
        let a = a1 * (1 - t) + a2 * t
        
        return UIColor(red: r, green: g, blue: b, alpha: a)
    }
}
