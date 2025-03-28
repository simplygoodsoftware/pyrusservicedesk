import Foundation

struct IndexPaths {
    let newRemoveIndexPaths: [IndexPath]
    let addIndexPaths: [IndexPath]
    let reloadIndexPaths: [IndexPath]
    let newRemoveSections: IndexSet
    let addSections: IndexSet
    let reloadSections: IndexSet
}

struct MessageToPass {
    let message: PSDMessage
    let commandId: String
}

class PSDChatInteractor: NSObject {
    private let presenter: PSDChatPresenterProtocol
    
    private var tableMatrix: [[PSDRowMessage]] = [[PSDRowMessage]()] {
        didSet {
            self.presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
        }
    }
    private var lastMessageFromServer:PSDMessage?
    private var needShowRating : Bool = false
    private var loadingTimer: Timer?
    private var chat: PSDChat? {
        didSet {
            presenter.doWork(.updateActive(isActive: chat?.isActive ?? true))
        }
    }
    private var messageId: String?
    private let reloadInterval = 30.0 //seconds
    private var timer: Timer?
    private var hasNoConnection: Bool = false
    private var gotData: Bool = false
    private var messageToSent: PSDMessage?
    private var messagesToPass = [MessageToPass]()
    private var firstLoad = true
    private var isRefreshing = false
    private var isOpen = false
    private var fromPush = false
    private var isScrollButtonHiden = true
    private var newMessagesCount = 0 {
        didSet {
            if newMessagesCount > 0 {
                presenter.doWork(.updateBadge(messagesCount: newMessagesCount))
            }
        }
    }
    
    var isRefresh = false
    
    init(presenter: PSDChatPresenterProtocol, chat: PSDChat? = nil, fromPush: Bool = false, messageId: String? = nil) {
        self.presenter = presenter
        self.chat = chat
        self.fromPush = fromPush
        self.messageId = messageId
        super.init()
    }
    
    deinit {
        clearTimer()
    }
}

extension PSDChatInteractor: PSDChatInteractorProtocol {
    func doInteraction(_ action: PSDChatInteractorCommand) {
        switch action {
        case .viewDidload:
            isOpen = true
            presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
            if let chat, let chatId = chat.chatId {
                messagesToPass = PSDMessagesStorage.getSendingMessages(for: chatId)
            }
            NotificationCenter.default.addObserver(self, selector: #selector(updateChats), name: PyrusServiceDesk.chatsUpdateNotification, object: nil)
            NotificationCenter.default.addObserver(forName: SyncManager.commandsResultNotification, object: nil, queue: .main) { [weak self] notification in
                DispatchQueue.main.async { [weak self] in
                    if let data = notification.userInfo?["tickets"] as? Data {
                        do {
                            let decoder = JSONDecoder()
                            let ticketResults = try decoder.decode([TicketCommandResult].self, from: data)
                            self?.updateMessages(commandsResult: ticketResults)
                        } catch { }
                    }
                }
            }
            NotificationCenter.default.addObserver(self, selector: #selector(showConnectionError), name: SyncManager.connectionErrorNotification, object: nil)
            NotificationCenter.default.addObserver(forName: .refreshNotification, object: nil, queue: .main) { [weak self] notification in
                DispatchQueue.main.async { [weak self] in
                    if let userInfo = notification.userInfo,
                       let commandId = userInfo["commandId"] as? String,
                       let message = self?.messagesToPass.first(where: { $0.commandId.lowercased() == commandId.lowercased() })?.message {
                        self?.refresh(message: message, changedToSent: false)
                    }
                }
            }
            NotificationCenter.default.addObserver(forName: .removeMesssageNotification, object: nil, queue: .main) { [weak self] notification in
                DispatchQueue.main.async { [weak self] in
                    if let userInfo = notification.userInfo,
                       let commandId = userInfo["commandId"] as? String,
                       let message = self?.messagesToPass.first(where: { $0.commandId.lowercased() == commandId.lowercased() })?.message {
                        self?.messagesToPass.removeAll(where: { $0.commandId.lowercased() == commandId.lowercased() })
                        self?.remove(message: message)
                    }
                }
            }
            if !PyrusServiceDesk.multichats || fromPush {
                reloadChat()
            } else {
                beginTimer()
                updateChat(chat: chat)
                
                if let messageId,
                   let index = tableMatrix.findIndexPath(messageId: messageId).first {
                    let reversedIndex =
                    IndexPath(
                        row: tableMatrix[index.section].count - index.row - 1,
                        section: tableMatrix.count - index.section - 1
                    )
                    presenter.doWork(.scrollToRow(indexPath: reversedIndex))
                }
            }
            startGettingInfo()
        case .send(message: let message, attachments: let attachments):
            send(message, attachments)
        case .sendRate(rateValue: let rateValue):
            sendRate(rateValue)
        case .refresh:
            isRefreshing = true
            DispatchQueue.main.async {
                PyrusServiceDesk.syncManager.syncGetTickets()
            }
        case .addNewRow:
            if let messageToSent {
                addNewRow(message: messageToSent)
            }
        case .sendAgainMessage(indexPath: let indexPath):
            sendAgainMessage(indexPath: indexPath)
        case .deleteMessage(indexPath: let indexPath):
            deleteMessage(indexPath: indexPath)
        case .forceRefresh(showFakeMessage: let showFakeMessage):
            forceRefresh(showFakeMessage: showFakeMessage)
        case .reloadChat:
            reloadChat()
        case .updateNoConnectionVisible(visible: let visible):
            hasNoConnection = visible
        case .startGettingInfo:
            startGettingInfo()
        case .viewWillDisappear:
            isOpen = false
            if !PyrusServiceDesk.multichats {
                stopGettingInfo()
            }
        case .viewDidAppear:
            if PyrusServiceDesk.multichats && chat?.chatId ?? 0 == 0 {
                presenter.doWork(.showKeyBoard)
            }
        case .scrollButtonVisibleUpdated(isHidden: let isHidden):
            isScrollButtonHiden = isHidden
            if isHidden {
                newMessagesCount = 0
                readChat()
            }
        case .viewDidLayoutSubviews:
            if let messageId,
               let index = tableMatrix.findIndexPath(messageId: messageId).first {
                let reversedIndex =
                IndexPath(
                    row: tableMatrix[index.section].count - index.row - 1,
                    section: tableMatrix.count - index.section - 1
                )
                presenter.doWork(.scrollToRow(indexPath: reversedIndex))
            }
        }
    }
}

private extension PSDChatInteractor {
    
    @objc func showConnectionError() {
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
        }
    }
    
    @objc func updateChats() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            let chat = PyrusServiceDesk.chats.first(where: { $0.chatId == self.chat?.chatId })
            if let chat {
                updateChatInfo()
                if chat.messages.count > self.chat?.messages.count ?? 0,
                   chat.messages.last?.owner.authorId != PyrusServiceDesk.authorId,
                   isOpen {
                    self.chat = chat
                    readChat()
                    if !isScrollButtonHiden {
                        newMessagesCount += 1
                    }
                } else {
                    self.chat = chat
                }
            }
            
            if PyrusServiceDesk.multichats {
                let customization = PyrusServiceDesk.mainController?.customization
                let label = UILabel()
                label.isUserInteractionEnabled = true
                label.textAlignment = .center
                label.font = CustomizationHelper.systemBoldFont(ofSize: 17)
                label.text = chat?.subject?.count ?? 0 > 0 ? chat?.subject : "NewTicket".localizedPSD()
                label.translatesAutoresizingMaskIntoConstraints = false
                label.widthAnchor.constraint(equalToConstant: 200).isActive = true
                
                customization?.setChatTitileView(label)
                presenter.doWork(.reloadTitle)
                presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
            }
            
            if firstLoad && !PyrusServiceDesk.multichats || fromPush {
                isRefresh = true
                
                self.updateChat(chat: chat)
                readChat()
                self.isRefresh = false
                fromPush = false
                firstLoad = false
            } else {
                if messagesToPass.count == 0 && !isRefresh {
                    redrawChat(chat: chat)
                }
            }
        }
    }


    func updateMessages(commandsResult: [TicketCommandResult]) {
        for commandResult in commandsResult {
            if let messageToPass = messagesToPass.first(where: { $0.commandId.lowercased() == commandResult.commandId.lowercased() })?.message {
                isRefresh = true
                messageToPass.messageId = String(commandResult.commentId ?? 0)
                if let ticketId = commandResult.ticketId {
                    chat?.chatId = ticketId
                }
                showSendMessageResult(messageToPass: messageToPass, success: commandResult.error == nil)
                messagesToPass.removeAll(where: { $0.commandId.lowercased() == commandResult.commandId.lowercased() })
            }
        }
        presenter.doWork(.reloadAll(animated: false))
    }
    
    func showSendMessageResult(messageToPass: PSDMessage, success: Bool) {
        if success {
            let _ = PyrusServiceDesk.setLastActivityDate()
            PyrusServiceDesk.restartTimer()
        }
        
        let newState: messageState = success ? .sent : .cantSend
        let newProgress: CGFloat = success ? 1 : 0.0
        
        messageToPass.state = newState
        for attachment in (messageToPass.attachments ?? [PSDAttachment]()){
            attachment.uploadingProgress = newProgress
            attachment.size = attachment.data.count
            attachment.data = Data()//clear saved data
        }
        messageToPass.date = Date()//mesages from the storage can have old date - change it to the current, to avoid diffrent drawing after second enter into chat
        
        DispatchQueue.main.async { [weak self] in
            self?.refresh(message: messageToPass, changedToSent: success)
        }
    }
    
    func forceRefresh(showFakeMessage: Int?) {
        DispatchQueue.main.async {
            if !PSDGetChat.isActive() {
                PyrusServiceDesk.syncManager.syncGetTickets()
            }
        }
    }
    
    /**
     Reloads ChatTableView. Creates the new tableMatrix.
     */
    func reloadChat() {
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.reloadChat)
            PyrusServiceDesk.syncManager.syncGetTickets()
        }
        tableMatrix = [[PSDRowMessage]()] // clean old chat
        beginTimer()        
    }
    
    func updateChatInfo() {
        guard let chat, let ticketId = chat.chatId, ticketId != 0 else { return }
        let userName: String
        if PyrusServiceDesk.customUserId == chat.userId {
            userName = PyrusServiceDesk.userName ?? ""
        } else {
            userName = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == chat.userId })?.userName ?? ""
        }
        presenter.doWork(.updateInfo(ticketId: ticketId, userName: userName, createdAt: chat.messages.first?.date ?? Date()))
    }
    
    func updateChat(chat: PSDChat?) {
        if chat != nil {
            UnreadMessageManager.removeLastComment()
        }
        
        self.gotData = true
        if let chat = chat {
            self.chat = chat
            drawTableWithData()
            if loadingTimer?.isValid ?? false {
                drawTableWithData()
            }
            DispatchQueue.main.async { [weak self] in
                self?.updateChatInfo()
            }
        } else {
            presenter.doWork(.endLoading)
            updateButtons()
        }
        if messageId == nil {
            presenter.doWork(.scrollsToBottom(animated: true))
        }
    }
    
    private func drawTableWithData() {
        guard gotData else {
            return
        }
        gotData = false
        presenter.doWork(.dataIsShown)
        needShowRating = chat?.showRating ?? false
        showRateIfNeed()
        
        if let chat {
            tableMatrix.create(from: chat)
           // presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
            lastMessageFromServer = chat.messages.last
        }
        setLastActivityDate()
        updateButtons()
        presenter.doWork(.drawTableWithData)
    }
    
    func readChat() {
        let ticketId = chat?.chatId ?? 0
        let lastReadedLocalId = max(chat?.lastReadedCommentId ?? 0, PyrusServiceDesk.repository.lastLocalReadCommentId(ticketId: chat?.chatId) ?? 0)

        if lastReadedLocalId < Int(chat?.lastComment?.messageId ?? "") ?? 0 {
            let params = TicketCommandParams(ticketId: ticketId, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId, messageId: Int(chat?.lastComment?.messageId ?? ""))
            let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId:  PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId, params: params)
            PyrusServiceDesk.repository.add(command: command)
            DispatchQueue.main.async {
                PyrusServiceDesk.syncManager.syncGetTickets()
            }
        }
    }
    
    private func setLastActivityDate(){
        var lastDate: Date?
        if let lastMessage = self.lastMessageFromServer, lastMessage.owner.personId == PyrusServiceDesk.userId {
            lastDate = lastMessage.date
        } else{
            lastDate = self.tableMatrix.lastUserMessageDate()
        }
        guard let date = lastDate else {
            return
        }
        if PyrusServiceDesk.setLastActivityDate(date) {
            PyrusServiceDesk.restartTimer()
            startGettingInfo()
        }
    }
    
    private func showRateIfNeed() {
        let needShow = needShowRating && !PSDMessagesStorage.hasRatingInStorage()
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.needShowRate(showRate: needShow))
        }
    }
    
    func redrawChat(chat: PSDChat?) {
        if let chat = chat {
            UnreadMessageManager.removeLastComment()
            needShowRating = chat.showRating
            showRateIfNeed()
            
            self.tableMatrix.complete(from: chat, startMessage: lastMessageFromServer) { (hasChanges: Bool) in
                DispatchQueue.main.async {
                    self.presenter.doWork(.removeNoConnectionView)
                    self.lastMessageFromServer = chat.messages.last
                    self.setLastActivityDate()
                    
                    if hasChanges {
                        self.presenter.doWork(.updateRows)
                        self.updateButtons()
                    }
                }
            }
            
            DispatchQueue.main.async { [weak self] in
                self?.updateButtons()
                if self?.isRefreshing ?? false {
                    self?.presenter.doWork(.endRefreshing)
                    self?.isRefreshing = false
                }
            }
        }
    }
    
    func updateButtons() {
        presenter.doWork(.updateButtons(buttons: PSDChat.draftAnswers(tableMatrix)))
    }
    
}

private extension PSDChatInteractor {
    func send(_ message: String, _ attachments: [PSDAttachment]) {
        let newMessage = PSDObjectsCreator.createMessage(message, attachments: attachments, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        if PyrusServiceDesk.multichats {
            if chat?.chatId ?? 0 == 0 {
                let nextId = PSDObjectsCreator.getNextLocalId()
                newMessage.requestNewTicket = true
                chat?.chatId = nextId
            }
            if let ticketId = chat?.chatId {
                newMessage.ticketId = ticketId
            }
        }
        prepareMessageForDrawing(newMessage)
        messageToSent = newMessage
        presenter.doWork(.addNewRow)
        newMessage.commandId = UUID().uuidString
        newMessage.userId = PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        newMessage.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    func sendRate(_ rateValue: Int) {
        let newMessage = PSDObjectsCreator.createMessage(rating: rateValue, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        prepareMessageForDrawing(newMessage)
        messageToSent = newMessage
        newMessage.commandId = UUID().uuidString
        newMessage.userId = PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        newMessage.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        presenter.doWork(.addNewRow)
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    private func prepareMessageForDrawing(_ newMessage: PSDMessage) {
        newMessage.state = .sending
        newMessage.isOutgoing = true
        if let attachments = newMessage.attachments {
            for attachment in attachments{
                guard attachment.emptyId() else {
                    continue
                }
                attachment.uploadingProgress = 0
            }
        }
    }
    
    ///Adds new row to table view to last index.
    ///- parameter message: PSDMessage object that need to be added.
    func addNewRow(message: PSDMessage) {
        var lastSection = self.tableMatrix.count - 1
        let lastRowDate = self.tableMatrix.date(of: lastSection)
        if let lastRowDate,
           message.date.compareWithoutTime(with: lastRowDate) != .equal {
            // if massage has other date create new section
            lastSection = lastSection + 1
        }
        for rowMessage in PSDObjectsCreator.parseMessageToRowMessage(message){
            self.addRow(at: lastSection, dataForRow: rowMessage)
        }
        updateButtons()
    }
    
    ///Add new row to tableMatrix and insert row to tableView, than scrolls it to bottom position
    ///- parameter index: section where row will be inserted and added new element to tableMatrix
    ///- parameter dataForRow:PSDMessage object for draw in cell.
    private func addRow(at index: Int, dataForRow: PSDRowMessage) {
        if self.tableMatrix.count - 1 < index {
            self.tableMatrix.append([PSDRowMessage]())
            self.tableMatrix[index].append(dataForRow)
        } else {
            self.tableMatrix[index].append(dataForRow)
        }
        let scrollToBottom = dataForRow.message.owner.personId == PyrusServiceDesk.userId
        presenter.doWork(.addRow(scrollsToBottom: scrollToBottom))
    }
    
    private func getMessage(at indexPath: IndexPath) -> PSDMessage? {
        if tableMatrix.count > indexPath.section && tableMatrix[indexPath.section].count > indexPath.row {
            let rowMessage = tableMatrix[indexPath.section][indexPath.row]
            return rowMessage.message
        }
        return nil
    }
    
    func sendAgainMessage(indexPath: IndexPath) {
        if let message = self.getMessage(at: indexPath) {
            message.userId = PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
            message.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
            message.ticketId = chat?.chatId ?? 0
            message.state = .sending
            message.commandId = UUID().uuidString
            PSDMessageSend.pass(message, delegate: self)
        }
    }
    ///Delete message from self and from storage
    func deleteMessage(indexPath: IndexPath) {
        if let message = self.getMessage(at: indexPath) {
            remove(message: message)
        }
    }
}


extension PSDChatInteractor: PSDMessageSendDelegate {
    func addMessageToPass(message: PSDMessage, commandId: String) {
        messagesToPass.append(MessageToPass(message: message, commandId: commandId))
    }
    
    func refresh(message: PSDMessage, changedToSent: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            startGettingInfo()
            
            // Находим индексы сообщения
            let indexPathsAndMessages = tableMatrix.findIndexPath(ofMessage: message.clientId)
            guard !indexPathsAndMessages.isEmpty else {
                EventsLogger.logEvent(.didNotFindMessageAfterUpdate)
                return
            }
            
            // Определяем новые индексы, если сообщение изменило статус на "отправлено"
            var lastIndexPath: [IndexPath]?
            if changedToSent && (message.attachments?.isEmpty == false || message.fromStrorage) {
                lastIndexPath = message.fromStrorage
                    ? tableMatrix.indexPathsAfterSentStoreMessage(for: message)
                    : tableMatrix.indexPathsAfterSent(for: message)
                
                if let lastIndexPath, !lastIndexPath.isEmpty {
                    let newSection = lastIndexPath[0].section
                    if tableMatrix.count <= newSection {
                        tableMatrix.append([PSDRowMessage]())
                        presenter.doWork(.reloadAll(animated: true))
                    }
                }
            }
            
            // Обновляем сообщения и перемещаем их, если необходимо
            let indexPaths = indexPathsAndMessages.keys.sorted(by: { $0 < $1 })
            var movedRows = 0
            var oldSection = 0
            
            for (i, indexPath) in indexPaths.enumerated() {
                oldSection = indexPath.section
                let movedIndexPath = IndexPath(row: indexPath.row - movedRows, section: indexPath.section)
                
                guard self.tableMatrix.has(indexPath: movedIndexPath),
                      let rowMessage = indexPathsAndMessages[indexPath] else {
                    continue
                }
                
                // Обновляем сообщение
                rowMessage.updateWith(message: message)
                let reversedIndexPath = IndexPath(
                    row: tableMatrix[movedIndexPath.section].count - 1 - movedIndexPath.row,
                    section: tableMatrix.count - 1 - movedIndexPath.section
                )
                presenter.doWork(.redrawCell(indexPath: reversedIndexPath, message: rowMessage))
                
                // Перемещаем сообщение, если необходимо
                if let lastIndexPath, lastIndexPath.count > i {
                    let newIndexPath = lastIndexPath[i]
                    if newIndexPath != indexPath && newIndexPath.row >= movedIndexPath.row {
                        movedRows += 1
                        tableMatrix[movedIndexPath.section].remove(at: movedIndexPath.row)
                        tableMatrix[newIndexPath.section].insert(rowMessage, at: newIndexPath.row)
                        presenter.doWork(.reloadAll(animated: true))
                    }
                }
            }
            
            // Удаляем пустую секцию, если она есть
            if tableMatrix[oldSection].isEmpty {
                tableMatrix.remove(at: oldSection)
                presenter.doWork(.reloadAll(animated: true))
            }
            
            isRefresh = false
        }
    }
    
    func remove(message: PSDMessage) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            let indexPathsAndRows = tableMatrix.findIndexPath(ofMessage: message.clientId).keys.sorted(by:{$0 > $1})
            for indexPath in indexPathsAndRows {
                if tableMatrix.has(indexPath: indexPath) {
                    tableMatrix[indexPath.section].remove(at: indexPath.row)
                }
            }
            PSDMessagesStorage.remove(messageId: message.clientId)
            showRateIfNeed()
            presenter.doWork(.reloadAll(animated: true))
        }
    }
    
    func updateTicketId(_ ticketId: Int) {
        chat?.chatId = ticketId
        readChat()
    }
}

private extension PSDChatInteractor {
    private func clearTimer() {
        loadingTimer?.invalidate()
        loadingTimer = nil
    }
    
    private func beginTimer() {
        guard loadingTimer == nil else {
            return
        }
        loadingTimer = Timer.scheduledTimer(timeInterval: LOADING_INTERVAL, target: self, selector: #selector(stopLoading), userInfo: nil, repeats: false)
    }
    
    @objc func stopLoading(sender: Timer) {
        clearTimer()
        drawTableWithData()
    }
    var LOADING_INTERVAL: Double { 1 }
}

extension PSDChatInteractor {
    public static let PSD_LAST_ACTIVITY_INTEVAL_MINUTE = TimeInterval(90)
    public static let PSD_LAST_ACTIVITY_INTEVAL_5_MINUTES = TimeInterval(300)
    public static let PSD_LAST_ACTIVITY_INTEVAL_HOUR = TimeInterval(3600)
    public static let PSD_LAST_ACTIVITY_INTEVAL_3_DAYS = TimeInterval(3*24*60*60)
    public static let REFRESH_TIME_INTEVAL_5_SECONDS = TimeInterval(5)
    public static let REFRESH_TIME_INTEVAL_15_SECONDS = TimeInterval(15)
    public static let REFRESH_TIME_INTEVAL_1_MINUTE = TimeInterval(60)
    public static let REFRESH_TIME_INTEVAL_3_MINUTES = TimeInterval(180)
    private static let PSD_LAST_ACTIVITY_KEY = "PSDLastActivityDate"
    
    private func stopGettingInfo() {
        if timer != nil {
            timer?.invalidate()
            timer = nil
        }
    }
    
    static func userLastActivityKey() -> String{
        return PSD_LAST_ACTIVITY_KEY + "_" + PyrusServiceDesk.userId
    }
    
    private static func getTimerInerval() -> TimeInterval {
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(), let date = pyrusUserDefaults.object(forKey: PSDChatInteractor.userLastActivityKey()) as? Date{
            let difference = Date().timeIntervalSince(date)
            if difference <= PSD_LAST_ACTIVITY_INTEVAL_MINUTE {
                return REFRESH_TIME_INTEVAL_5_SECONDS
            } else if difference <= PSD_LAST_ACTIVITY_INTEVAL_5_MINUTES {
                return REFRESH_TIME_INTEVAL_15_SECONDS
            }
        }
        return REFRESH_TIME_INTEVAL_1_MINUTE
    }
    
    func startGettingInfo() {
        stopGettingInfo()
        timer = Timer.scheduledTimer(timeInterval: PSDChatInteractor.getTimerInerval(), target: self, selector: #selector(updateTable), userInfo:nil , repeats: false)
    }
    
    @objc private func updateTable() {
        startGettingInfo()
        if !hasNoConnection {
            DispatchQueue.main.async {
                PyrusServiceDesk.syncManager.syncGetTickets()
            }
        }
    }
}

extension PSDChatInteractor: PSDGetDelegate {
    func showNoConnectionView() {
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.showNoConnectionView)
        }
    }
}
