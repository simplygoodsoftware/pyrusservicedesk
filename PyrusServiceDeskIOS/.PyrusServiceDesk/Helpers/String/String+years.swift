

import Foundation
extension String {
    /**
     Returns year string according to its number: year/years, год/лет/года
     */
    static func yearString(_ years:Int)->String
    {
        var yearString :String = "Interval_Years".localizedPSD()
        if years == 1{
            yearString = "Interval_One_Year".localizedPSD()
        }
        else{
            let lastDigit = years % 10
            if years == 11 || years == 12 || years == 13 || years == 14{
                yearString = "Interval_Years".localizedPSD()
            }
            else if lastDigit == 1{
                yearString = "Interval_One_End_Year".localizedPSD()
            }
            else if [2,3,4].contains(lastDigit) {
                yearString = "Interval_Years_234".localizedPSD()
            }
        }
        
        return yearString.replacingOccurrences(of: REPLACE_STRING, with: "\(years)") 
    }
}
