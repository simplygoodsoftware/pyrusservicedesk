
import Foundation
extension String {
    /**
     Returns date (timeZone is UTC) from string with expected format.
     - parameter format: Is expected format of date.
     */
    func dateFromString(format: String) -> Date {
        if self.count == 0{
            return Date()
        }
        
        let dateFormatter = DateFormatter()
        dateFormatter.timeZone = TimeZone(abbreviation: "UTC")
        dateFormatter.dateFormat = format
        guard let date = dateFormatter.date(from: self) else {
            print("Pyrus Service Desk Error: Date conversion failed due to mismatched format.")
            return Date()
        }
        return date
    }
    
    @inline(__always)
    func fastParseISODate() -> Date {
        let chars = Array(self)
        func num(_ i: Int, _ j: Int) -> Int {
            var v = 0
            for k in i..<j {
                let c = chars[k]
                guard let d = c.wholeNumberValue else { return 0 }
                v = v * 10 + d
            }
            return v
        }
        
        // yyyy-MM-dd'T'HH:mm:ss
        let year   = num(0, 4)
        let month  = num(5, 7)
        let day    = num(8, 10)
        let hour   = num(11, 13)
        let minute = num(14, 16)
        let second = num(17, 19)
        
        // смещение часового пояса
        var tzOffset = 0  // в секундах
        if self.count > 19 {
            let tzSign = chars[19]
            if tzSign == "Z" {
                tzOffset = 0
            } else if tzSign == "+" || tzSign == "-" {
                let tzHours = num(20, 22)
                let tzMinutes = num(23, 25)
                tzOffset = (tzHours * 3600) + (tzMinutes * 60)
                if tzSign == "-" { tzOffset = -tzOffset }
            }
        }
        
        // Перевод в seconds since 1970 (UTC)
        var y = year
        var m = month
        if m <= 2 { y -= 1; m += 12 }
        
        // количество дней с Unix epoch (1970-01-01)
        let eraDays = 365 * y + y/4 - y/100 + y/400 + (153 * (m-3)+2)/5 + day - 719469
        let totalSeconds = eraDays * 86400 + hour * 3600 + minute * 60 + second + tzOffset
        
        return Date(timeIntervalSince1970: TimeInterval(totalSeconds))
    }
}
