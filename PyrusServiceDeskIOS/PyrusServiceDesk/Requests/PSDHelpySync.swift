import Foundation

/// Синхронизация через новый запрос HelpySync.
///
/// По спеке на HelpySync переводится только приложение Помощник
/// (режим мультичатов) — старый `PSDGetChats` (GetTickets) остаётся
/// без изменений для остальных потребителей библиотеки.
///
/// Блок `applications` в ответе не менялся, поэтому клиенты и объявления
/// продолжают разбираться существующей логикой `PSDGetChats`.
struct PSDHelpySync {

    private enum Constants {
        static let applicationsKey = "applications"
        static let tooManyRequestsCode = 429
        static let forbiddenCode = 403
        static let successCode = 200
        /// Максимальная длина фрагмента тела ответа в логах при ошибке парсинга.
        static let logSnippetLength = 30
        /// Сколько тикетов с дельтой максимум печатать в лог ответа.
        static let maxLoggedDeltaTickets = 30
        static let dumpLabel = "HelpySync"
    }

    private static var sessionTask: URLSessionDataTask?

    /// Выполняет синхронизацию HelpySync.
    /// Ожидает вызова с главного потока: снапшот кэша чатов и состояние
    /// пользователей читаются при сборке запроса.
    /// Completion всегда вызывается на главном потоке — там же выполняется
    /// разбор дельты и весь пост-процессинг, мутирующий общее состояние
    /// (кэш чатов и снапшот — reference-типы, живущие на main).
    /// Возвращает `GetTicketsResponse` (complete == false, если синк
    /// нужно повторить).
    static func get(
        commands: [TicketCommand],
        completion: @escaping (GetTicketsResponse) -> Void
    ) {
        remove()

        // Один снапшот кэша и для блоба tickets в запросе,
        // и для применения дельты в маппере.
        let cachedChats = PyrusServiceDesk.chats
        let requestBody = HelpySyncRequestBuilder.makeRequest(
            commands: commands,
            cachedChats: cachedChats
        )

        guard
            let request = URLRequest.createRequest(
                type: .helpySync,
                body: requestBody,
                encoder: HelpySyncWireFormat.makeEncoder()
            )
        else {
            completion(GetTicketsResponse(complete: true))
            return
        }

        PyrusLogger.shared.logEvent("HelpySync did begin, commands count: \(commands.count)")
        let requestStartTime = CFAbsoluteTimeGetCurrent()

        #if DEBUG
        let dumpToken = NetworkDumpWriter.saveRequest(
            label: Constants.dumpLabel,
            request: request,
            fallbackBody: try? HelpySyncWireFormat.makeEncoder().encode(requestBody)
        )
        #endif
        
        sessionTask = PyrusServiceDesk.mainSession.dataTask(with: request) { data, response, error in
            let elapsed = CFAbsoluteTimeGetCurrent() - requestStartTime
            
            #if DEBUG
            NetworkDumpWriter.saveResponse(
                for: dumpToken,
                body: data,
                statusCode: (response as? HTTPURLResponse)?.statusCode,
                error: error,
                duration: elapsed
            )
            #endif
            
            guard let data, error == nil else {
                PyrusLogger.shared.logEvent(
                    "HelpySync network error after \(String(format: "%.2f", elapsed))s:"
                    + " \(error?.localizedDescription ?? "no data")"
                )
                DispatchQueue.main.async {
                    completion(GetTicketsResponse(complete: false))
                }
                return
            }

            if let httpStatus = response as? HTTPURLResponse, httpStatus.statusCode != Constants.successCode {
                handleFailure(statusCode: httpStatus.statusCode, completion: completion)
                return
            }

            handleSuccess(data: data, cachedChats: cachedChats, elapsed: elapsed, completion: completion)
        }
        sessionTask?.resume()
    }

    /// Отменяет текущий запрос, если он есть.
    static func remove() {
        sessionTask?.cancel()
        sessionTask = nil
    }
}

private extension PSDHelpySync {

    static func handleFailure(
        statusCode: Int,
        completion: @escaping (GetTicketsResponse) -> Void
    ) {
        PyrusLogger.shared.logEvent("HelpySync failed with status code: \(statusCode)")

        switch statusCode {
        case Constants.tooManyRequestsCode:
            DispatchQueue.main.async {
                SyncManager.removeLastActivityDate()
                // Как и раньше при 429: не считаем синк проваленным,
                // чтобы не запускать ретраи поверх троттлинга сервера.
                completion(GetTicketsResponse(complete: true))
            }
        case Constants.forbiddenCode:
            DispatchQueue.main.async {
                if let onFailed = PyrusServiceDesk.onAuthorizationFailed {
                    onFailed()
                } else if !PyrusServiceDesk.multichats {
                    PyrusServiceDesk.mainController?.closeServiceDesk()
                }
                completion(GetTicketsResponse(complete: false))
            }
        default:
            DispatchQueue.main.async {
                completion(GetTicketsResponse(complete: false))
            }
        }
    }

    static func handleSuccess(
        data: Data,
        cachedChats: [PSDChat],
        elapsed: CFAbsoluteTime,
        completion: @escaping (GetTicketsResponse) -> Void
    ) {
        // Декодирование — чистая работа с Data, остаётся на фоне.
        let syncResponse: HelpySyncResponse
        let clientsArray: NSArray
        do {
            // Изменившаяся часть контракта — через Codable.
            let decoder = PSDJSONDecoderFactory.makeServerResponseDecoder()
            syncResponse = try decoder.decode(HelpySyncResponse.self, from: data)
            logResponseSummary(syncResponse, bodySize: data.count, elapsed: elapsed)

            // Блок applications не менялся — разбирается прежней логикой.
            let responseDictionary = try JSONSerialization.jsonObject(
                with: data,
                options: .allowFragments
            ) as? [String: Any] ?? [:]
            clientsArray = responseDictionary[Constants.applicationsKey] as? NSArray ?? NSArray()
        } catch {
            logParsingFailure(error, data: data)
            DispatchQueue.main.async {
                completion(GetTicketsResponse(complete: false))
            }
            return
        }

        // Маппинг и разбор applications мутируют общее состояние
        // (глобальные lastNoteId, список пользователей поддержки, clients)
        // и итерируют reference-типы снапшота, живущие на main.
        DispatchQueue.main.async {
            let clientsResult = PSDGetChats.generateClients(from: clientsArray)
            let announcementsResult = PSDGetChats.generateAnnouncements(
                from: clientsResult.serverAnnouncements
            )

            let chats = HelpySyncChatsMapper.makeChats(
                from: syncResponse.tickets ?? [],
                cachedChats: cachedChats
            )

            completion(
                GetTicketsResponse(
                    complete: true,
                    chats: chats,
                    clients: clientsResult.clients,
                    commandsResult: syncResponse.commandsResult,
                    authorAccessDenied: syncResponse.authorAccessDenied,
                    announcementsResult: announcementsResult,
                    hasMoreClosedTickets: syncResponse.hasMoreClosedTickets
                )
            )
        }
    }

    /// Сводка по ответу: сколько тикетов и новых комментариев пришло,
    /// и дельта по тикетам — вторая половина диагностики логики lastNoteId
    /// (первая — лог блоба в HelpySyncRequestBuilder).
    static func logResponseSummary(
        _ response: HelpySyncResponse,
        bodySize: Int,
        elapsed: CFAbsoluteTime
    ) {
        #if DEBUG
        let tickets = response.tickets ?? []
        let newCommentsCount = tickets.reduce(0) { $0 + ($1.comments?.count ?? 0) }
        print(
            "HelpySync response in \(String(format: "%.2f", elapsed))s,"
            + " body: \(bodySize) bytes,"
            + " tickets: \(tickets.count),"
            + " new comments: \(newCommentsCount),"
            + " hasMoreClosedTickets: \(response.hasMoreClosedTickets.map { String($0) } ?? "nil"),"
            + " commandsResult: \(response.commandsResult?.count ?? 0),"
            + " authorAccessDenied: \(response.authorAccessDenied?.count ?? 0)"
        )

        // Только тикеты с непустой дельтой: если после «тихого» синка здесь
        // вся история — сервер не сопоставил lastNoteId из блоба запроса.
        let ticketsWithDelta = tickets.filter { !($0.comments ?? []).isEmpty }
        guard !ticketsWithDelta.isEmpty else {
            return
        }
        let delta = ticketsWithDelta
            .prefix(Constants.maxLoggedDeltaTickets)
            .map { ticket -> String in
                let comments = ticket.comments ?? []
                let lastId = comments.map { $0.commentId }.max() ?? 0
                return "\(ticket.ticketId): +\(comments.count) (last \(lastId))"
            }
            .joined(separator: ", ")
        let suffix = ticketsWithDelta.count > Constants.maxLoggedDeltaTickets ? ", …" : ""
        print(
            "HelpySync delta by ticket (\(ticketsWithDelta.count)): [\(delta)\(suffix)]"
        )
        #endif
    }

    /// Пишет ошибку декодирования (с путём до поля) и фрагмент тела ответа.
    static func logParsingFailure(_ error: Error, data: Data) {
        let snippet = String(decoding: data.prefix(Constants.logSnippetLength), as: UTF8.self)
        PyrusLogger.shared.logEvent("HelpySync parsing error: \(error)")
        PyrusLogger.shared.logEvent("HelpySync response snippet: \(snippet)")
        #if DEBUG
        print("HelpySync parsing error: \(error)")
        if let decodingError = error as? DecodingError {
            print(decodingError)
        }
        print("HelpySync response snippet: \(snippet)")
        #endif
    }
}
