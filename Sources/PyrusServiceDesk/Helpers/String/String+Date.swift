
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
}
