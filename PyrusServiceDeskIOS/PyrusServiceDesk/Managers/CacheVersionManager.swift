import Foundation

final class CacheVersionManager {
    
    static let shared = CacheVersionManager()

    /// Текущая версия кэша
    private let currentVersion = 2
    
    private let userDefaults = UserDefaults.standard
    private let cacheVersionKey = "cacheVersionKey"

    private init() {}

    /// Проверяет, обновилась ли версия кэша. Сохраняет новую, если она выше предыдущей.
    /// - Returns: `true`, если версия кэша изменилась (выросла)
    func checkAndUpdateIfNeeded() -> Bool {
        let savedVersion = userDefaults.integer(forKey: cacheVersionKey)
        
        guard currentVersion > savedVersion else {
            return false
        }

        userDefaults.set(currentVersion, forKey: cacheVersionKey)
        return true
    }

    /// Возвращает текущую версию из кода
    func getCurrentVersion() -> Int {
        return currentVersion
    }

    /// Возвращает сохранённую версию (из UserDefaults)
    func getSavedVersion() -> Int {
        return userDefaults.integer(forKey: cacheVersionKey)
    }

    /// Сбросить сохранённую версию
    func resetSavedVersion() {
        userDefaults.removeObject(forKey: cacheVersionKey)
    }
}
