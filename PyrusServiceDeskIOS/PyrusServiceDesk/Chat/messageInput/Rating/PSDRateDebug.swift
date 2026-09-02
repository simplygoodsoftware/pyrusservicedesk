import UIKit

///ВРЕМЕННАЯ диагностика задержки появления рейтинга.
///Удалить файл и все вызовы `PSDRateDebug.log` после расследования.
enum PSDRateDebug {
    static let isEnabled = true
    
    static func log(_ message: String) {
        guard isEnabled else { return }
        print(String(format: "[rate %.3f] ", CACurrentMediaTime()) + message)
    }
}
