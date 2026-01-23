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
            if PyrusServiceDesk.multichats {
                presenter.doWork(.updateActive(isActive: chat?.isActive ?? true))
            }
        }
    }
    private var messageId: String?
    private var timer: Timer?
    private var hasNoConnection: Bool = false
    private var gotData: Bool = false
    private var messageToSent: PSDMessage?
    private var messagesToPass = [MessageToPass]()
    private var firstLoad = true
    private var isRefreshing = false
    private var isOpen = false
    private var fromPush = false
    private var drawTable: Bool = false
    private var needUpdate: Bool = false
    private var chatForUpdate: PSDChat?
    private var isScrollButtonHiden = true
    private var isLoading: Bool = false
    private var hasOperatorTime: Bool = false
    private var newMessagesCount = 0 {
        didSet {
            if newMessagesCount > 0 {
                presenter.doWork(.updateBadge(messagesCount: newMessagesCount))
            }
        }
    }
    private let tableMatrixQueue = DispatchQueue(label: "com.psd.tableMatrix.queue")

    var isRefresh = false
    
    init(presenter: PSDChatPresenterProtocol, chat: PSDChat? = nil, fromPush: Bool = false, messageId: String? = nil) {
        self.presenter = presenter
        self.chat = chat
        self.fromPush = fromPush
        self.messageId = messageId
        
        if !PyrusServiceDesk.multichats {
            if let lastChat = PyrusServiceDesk.chats.first {
                let singleChat = PSDChat(
                    chatId: lastChat.chatId,
                    date: lastChat.date ?? Date(),
                    messages: PyrusServiceDesk.allMessages
                )
                singleChat.isActive = lastChat.isActive
                singleChat.showRating = lastChat.showRating
                singleChat.showRatingText = lastChat.showRatingText
                singleChat.userId = lastChat.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
                singleChat.lastReadedCommentId = lastChat.lastReadedCommentId
                singleChat.createdAt = lastChat.createdAt
                singleChat.lastMessageDate = lastChat.lastMessageDate
                singleChat.lastComment = PyrusServiceDesk.allMessages.last
                
                self.chat = singleChat
            } else if PyrusServiceDesk.clients.count > 0 {
                self.chat = PSDChat(chatId: 0, date: Date(), messages: [])
                self.chat?.isActive = false
            }
        }
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
            if !PyrusServiceDesk.multichats {
                if let lastChat = PyrusServiceDesk.chats.first {
                    let singleChat = PSDChat(
                        chatId: lastChat.chatId,
                        date: lastChat.date ?? Date(),
                        messages: PyrusServiceDesk.allMessages
                    )
                    singleChat.isActive = lastChat.isActive
                    singleChat.showRating = lastChat.showRating
                    singleChat.showRatingText = lastChat.showRatingText
                    singleChat.userId = lastChat.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
                    singleChat.lastReadedCommentId = lastChat.lastReadedCommentId
                    singleChat.createdAt = lastChat.createdAt
                    singleChat.lastMessageDate = lastChat.lastMessageDate
                    singleChat.lastComment = PyrusServiceDesk.allMessages.last
                    
                    self.chat = singleChat
                    if !(singleChat.lastComment?.isSupportMessage ?? false) ||
                        (singleChat.lastComment?.isSystemMessage ?? false) {
                        sendOperatorCalcCommand(needSync: false)
                        hasOperatorTime = true
                    }
                    readChat()
                } else if PyrusServiceDesk.clients.count > 0 {
                    self.chat = PSDChat(chatId: 0, date: Date(), messages: [])
                    self.chat?.isActive = false
                    PyrusServiceDesk.syncManager.syncGetTickets()
                } else {
                    PyrusServiceDesk.syncManager.syncGetTickets()
                }
            }
            isOpen = true
            presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
            if let chat, let chatId = chat.chatId {
                messagesToPass = PSDMessagesStorage.getSendingMessages(for: chatId)
            }
            
            addObservers()
            
            var needShowLoading = fromPush || PyrusServiceDesk.needShowLoading
            if !needShowLoading && !PyrusServiceDesk.multichats {
                needShowLoading = PyrusServiceDesk.clients.count == 0 ||
                    PyrusServiceDesk.clientId != PyrusServiceDesk.clients.first?.clientId
                if !needShowLoading && PyrusServiceDesk.customUserId != nil && PyrusServiceDesk.chats.count > 0 {
                    needShowLoading = !PyrusServiceDesk.chats.contains(where: {
                        $0.userId == PyrusServiceDesk.customUserId
                    })
                }
            }
            if needShowLoading {
                isLoading = true
                reloadChat()
            } else {
                beginTimer()
                updateChat(chat: chat)
                if let message = PyrusServiceDesk.messageToSend, message.count > 0 {
                    send(message, [], newTicket: true)
                    PyrusServiceDesk.messageToSend = nil
                }
                
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
                PyrusServiceDesk.restartTimer()
            }
//            OpusPlayer.shared.stopAllPlay()
        case .viewDidAppear:
            if PyrusServiceDesk.multichats && chat?.chatId ?? 0 == 0 {
                presenter.doWork(.showKeyBoard)
            }
        case .scrollButtonVisibleUpdated(isHidden: let isHidden):
            isScrollButtonHiden = isHidden
            if isHidden {
                newMessagesCount = 0
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
        case .viewWillAppear:
            startGettingInfo()
        case .sendRatingComment(comment: let comment, rating: let rating):
            sendRatingComment(ratingComment: comment, rating: rating)
        }
    }
}

private extension PSDChatInteractor {
    
    func sendOperatorCalcCommand(needSync: Bool = true) {
        guard let chat else { return }
        let params = TicketCommandParams(ticketId: chat.chatId, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId)
        let command = TicketCommand(commandId: UUID().uuidString, type: .calcOperatorTime, appId: PyrusServiceDesk.currentClientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, params: params)
        PyrusServiceDesk.repository.add(command: command, needSync: needSync)
    }

    func addObservers() {
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
        
        NotificationCenter.default.addObserver(forName: SyncManager.updateOperatorTimeNotification, object: nil, queue: .main) { [weak self] notification in
            DispatchQueue.main.async { [weak self] in
                if let userInfo = notification.userInfo,
                   let ticketId = userInfo["ticketId"] as? Int,
                   let message = userInfo["message"] as? String,
                   self?.chat?.chatId == ticketId {
                    self?.hasOperatorTime = true
                    self?.presenter.doWork(.updateOperatorTime(timeMessage: message))
                }
            }
        }
        NotificationCenter.default.addObserver(forName: SyncManager.removeOperatorTimeNotification, object: nil, queue: .main) { [weak self] notification in
            DispatchQueue.main.async { [weak self] in
                if let userInfo = notification.userInfo,
                   let ticketId = userInfo["ticketId"] as? Int,
                   self?.chat?.chatId == ticketId {
                    self?.hasOperatorTime = false
                    self?.presenter.doWork(.updateOperatorTime(timeMessage: nil))
                }
            }
        }
    }
    
    @objc func showConnectionError() {
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
        }
    }
    
    @objc func updateChats(fromSync: Bool = true) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            guard !drawTable else {
                needUpdate = true
                return
            }
            var chat: PSDChat? = nil
            if PyrusServiceDesk.multichats {
               chat = PyrusServiceDesk.chats.first(where: { $0.chatId == self.chat?.chatId })
            } else if let lastChat = PyrusServiceDesk.chats.first {
                let singleChat = PSDChat(
                    chatId: lastChat.chatId,
                    date: lastChat.date ?? Date(),
                    messages: PyrusServiceDesk.allMessages
                )
                singleChat.isActive = lastChat.isActive
                singleChat.showRating = lastChat.showRating
                singleChat.showRatingText = lastChat.showRatingText
                singleChat.userId = lastChat.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
                singleChat.lastReadedCommentId = lastChat.lastReadedCommentId
                singleChat.createdAt = lastChat.createdAt
                singleChat.lastMessageDate = lastChat.lastMessageDate
                singleChat.lastComment = PyrusServiceDesk.allMessages.last
                
                chat = singleChat
            } else if !isLoading || fromSync {
                chat = PSDChat(chatId: 0, date: Date(), messages: [])
                chat?.isActive = false
            }
            if let chat {
                if isOpen,
                   !PyrusServiceDesk.multichats,
                   chat.lastComment?.messageId != self.chat?.lastComment?.messageId,
                   chat.lastComment?.isSupportMessage ?? false {
                    readChat()
                }
                
                updateChatInfo()
                if chat.messages.count > self.chat?.messages.count ?? 0,
                   chat.messages.last?.owner?.authorId != PyrusServiceDesk.authorId,
                   isOpen {
                    self.chat = chat
                    readChat()
                    if !isScrollButtonHiden {
                        newMessagesCount += 1
                    }
                    if chat.messages.last?.isSupportMessage ?? false || chat.messages.last?.isSystemMessage ?? false {
                        presenter.doWork(.updateOperatorTime(timeMessage: nil))
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
                
                customization?.setChatTitileView(label)
                presenter.doWork(.reloadTitle)
                presenter.doWork(.updateTitle(connectionError: !PyrusServiceDesk.syncManager.networkAvailability))
            }
            
            if fromPush || !PyrusServiceDesk.multichats && isLoading {
                isRefresh = true
                
                self.updateChat(chat: chat)
                if let message = PyrusServiceDesk.messageToSend, message.count > 0 {
                    send(message, [], newTicket: true)
                    PyrusServiceDesk.messageToSend = nil
                }
                readChat()
                self.isRefresh = false
                fromPush = false
                firstLoad = false
                isLoading = false
                PyrusServiceDesk.needShowLoading = false
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
            let _ = SyncManager.setLastActivityDate()
            startGettingInfo()
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
        guard PyrusServiceDesk.multichats, let chat, let ticketId = chat.chatId, ticketId != 0 else { return }
        let userName: String
        if PyrusServiceDesk.customUserId == chat.userId {
            userName = PyrusServiceDesk.userName ?? ""
        } else {
            userName = PyrusServiceDesk.additionalUsers.first(where: { $0.userId == chat.userId })?.userName ?? ""
        }
        presenter.doWork(.updateInfo(ticketId: ticketId, userName: userName, createdAt: chat.messages.first?.date ?? chat.date ?? Date()))
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
//            presenter.doWork(.endLoading)
            updateButtons()
        }
        if messageId == nil {
            presenter.doWork(.scrollsToBottom(animated: true))
        }
    }
    
    private func drawTableWithData() {
        drawTable = true
        guard gotData else {
            drawTable = false
            return
        }
        gotData = false
        presenter.doWork(.dataIsShown)
        needShowRating = chat?.showRating ?? false
        showRateIfNeed()
        
        if let chat {
            tableMatrix.create(from: chat)
            lastMessageFromServer = chat.messages.last
        }
        setLastActivityDate()
        updateButtons()
        presenter.doWork(.drawTableWithData)
        drawTable = false
        if needUpdate {
            updateChats(fromSync: false)
        }
    }
    
    func readChat() {
        if !PyrusServiceDesk.multichats {
            let params = TicketCommandParams(ticketId: chat?.chatId ?? 0, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, messageId: nil)
            let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId:  PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, params: params)
            PyrusServiceDesk.repository.add(command: command)
            return
        }
        
        
        let ticketId = chat?.chatId ?? 0
        let lastReadedLocalId = max(chat?.lastReadedCommentId ?? 0, PyrusServiceDesk.repository.lastLocalReadCommentId(ticketId: chat?.chatId) ?? 0)

        if lastReadedLocalId < Int(chat?.lastComment?.messageId ?? "") ?? 0 {
            let messageId = Int(chat?.messages.last?.messageId ?? "")
            let params = TicketCommandParams(ticketId: ticketId, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId: PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, messageId: messageId)
            let command = TicketCommand(commandId: UUID().uuidString, type: .readTicket, appId: PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId, userId:  PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId, params: params)
            PyrusServiceDesk.repository.add(command: command)
        }
    }
    
    private func setLastActivityDate() {
        var lastDate: Date?
        if let lastMessage = self.lastMessageFromServer, lastMessage.owner?.personId == PyrusServiceDesk.userId {
            lastDate = lastMessage.date
        } else{
            lastDate = self.tableMatrix.lastUserMessageDate()
        }
        guard let date = lastDate else {
            return
        }
        if SyncManager.setLastActivityDate(date) {
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
        tableMatrixQueue.async { [weak self] in
            guard let self else { return }
            if let chat = chat {
                UnreadMessageManager.removeLastComment()
                needShowRating = chat.showRating
                showRateIfNeed()
                
                self.tableMatrix.complete(from: chat, startMessage: lastMessageFromServer) { [weak self] (hasChanges: Bool) in
                    DispatchQueue.main.async { [weak self] in
                        guard let self else { return }
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
        
    }
    
    func updateButtons() {
        presenter.doWork(.updateButtons(buttons: PSDChat.draftAnswers(tableMatrix)))
    }
    
    func removeLastMessage() {
        tableMatrix[tableMatrix.count - 1].removeLast()
        presenter.doWork(.reloadAll(animated: true))
    }
    
}

private extension PSDChatInteractor {
    func send(_ message: String, _ attachments: [PSDAttachment], newTicket: Bool = false) {
        let newMessage = PSDObjectsCreator.createMessage(message, attachments: attachments, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        if PyrusServiceDesk.multichats {
            if chat?.chatId ?? 0 == 0 {
                let nextId = PSDObjectsCreator.getNextLocalId()
                newMessage.requestNewTicket = true
                chat?.chatId = nextId
//                RateManager.incrementActionCount()
            }
            if let ticketId = chat?.chatId {
                newMessage.ticketId = ticketId
            }
        } else {
            let requestNewTicket = !(chat?.isActive ?? false) || newTicket
            newMessage.requestNewTicket = requestNewTicket
            if requestNewTicket {
                let nextId = PSDObjectsCreator.getNextLocalId()
                chat?.chatId = nextId
                chat?.isActive = true
            }
            
            if let ticketId = chat?.chatId {
                newMessage.ticketId = ticketId
            }
            
        }
        prepareMessageForDrawing(newMessage)
        messageToSent = newMessage
        presenter.doWork(.addNewRow)
        newMessage.commandId = UUID().uuidString
        newMessage.userId = chat?.userId ?? PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        newMessage.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        if !hasOperatorTime && (newMessage.requestNewTicket || SyncManager.getLastActivityDuration() > SyncManager.PSD_LAST_ACTIVITY_INTERVAL_HOUR) {
            hasOperatorTime = true
            sendOperatorCalcCommand(needSync: false)
        }
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    func sendRate(_ rateValue: Int) {
        presenter.doWork(.showRatingComment(ratingText: chat?.showRatingText, rating: rateValue))
        let newMessage = PSDObjectsCreator.createMessage(rating: rateValue, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        newMessage.commandId = UUID().uuidString
        newMessage.userId = PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        newMessage.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
        if CustomizationHelper.ratingSettings.type != RatingType.text.rawValue {
            prepareMessageForDrawing(newMessage)
            messageToSent = newMessage
            presenter.doWork(.addNewRow)
        } else {
            removeLastMessage()
        }
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    func sendRatingComment(ratingComment: String?, rating: Int) {
        let newMessage = PSDObjectsCreator.createMessage(ratingComment: ratingComment, rating: rating, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        newMessage.commandId = UUID().uuidString
        newMessage.userId = PyrusServiceDesk.currentUserId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        newMessage.appId = PyrusServiceDesk.currentClientId ?? PyrusServiceDesk.clientId
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
        let scrollToBottom = dataForRow.message.owner?.personId == PyrusServiceDesk.userId
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
        let reversedSection = tableMatrix.count - indexPath.section - 1
        let reversedIndex =
        IndexPath(
            row: tableMatrix[reversedSection].count - indexPath.row - 1,
            section: reversedSection
        )
        if let message = self.getMessage(at: reversedIndex) {
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
        let reversedSection = tableMatrix.count - indexPath.section - 1
        let reversedIndex =
        IndexPath(
            row: tableMatrix[reversedSection].count - indexPath.row - 1,
            section: reversedSection
        )
        if let message = self.getMessage(at: reversedIndex) {
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
    
    private func stopGettingInfo() {
        if timer != nil {
            timer?.invalidate()
            timer = nil
        }
    }
    
    func startGettingInfo() {
        stopGettingInfo()
        let timeInterval = SyncManager.getTimerInerval() ?? SyncManager.REFRESH_TIME_INTERVAL_3_MINUTES
        timer = Timer.scheduledTimer(timeInterval: timeInterval, target: self, selector: #selector(updateTable), userInfo: nil, repeats: false)
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
