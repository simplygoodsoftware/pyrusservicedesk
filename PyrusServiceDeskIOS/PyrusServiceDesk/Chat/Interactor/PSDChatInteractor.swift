import Foundation

struct IndexPaths {
    let newRemoveIndexPaths: [IndexPath]
    let addIndexPaths: [IndexPath]
    let reloadIndexPaths: [IndexPath]
    let newRemoveSections: IndexSet
    let addSections: IndexSet
    let reloadSections: IndexSet
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
    private var chat: PSDChat?
    private let reloadInterval = 30.0 //seconds
    private var timer: Timer?
    private var hasNoConnection: Bool = false
    private var gotData: Bool = false
    private var messageToSent: PSDMessage?
    
    init(presenter: PSDChatPresenterProtocol, chat: PSDChat? = nil) {
        self.presenter = presenter
        self.chat = chat
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
                reloadChat()
            } else {
                beginTimer()
                updateChat(chat: chat)
                readChat()
            }
            startGettingInfo()
        case .send(message: let message, attachments: let attachments):
            send(message, attachments)
        case .sendRate(rateValue: let rateValue):
            sendRate(rateValue)
        case .refresh:
            if !PSDGetChat.isActive() {
                updateChat(needProgress: true)
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
            if !PyrusServiceDesk.multichats {
                stopGettingInfo()
            }
        case .viewDidAppear:
            if PyrusServiceDesk.multichats && chat?.chatId ?? 0 == 0 {
                presenter.doWork(.showKeyBoard)
            }
        }
    }
}

private extension PSDChatInteractor {
    func forceRefresh(showFakeMessage: Int?) {
        if !PSDGetChat.isActive() {
            if let showFakeMessage, showFakeMessage != 0 {
                presenter.doWork(.addFakeMessage(messageId: showFakeMessage))
            }
            updateChat(needProgress: true)
        }
    }
    
    func reload() {
        tableMatrix = [[PSDRowMessage]()]
        presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
        let ticketId = chat?.chatId ?? 0
        DispatchQueue.global().async { [weak self] in
            PSDGetChat.get(needShowError: true, delegate: self, ticketId: ticketId) {
                chat in
                PSDGetChats.get() { _ in }
                DispatchQueue.main.async { [weak self] in
                    self?.updateChat(chat: chat)
                }
            }
        }
    }
    
    /**
     Reloads ChatTableView. Creates the new tableMatrix.
     */
    func reloadChat() {
        DispatchQueue.main.async { [weak self] in
            self?.presenter.doWork(.reloadChat)
        }
        tableMatrix = [[PSDRowMessage]()] // clean old chat
        beginTimer()
        let ticketId = chat?.chatId ?? 0
        DispatchQueue.global().async { [weak self] in
            PSDGetChat.get(needShowError: true, delegate: self, ticketId: ticketId) {
                chat in
                DispatchQueue.main.async {
                    self?.updateChat(chat: chat)
                }
            }
        }
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
        } else {
            presenter.doWork(.endLoading)
            updateButtons()
        }
        presenter.doWork(.scrollsToBottom(animated: true))
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
            presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
            lastMessageFromServer = chat.messages.last
        }
        setLastActivityDate()
        updateButtons()
        presenter.doWork(.drawTableWithData)
    }
    
    func readChat() {
        let ticketId = chat?.chatId ?? 0
        let userId = chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId
        DispatchQueue.global().async { [weak self] in
            PSDGetChat.get(needShowError: true, delegate: self, ticketId: ticketId, userId: userId) { _ in
                PSDGetChats.get() { _ in }
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
    
    func updateChat(needProgress:Bool) {
        PSDGetChat.get(needShowError: needProgress, delegate: nil, ticketId: chat?.chatId ?? 0) {
            [weak self] (chat : PSDChat?) in
            if let chat = chat {
                UnreadMessageManager.removeLastComment()
                self?.needShowRating = chat.showRating
                self?.showRateIfNeed()
                //compare number of messages it two last sections
                guard let self = self else{
                    return
                }
                var hasChanges = false
                let (removeIndexPaths, removeSections) = self.tableMatrix.removeFakeMessages()
                if removeIndexPaths.count > 0 || removeSections.count > 0,
                   self.tableMatrix.count > 0 {
                    if !hasChanges {
                        hasChanges = true
                        PyrusLogger.shared.logEvent("Колличество ячеек до удаления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                        PyrusLogger.shared.logEvent("Колличество ячеек после удаления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                    }
                }
                
                self.tableMatrix.complete(from: chat, startMessage:self.lastMessageFromServer) { (indexPaths: [IndexPath], sections: IndexSet, _) in
                    DispatchQueue.main.async {
                        if indexPaths.count > 0 ||
                            sections.count > 0,
                           self.tableMatrix.count > 0
                        {
                            if !hasChanges {
                                hasChanges = true
                                PyrusLogger.shared.logEvent("Колличество ячеек до удаления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                            }
                            PyrusLogger.shared.logEvent("Колличество ячеек после добавления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                            PyrusLogger.shared.logEvent("При удалении фейка: ячейки = \(removeIndexPaths), секции \(removeSections)")
                            PyrusLogger.shared.logEvent("При добавленни нового сообщения: ячейки = \(indexPaths), секции \(sections)")
                        }
                                        
                        self.presenter.doWork(.removeNoConnectionView)
                        self.lastMessageFromServer = chat.messages.last
                        self.setLastActivityDate()
                        
                        if indexPaths.count > 0 || sections.count > 0
                            || removeIndexPaths.count > 0 || removeSections.count > 0 {
                            let indexPaths = PSDTableView.compareAddAndRemoveRows(removeIndexPaths: removeIndexPaths, addIndexPaths: indexPaths, removeSections: removeSections, addSections: sections)
                            PyrusLogger.shared.logEvent("Результат после сопоставления: удалять = \(indexPaths.newRemoveIndexPaths), \(indexPaths.newRemoveSections); \n добавлять = \(indexPaths.addIndexPaths), \(indexPaths.addSections); \n Обновлять = \(indexPaths.reloadIndexPaths), \(indexPaths.reloadSections)")
                            
                            self.presenter.doWork(.updateRows(indexPaths: indexPaths))
                            self.updateButtons()
                        }
                    }
                }
            }
            
            DispatchQueue.main.async { [weak self] in
                self?.updateButtons()
                self?.presenter.doWork(.endRefreshing)
            }
        }
    }
    
    func updateButtons() {
        presenter.doWork(.updateButtons(buttons: PSDChat.draftAnswers(tableMatrix)))
    }
    
}

private extension PSDChatInteractor {
    func send(_ message:String, _ attachments: [PSDAttachment]) {
        let newMessage = PSDObjectsCreator.createMessage(message, attachments: attachments, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        prepareMessageForDrawing(newMessage)
        messageToSent = newMessage
        presenter.doWork(.addNewRow)
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    func sendRate(_ rateValue: Int) {
        let newMessage = PSDObjectsCreator.createMessage(rating: rateValue, ticketId: chat?.chatId ?? 0, userId: chat?.userId ?? PyrusServiceDesk.customUserId ?? PyrusServiceDesk.userId)
        prepareMessageForDrawing(newMessage)
        messageToSent = newMessage
        presenter.doWork(.addNewRow)
        PSDMessageSend.pass(newMessage, delegate: self)
    }
    
    private func prepareMessageForDrawing(_ newMessage: PSDMessage) {
        newMessage.state = .sending
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
    private func addRow(at index:Int, dataForRow: PSDRowMessage) {
        var insertSections = false
        if self.tableMatrix.count-1 < index {
            self.tableMatrix.append([PSDRowMessage]())
            self.tableMatrix[index].append(dataForRow)
            insertSections = true
        } else {
            self.tableMatrix[index].append(dataForRow)
        }
        let scrollToBottom = dataForRow.message.owner.personId == PyrusServiceDesk.userId
        
        presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
        presenter.doWork(.addRow(index: index, lastIndexPath: lastIndexPath(), insertSections: insertSections, scrollsToBottom: scrollToBottom))
    }
    
    private func lastIndexPath() -> IndexPath {
        let row: Int = tableMatrix.last?.count ?? 1
        let section = tableMatrix.count > 0 ? tableMatrix.count - 1 : 0
        let index = IndexPath(row: row > 0 ? row - 1 : 0, section: section)
        return index
    }
    
    
    
    private func getMessage(at indexPath:IndexPath) -> PSDMessage? {
        if tableMatrix.count > indexPath.section && tableMatrix[indexPath.section].count>indexPath.row {
            let rowMessage = tableMatrix[indexPath.section][indexPath.row]
            return rowMessage.message
        }
        return nil
    }
    
    func sendAgainMessage(indexPath: IndexPath) {
        if let message = self.getMessage(at: indexPath) {
            message.ticketId = chat?.chatId ?? 0
            message.state = .sending
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
    func refresh(message: PSDMessage, changedToSent: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.startGettingInfo()
            guard let self = self else {
                return
            }
            let indexPathsAndMessages = self.tableMatrix.findIndexPath(ofMessage: message.clientId)
            guard  indexPathsAndMessages.count > 0 else {
                EventsLogger.logEvent(.didNotFindMessageAfterUpdate)
                return
            }
            var lastIndexPath: [IndexPath]? = nil
            
            if changedToSent && message.fromStrorage {
                //is state was changed need to move sendded message up to sent block
                lastIndexPath = self.tableMatrix.indexPathsAfterSent(for: message)
                if let lastIndexPath = lastIndexPath, lastIndexPath.count > 0 {
                    let newSection = lastIndexPath[0].section
                    if self.tableMatrix.count - 1 < newSection {
                        self.tableMatrix.append([PSDRowMessage]())
                        presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
                        presenter.doWork(.insertSections(sections: IndexSet(arrayLiteral: newSection)))
                    }
                }
            }
            var oldSection = 0
            let indexPaths = indexPathsAndMessages.keys.sorted(by: {$0 < $1})
            var movedRows = 0
            for (i,indexPath) in indexPaths.enumerated() {
                oldSection = indexPath.section
                let movedIndexPath = IndexPath(row: indexPath.row - movedRows, section: indexPath.section)
                guard self.tableMatrix.has(indexPath: movedIndexPath), let rowMessage = indexPathsAndMessages[indexPath] else {
                    continue
                }
                rowMessage.updateWith(message: message)
                presenter.doWork(.redrawCell(indexPath: movedIndexPath, message: rowMessage))
                if let lastIndexPath = lastIndexPath, lastIndexPath.count > i {
                    let newIndexPath = lastIndexPath[i]
                    if newIndexPath != indexPath {
                        movedRows = movedRows + 1
                        self.tableMatrix[movedIndexPath.section].remove(at: movedIndexPath.row)
                        self.tableMatrix[newIndexPath.section].insert(rowMessage, at:newIndexPath.row)
                        presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
                        presenter.doWork(.moveRow(movedIndexPath: movedIndexPath, newIndexPath: newIndexPath))
                    }
                }
            }
            if self.tableMatrix[oldSection].count == 0 {
                self.tableMatrix.remove(at: oldSection)
                presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
                presenter.doWork(.deleteSections(sections: IndexSet(arrayLiteral: oldSection)))
            }
        }
    }
    
    func remove(message: PSDMessage) {
        let indexPathsAndRows = self.tableMatrix.findIndexPath(ofMessage: message.clientId).keys.sorted(by:{$0 > $1})
        var indexPaths = [IndexPath]()
        for indexPath in indexPathsAndRows {
            if tableMatrix.has(indexPath: indexPath){
                tableMatrix[indexPath.section].remove(at: indexPath.row)
                presenter.doWork(.updateTableMatrix(matrix: tableMatrix))
                indexPaths.append( indexPath)
            }
        }
        
        DispatchQueue.main.async { [weak self] in
            PSDMessagesStorage.removeFromStorage(messageId: message.clientId)
            self?.showRateIfNeed()
            
            if indexPaths.count > 0 {
                let section = indexPaths[0].section
                self?.presenter.doWork(.deleteRows(indexPaths: indexPaths, section: section))
            }
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
        if !hasNoConnection && !PSDGetChat.isActive() {
           updateChat(needProgress: false)
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
