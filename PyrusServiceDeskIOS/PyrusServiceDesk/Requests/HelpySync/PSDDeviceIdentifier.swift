import Foundation
import Security

/// Идентификатор устройства для запроса HelpySync.
///
/// По спеке `device_id` не должен меняться при удалении и повторной
/// установке приложения (в отличие от `instance_id`), поэтому UserDefaults
/// не подходит — значение хранится в Keychain, который переживает
/// переустановку.
enum PSDDeviceIdentifier {

    private enum Constants {
        static let service = "com.pyrus.servicedesk.device"
        static let account = "device_id"
    }

    /// Кэш, чтобы не ходить в Keychain на каждый синк.
    /// Доступ ожидается с main (запрос собирается на главном потоке).
    private static var cachedDeviceId: String?

    /// Возвращает постоянный id устройства, при первом обращении создаёт его.
    static var deviceId: String {
        if let cachedDeviceId {
            return cachedDeviceId
        }
        let identifier = readFromKeychain() ?? createAndStore()
        cachedDeviceId = identifier
        return identifier
    }
}

private extension PSDDeviceIdentifier {

    static func createAndStore() -> String {
        let identifier = UUID().uuidString
        storeInKeychain(identifier)
        return identifier
    }

    static func readFromKeychain() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.service,
            kSecAttrAccount as String: Constants.account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        guard
            status == errSecSuccess,
            let data = item as? Data,
            let identifier = String(data: data, encoding: .utf8)
        else {
            return nil
        }
        return identifier
    }

    static func storeInKeychain(_ identifier: String) {
        guard let data = identifier.data(using: .utf8) else {
            return
        }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.service,
            kSecAttrAccount as String: Constants.account
        ]

        // Удаляем возможную старую запись, чтобы SecItemAdd не вернул duplicate.
        SecItemDelete(query as CFDictionary)

        var attributes = query
        attributes[kSecValueData as String] = data
        // AfterFirstUnlock — токен нужен и фоновым синкам,
        // ThisDeviceOnly — id устройства не должен мигрировать через iCloud/бэкап.
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        SecItemAdd(attributes as CFDictionary, nil)
    }
}
