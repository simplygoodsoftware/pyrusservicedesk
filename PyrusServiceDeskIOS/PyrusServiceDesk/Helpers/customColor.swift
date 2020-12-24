
import UIKit
///Is value beetwin 1.0...0.0 to make color darker
let DARKER_COLOR_AMOUNT : CGFloat = 0.75

extension UIColor {
    ///The main application color - color for user messages.
    static var appColor: UIColor {
        guard let color = PyrusServiceDesk.mainController?.customization?.themeColor else {
            let sharedApp = UIApplication.shared
            return sharedApp.delegate?.window??.tintColor ?? defaultColor
        }
        return color
    }
    ///The main application color - color for all buttons.
    static var darkAppColor : UIColor{
        if #available(iOS 13.0, *) {
            return dynamicDarkAppColor
        }
        return UIColor.appColor.darkerColor()
    }
    @available(iOS 13.0, *)
       private static var dynamicDarkAppColor = UIColor { (traitCollection: UITraitCollection) -> UIColor in
           switch traitCollection.userInterfaceStyle {
           case
             .unspecified,
             .light: return UIColor.appColor
           case .dark: return UIColor.appColor.darkerColor()
           @unknown default:
                return UIColor.appColor
        }
       }
    private func darkerColor() -> UIColor{
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if(self.getHue(&h, saturation: &s, brightness: &b, alpha: &a)){
            return UIColor(hue: h, saturation: s, brightness: b*DARKER_COLOR_AMOUNT, alpha: 1.0)
        }
        return self
    }
    
    ///The color (white or black) for text on view that colored in appColor
    static var appTextColor: UIColor {
        if #available(iOS 13.0, *) {
            return dynamicAppTextColor
        }
        return appColor.isDarkColor ? .white : .black
    }
    
    ///The color (white or black) for text on view that colored in appColor
    static var psdGray5Color: UIColor {
        if #available(iOS 13.0, *) {
            return .systemGray5
        }
        return #colorLiteral(red: 0.8980392157, green: 0.8980392157, blue: 0.9176470588, alpha: 1)
    }
    
    @available(iOS 13.0, *)
    private static var dynamicAppTextColor = UIColor { (traitCollection: UITraitCollection) -> UIColor in
        switch traitCollection.userInterfaceStyle {
        case
          .unspecified,
          .light: return appColor.isDarkColor ? .white : .black
        case .dark: return appColor.isDarkColor ? .white : .black
        @unknown default:
            return appColor.isDarkColor ? .white : .black
        }
    }
    
    ///Detect is color is dark. Use to detect text color: someColor.isDarkColor ? .white : .black
    ///Relative luminance: https://en.wikipedia.org/wiki/Relative_luminance
    var isDarkColor: Bool {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        self.getRed(&r, green: &g, blue: &b, alpha: &a)
        let luminance = 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b)
        return  luminance < 0.50
    }
    func toLinear(_ colorComponent:CGFloat)->CGFloat{
        return colorComponent<0.0045 ? colorComponent/12.92 : pow((colorComponent+0.055)/1.055, 2.4)
    }
    static let defaultColor: UIColor = #colorLiteral(red: 0, green: 0.4784313725, blue: 1, alpha: 1)
    static var goldColor: UIColor = #colorLiteral(red: 0.9568627451, green: 0.7490196078, blue: 0.1882352941, alpha: 1)
    
    static func getTextColor(for color: UIColor) -> UIColor {
        return color.isDarkColor ? .white : .black
    }
}
