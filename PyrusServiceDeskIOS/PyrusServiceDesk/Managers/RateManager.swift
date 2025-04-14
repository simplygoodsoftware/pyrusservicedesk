import Foundation

@objc public class RateManager: NSObject {
    @objc public static let rateAppString = "itms-apps://itunes.apple.com/us/app/itunes-u/id385251753?action=write-review"
    private static let NEW_DATE_COMPONENT_MONTH = 1
    private static let INCREASE_DATE_COMPONENT_MONTH = 4
    private static let INCREASE_DATE_COMPONENT_DAY = 1
    
    private static let LAST_RATE_VERSION_KEY = "LAST_RATE_VERSION"
    private static let DATE_FOR_NEXT_RATE_KEY = "DATE_FOR_NEXT_RATE"
    private static let ACTION_COUNT_KEY = "ACTION_COUNT"
        
    @objc public static func updateLastRateVersion() {
        UserDefaults.standard.set(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String, forKey: LAST_RATE_VERSION_KEY)
    }
    
    private static func getLastRateVersion() -> String? {
        return UserDefaults.standard.string(forKey: LAST_RATE_VERSION_KEY)
    }
    
    private static func setDateForNextRate(_ date: Date?) {
        UserDefaults.standard.set(date, forKey: DATE_FOR_NEXT_RATE_KEY)
    }
    
    @objc public static func getDateForNextRate() -> Date? {
        return UserDefaults.standard.object(forKey: DATE_FOR_NEXT_RATE_KEY) as? Date
    }
        
    @objc public static func setIfNilDateForNextRate() {
        if getDateForNextRate() == nil {
            let calendar = Calendar.init(identifier: .gregorian)
            var components = DateComponents()
            components.month = NEW_DATE_COMPONENT_MONTH
            let newDate = calendar.date(byAdding: components, to: Date())
            setDateForNextRate(newDate)
        }
    }
    
    @objc public static func isNeedRateCurrentVersion() -> Bool {
        return getDateForNextRate() != nil &&
        (Date().compare(getDateForNextRate() ?? Date()) == .orderedDescending) &&
        !(getLastRateVersion() == Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String)
    }
    
    @objc public static func increaseDateForNextRate() {
        let calendar = Calendar.init(identifier: .gregorian)
        var components = DateComponents()
        components.month = INCREASE_DATE_COMPONENT_MONTH
        components.day = INCREASE_DATE_COMPONENT_DAY
        let newDate = calendar.date(byAdding: components, to: getDateForNextRate() ?? Date())
        setDateForNextRate(newDate)
        updateLastRateVersion()
    }
        
    @objc public static func incrementActionCount() {
        let key = ACTION_COUNT_KEY
        let currentCount = UserDefaults.standard.integer(forKey: key)
        UserDefaults.standard.set(currentCount + 1, forKey: key)
    }
    
    @objc public static func resetActionCount() {
        let key = ACTION_COUNT_KEY
        UserDefaults.standard.set(0, forKey: key)
    }
    
    /// Проверяет, было ли действие выполнено указанное количество раз
    @objc public static func isActionPerformed(times: Int) -> Bool {
        let key = ACTION_COUNT_KEY
        return UserDefaults.standard.integer(forKey: key) == times
    }
}
