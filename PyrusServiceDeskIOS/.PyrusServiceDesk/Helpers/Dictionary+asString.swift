
import Foundation
extension Dictionary where Key ==  String , Value == Any{
    ///Return any object in key as String, if
    func stringOfKey(_ key:String)->String{
        let value = self[key]
        if value == nil{
            return ""
        }
        if let string = value as? String{
            return string
        }
        if let number = value as? NSNumber{
            return "\(number)"
        }
        if let int = value as? Int{
            return "\(int)"
        }
        return ""
    }
    ///Return any object in key as Int, if
    func intOfKey(_ key:String)->Int{
        let value = self[key]
        if value == nil{
            return 0
        }
        if let int = value as? Int{
            return int
        }
        if let number = value as? NSNumber{
            return number.intValue
        }
        return value as? Int ?? 0
    }
    ///Return any object in key as Int, if
    func colorOfKey(_ key:String)->UIColor{
        let value = self.stringOfKey(key)
        if value.count == 0{
            return .appColor
        }
        else {
            return hexStringToUIColor(value)
        }
    }
    
    private func hexStringToUIColor (_ hex:String) -> UIColor {
        var cString:String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        
        if (cString.hasPrefix("#")) {
            cString.remove(at: cString.startIndex)
        }
        
        if ((cString.count) != 6) {
            return .appColor
        }
        
        var rgbValue:UInt32 = 0
        Scanner(string: cString).scanHexInt32(&rgbValue)
        
        return UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
}
