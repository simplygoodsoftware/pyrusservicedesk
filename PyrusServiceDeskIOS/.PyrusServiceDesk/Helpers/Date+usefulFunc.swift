
let REPLACE_STRING = "%%"
import Foundation
extension Date {
    /**
     Returns string with time that has passed from "now"
     */
    func timeIntervalString()->String
    {
        let calendar = NSCalendar.autoupdatingCurrent
        let components = calendar.dateComponents([.minute,.hour,.day,.month,.year], from: self, to: Date())
        let minutes :Int = components.minute ?? 0
        let hours :Int = components.hour ?? 0
        let days :Int = components.day ?? 0
        let months :Int = components.month ?? 0
        let years :Int = components.year ?? 0
        
        if years > 0{
            return String.yearString(years)
        }
        if months > 0{
            return "Interval_Mounth".localizedPSD().replacingOccurrences(of: REPLACE_STRING, with:"\(months)")
        }
        if days>=7 {
            return "Interval_Week".localizedPSD().replacingOccurrences(of: REPLACE_STRING, with: "\(Int(round(Double(days/7))))")
        }
        if days > 0{
            return "Interval_Day".localizedPSD().replacingOccurrences(of: REPLACE_STRING, with:"\(days)")
        }
        if hours > 0{
            return  "Interval_Hour".localizedPSD().replacingOccurrences(of: REPLACE_STRING, with:"\(hours)")
        }
        if minutes >= 1{
            return "Interval_Minute".localizedPSD().replacingOccurrences(of: REPLACE_STRING, with:"\(minutes)")
        }
        return "Interval_Recent".localizedPSD()
    }
    /**
     Returns date as string. Where "Today", "Yesterday", and other date is in "Date_Format" if date has current year, or "Date_Format_Year" else.
     */
    func asString()->String
    {
        let calendar = NSCalendar.autoupdatingCurrent
        
        if calendar.isDateInToday(self){
            return "Today".localizedPSD()
        }
        if calendar.isDateInYesterday(self){
            return "Yesterday".localizedPSD()
        }
        
        let dateFormatter = DateFormatter()
        if self.year() == Date().year(){
            dateFormatter.dateFormat = "Date_Format".localizedPSD()
        }
        else{
            dateFormatter.dateFormat = "Date_Format_Year".localizedPSD()
        }
        return dateFormatter.string(from: self)
    }
    /**
     Returns date as string in format yyyyMMddHHmmss
     */
    func asNumbersString()->String{
        let formatter = DateFormatter()
        formatter.dateFormat = "Numbers_Date_Format".localizedPSD()
        return formatter.string(from: self)
    }
    /**
     Returns String with given format
     */
    func stringWithFormat(_ format: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = format
        return formatter.string(from: self)
    }
    /**
     Returns Date from string with given format
     */
    static func getDate(from string: String, with format: String) -> Date? {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = format
        return dateFormatter.date(from: string)
    }
    /**
      Returns date's time as string.
 */
    func timeAsString()->String
    {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "Time_Format".localizedPSD()
        return dateFormatter.string(from: self)
    }
    ///Return date year
    private func year()->Int{
        return Calendar.current.component(.year, from: self)
    }
    ///Return date month
    private func month()->Int{
        return Calendar.current.component(.month, from: self)
    }
    ///Return date day
    private func day()->Int{
        return Calendar.current.component(.day, from: self)
    }
    ///Return date hour
    private func hour()->Int{
        return Calendar.current.component(.hour, from: self)
    }
    ///Return date minute
    private func minute()->Int{
        return Calendar.current.component(.minute, from: self)
    }
    ///Return date second
    private func second()->Int{
        return Calendar.current.component(.second, from: self)
    }
    
   /* func compareWithoutTime(with date:Date)->Bool
    {
        if date.year() == self.year() &&  date.month() == self.month() && date.day() == self.day(){
             return true
        }
       
        return false
    }*/
    enum compareType {
        case equal
        case less
        case more
    }
    ///Compares to date without the time portion. Returns compareType
    func compareWithoutTime(with date:Date)->compareType
    {
        if date.year() == self.year() &&  date.month() == self.month() && date.day() == self.day(){
            return .equal
        }
        if date.year() < self.year(){
            return .less
        }
        else if date.year() == self.year(){
            if date.month() < self.month(){
                return .less
            }else if date.month() == self.month(){
                if date.day() < self.day(){
                    return .less
                }
            }
        }
        return .more
    }
    ///Compares to date without the date portion - only its times. Returns compareType
    func compareTime(with date:Date)->compareType
    {
        if date.hour() == self.hour() &&  date.minute() == self.minute() && date.second() == self.second(){
            return .equal
        }
        if date.hour() < self.hour(){
            return .less
        }
        else if date.hour() == self.hour(){
            if date.minute() < self.minute(){
                return .less
            }else if date.minute() == self.minute(){
                if date.second() < self.second(){
                    return .less
                }
            }
        }
        return .more
    }
}
