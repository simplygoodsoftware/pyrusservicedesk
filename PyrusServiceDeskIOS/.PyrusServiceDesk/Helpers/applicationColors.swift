//
//  applicationColors.swift
//  PyrusServiceDesk
//
//  Created by  Галина Муравьева on 20/08/2019.
//  Copyright © 2019  Галина Муравьева. All rights reserved.
//

import Foundation
extension UIColor{
    static var psdLightGray : UIColor {
        if #available(iOS 13, * ) {
            return .secondarySystemBackground
        }
        return #colorLiteral(red: 0.9490196078, green: 0.9490196078, blue: 0.968627451, alpha: 1)
    }
    static var psdGray : UIColor {
        if #available(iOS 13, *) {
            return .systemGray
        }
        return #colorLiteral(red: 0.5568627451, green: 0.5568627451, blue: 0.5764705882, alpha: 1)
    }
    static var psdSeparator: UIColor {
        if #available(iOS 13, *) {
            return .separator
        }
        return #colorLiteral(red: 0.2352941176, green: 0.2352941176, blue: 0.262745098, alpha: 0.29)
    }
    static var psdLabel: UIColor {
        if #available(iOS 13, *) {
            return .label
        }
        return #colorLiteral(red: 0, green: 0, blue: 0, alpha: 1)
    }
    static var psdBackground: UIColor {
        if #available(iOS 13, *) {
            return .systemBackground
        }
        return #colorLiteral(red: 1, green: 1, blue: 1, alpha: 1)
    }
    
}
extension UIColor {
    static func themedColor(lightColor: UIColor, darkColor: UIColor) -> UIColor {
        return {
            if #available(iOS 13.0, *) {
                return UIColor {
                    switch $0.userInterfaceStyle {
                    case .dark:
                        return darkColor
                    default:
                        return lightColor
                    }
                }
            }
            return lightColor
        }()
    }
}
