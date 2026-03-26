import Foundation
import UIKit

struct Logs: Codable {
    let exceptions: [LogInfo]
    
    enum CodingKeys: String, CodingKey {
        case exceptions = "Exceptions"
    }
}

struct LogInfo: Codable {
    let message: String
    let stack: String
    var serial: Int = 1
    
    enum CodingKeys: String, CodingKey {
        case message = "Message",
             stack = "Stack",
             serial = "Serial"
    }
}

final class PSDLogService {

    enum RequestType {
        case crashLog
        case customLog
    }

    static private let session = URLSession.shared
    static private(set) var lastRequestType: RequestType?

    // MARK: - Limit

    static private let maxRequestsPerDay = 5
    static private let limitQueue = DispatchQueue(label: "com.pyrus.psdlogservice.limit")

    static private let requestsCountKey = "PSDLogService.requestsCountKey"
    static private let requestsDateKey = "PSDLogService.requestsDateKey"

    // MARK: - Public API

    static func sendLog<T: Codable>(
        _ model: T,
        toMethod urlString: String = "https://pyrus.com/services/mobileLog",
        requestType: RequestType = .customLog,
        needCheckLimit: Bool = true
    ) {
        if needCheckLimit,
           !reserveDailySendSlot() {
            return
        }
        
        let encoder = JSONEncoder()
        encoder.outputFormatting = []

        do {
            let data = try encoder.encode(model)
            sendRawRequest(data, toMethod: urlString, requestType: requestType)
        } catch { }
    }

    // MARK: - Request sending

    static private func sendRawRequest(
        _ body: Data,
        toMethod urlString: String,
        requestType: RequestType
    ) {
        guard let url = URL(string: urlString) else {
            return
        }

        lastRequestType = requestType

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        let task = session.dataTask(with: request)
        task.resume()
    }

    // MARK: - Daily limit

    @discardableResult
    static private func reserveDailySendSlot() -> Bool {
        limitQueue.sync {
            let defaults = UserDefaults.standard
            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())

            let savedDate = defaults.object(forKey: requestsDateKey) as? Date
            var requestsCount = defaults.integer(forKey: requestsCountKey)

            if let savedDate, calendar.isDate(savedDate, inSameDayAs: today) {
                // тот же день, используем текущий счётчик
            } else {
                // новый день — сбрасываем лимит
                requestsCount = 0
                defaults.set(today, forKey: requestsDateKey)
                defaults.set(0, forKey: requestsCountKey)
            }

            guard requestsCount < maxRequestsPerDay else {
                return false
            }

            defaults.set(requestsCount + 1, forKey: requestsCountKey)
            defaults.set(today, forKey: requestsDateKey)

            return true
        }
    }
}
