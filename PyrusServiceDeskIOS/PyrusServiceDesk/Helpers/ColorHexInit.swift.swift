import UIKit

@objc extension UIColor {
    
    /// Может принимать в себя HEX в разных форматах и если правильно прописать формат - создастся соответствующий объект UIColor
    /// - Parameter hex:"#XXXXXX", "#XXXXXXXX", "XXXXXX", "XXXXXXXX"
    ///
    /// Что очевидно, можно передавать как 3, так и 4 канальные цвета
    ///
    /// Если передать 3 канальный, alpha станет равной 1
    ///
    /// Регистр букв значения не имеет
    @objc public convenience init?(hex: String?) {
        guard var hexColor = hex else { return nil }
        let r, g, b, a: CGFloat
        if hexColor.hasPrefix("#") {
            let start = hexColor.index(hexColor.startIndex, offsetBy: 1)
            hexColor = String(hexColor[start...])
        }
        switch hexColor.count {
        case 8:
            let scanner = Scanner(string: hexColor)
            var hexNumber: UInt64 = 0

            if scanner.scanHexInt64(&hexNumber) {
                r = CGFloat((hexNumber & 0xff000000) >> 24) / 255
                g = CGFloat((hexNumber & 0x00ff0000) >> 16) / 255
                b = CGFloat((hexNumber & 0x0000ff00) >> 8) / 255
                a = CGFloat(hexNumber & 0x000000ff) / 255

                self.init(red: r, green: g, blue: b, alpha: a)
                return
            }
        case 6:
            let scanner = Scanner(string: hexColor)
            var hexNumber: UInt64 = 0

            if scanner.scanHexInt64(&hexNumber) {
                r = CGFloat((hexNumber & 0xff0000) >> 16) / 255
                g = CGFloat((hexNumber & 0x00ff00) >> 8) / 255
                b = CGFloat((hexNumber & 0x0000ff) >> 0) / 255

                self.init(red: r, green: g, blue: b, alpha: 1.0)
                return
            }
        default:
            return nil
        }
        return nil
    }
    
    @objc func toHexString() -> String {
        var r:CGFloat = 0
        var g:CGFloat = 0
        var b:CGFloat = 0
        var a:CGFloat = 0

        getRed(&r, green: &g, blue: &b, alpha: &a)

        let rgb:Int = (Int)(r*255)<<16 | (Int)(g*255)<<8 | (Int)(b*255)<<0

        return String(format:"#%06x", rgb)
    }
}


