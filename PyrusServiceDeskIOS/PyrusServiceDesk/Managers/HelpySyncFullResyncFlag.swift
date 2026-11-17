import Foundation

/// Персистентный флаг «нужен полный синк HelpySync».
///
/// Взводится при повышении версии кэша (`CacheVersionManager`): пока флаг
/// взведён, блоб `tickets` в запрос не отправляется, и сервер возвращает
/// полную историю — кэш при этом остаётся видимым в UI до ответа.
/// Снимается после успешного сохранения полученных чатов.
enum HelpySyncFullResyncFlag {

    private enum Constants {
        static let key = "PSDHelpySyncNeedFullResync"
    }

    static var isRaised: Bool {
        UserDefaults.standard.bool(forKey: Constants.key)
    }

    static func raise() {
        UserDefaults.standard.set(true, forKey: Constants.key)
    }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: Constants.key)
    }
}
