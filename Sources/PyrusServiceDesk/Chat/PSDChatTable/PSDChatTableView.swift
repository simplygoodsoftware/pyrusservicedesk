import UIKit
protocol PSDChatTableViewDelegate: NSObjectProtocol {
    func needShowRate(_ showRate: Bool)
    func restartTimer()
}

class PSDChatTableView: PSDDetailTableView{
    ///The id of chat that is shown in table view
    weak var chatDelegate: PSDChatTableViewDelegate?
    private let footerHeight : CGFloat = 10.0
    private let BOTTOM_INFELICITY : CGFloat = 10.0
    private var needShowRating : Bool = false
    private static let userCellId = "CellUser"
    private static let supportCellId = "CellSupport"
    private var tableMatrix : [[PSDRowMessage]] = [[PSDRowMessage]()]
    private var heightsMap : [IndexPath : CGFloat] = [IndexPath : CGFloat]()
    override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: .grouped)
        self.allowsMultipleSelection = false
        self.allowsSelection = false
    }
    private var lastMessageFromServer:PSDMessage?
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    private lazy var customRefresh: PSDRefreshControl = {
        let refreshControl = PSDRefreshControl.init(frame: self.bounds)
        refreshControl.position = .top
        refreshControl.tintColor = CustomizationHelper.textColorForTable
        refreshControl.addTarget(self, action: #selector(refreshChat), for: .valueChanged)
        return refreshControl
    }()
    private lazy var bottomRefresh : PSDRefreshControl = {
        let refreshControl = PSDRefreshControl.init(frame: self.bounds)
        refreshControl.position = .bottom
        refreshControl.tintColor = CustomizationHelper.textColorForTable
        refreshControl.addTarget(self, action: #selector(refreshChat), for: .valueChanged)
        return refreshControl
    }()
    @objc private func refreshChat(sender: PSDRefreshControl) {
        if !(self.superview?.subviews.contains(self.noConnectionView) ?? false) && !PSDGetChat.isActive() && sender.isRefreshing{
            updateChat(needProgress: true)
        }
        
    }
    deinit {
        self.removeRefreshControls()
    }
    func forceRefresh(showFakeMessage: Int?) {
        if !PSDGetChat.isActive(){
            if let showFakeMessage = showFakeMessage, showFakeMessage != 0 {
                let (addIndexPaths, addSections) =  tableMatrix.addFakeMessagge(messageId: showFakeMessage)
                if addIndexPaths.count>0 || addSections.count>0{
                    self.beginUpdates()
                    if(addIndexPaths.count>0){
                        self.insertRows(at: addIndexPaths, with: .none)
                    }
                    if(addSections.count>0){
                        self.insertSections(addSections, with: .none)
                    }
                    self.endUpdates()
                    self.scrollsToBottom(animated: false)
                }
            }
            updateChat(needProgress: true)
            
        }
    }
    override func recolor() {
        customRefresh.tintColor = CustomizationHelper.textColorForTable
        bottomRefresh.tintColor = CustomizationHelper.textColorForTable
        reloadData()
    }
    ///Setups needed properties to table view
    func setupTableView() {
        self.delegate=self
        self.dataSource=self
        self.backgroundColor = .clear
        self.register(PSDUserMessageCell.self, forCellReuseIdentifier: PSDChatTableView.userCellId)
        self.register(PSDSupportMessageCell.self, forCellReuseIdentifier: PSDChatTableView.supportCellId)
        self.keyboardDismissMode = .interactive
        self.separatorColor = .clear
    }
    /**
     Reloads ChatTableView. Creates the new tableMatrix.
     */
    func reloadChat()  {
        self.bottomPSDRefreshControl.insetHeight = 0.0
        self.removeRefreshControls()
        heightsMap = [IndexPath : CGFloat]()
        tableMatrix = [[PSDRowMessage]()] // clean old chat
        DispatchQueue.main.async {
            self.reloadData()
            self.isLoading = true
        }
        DispatchQueue.global().async {
            [weak self] in
            PSDGetChat.get(needShowError: true, delegate: self) {
                chat in
                DispatchQueue.main.async {
                    if chat != nil {
                        UnreadMessageManager.removeLastComment()
                    }
                    guard let self = self else {
                        return
                    }
                    self.needShowRating = chat?.showRating ?? false
                    self.showRateIfNeed()
                    
                    self.isLoading = false
                    if let chat = chat {
                        self.tableMatrix.create(from: chat)
                        
                        self.lastMessageFromServer = chat.messages.last
                        self.setLastActivityDate()
                        self.reloadData()
                        
                        self.removeNoConnectionView()
                        
                        UIView.setAnimationsEnabled(false)
                            self.scrollsToBottom(animated: false)
                            self.setNeedsLayout()
                            self.layoutIfNeeded()
                            self.scrollsToBottom(animated: false)
                            self.layoutIfNeeded()
                            self.scrollsToBottom(animated: false)
                        UIView.setAnimationsEnabled(true)
                    }
                    self.addRefreshControls()
                }
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
        if PyrusServiceDesk.setLastActivityDate(date){
            PyrusServiceDesk.restartTimer()
            self.chatDelegate?.restartTimer()
        }
    }
    private func showRateIfNeed() {
        let needShow = needShowRating && !PSDMessagesStorage.hasRatingInStorage()
        self.chatDelegate?.needShowRate(needShow)
    }
    private func addRefreshControls(){
        if !(self.superview?.subviews.contains(self.noConnectionView) ?? false){
            let deadlineTime = DispatchTime.now() + .seconds(1)
            DispatchQueue.main.asyncAfter(deadline: deadlineTime) {
                self.bottomPSDRefreshControl = self.bottomRefresh
                self.insertSubview(self.bottomRefresh, at: 0)
                self.insertSubview(self.customRefresh, at: 0)
            }
        }
        
    }
    private func removeRefreshControls(){
        removeTopRefreshControl()
        removeBottomRefreshControl()
    }
    private func removeBottomRefreshControl(){
        self.bottomRefresh.endRefreshing()
        self.bottomRefresh.removeFromSuperview()
    }
    private func removeTopRefreshControl(){
        self.customRefresh.endRefreshing()
        self.customRefresh.removeFromSuperview()
    }
    ///update taable matrix
    ///- parameter needProgress: Determines whether the view should respond to updating(need to show error) 
    func updateChat(needProgress:Bool) {
            PSDGetChat.get(needShowError: needProgress, delegate: nil) { [weak self]
                (chat : PSDChat?) in
                DispatchQueue.main.async  {
                    if let chat = chat{
                        UnreadMessageManager.removeLastComment()
                        self?.needShowRating = chat.showRating
                        self?.showRateIfNeed()
                        //compare number of messages it two last sections
                        guard let self = self else{
                            return
                        }
                        var hasChanges = false
                        let (removeIndexPaths, removeSections) = self.tableMatrix.removeFakeMessages()
                        if removeIndexPaths.count > 0
                            || removeSections.count > 0,
                           self.tableMatrix.count > 0
                        {
                            if !hasChanges {
                                hasChanges = true
                                PyrusLogger.shared.logEvent("Колличество ячеек до удаления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                                PyrusLogger.shared.logEvent("Колличество ячеек после удаления: \(self.tableMatrix[self.tableMatrix.count-1].count)")
                            }
                        }
                        self.tableMatrix.complete(from: chat, startMessage:self.lastMessageFromServer){
                            (indexPaths: [IndexPath], sections:IndexSet, _) in
                            DispatchQueue.main.async  {
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
                                let oldContentOffset = self.contentOffset
                                let oldContentSize = self.contentSize
                                self.removeNoConnectionView()
                                self.lastMessageFromServer = chat.messages.last
                                self.setLastActivityDate()
                                if indexPaths.count > 0
                                    || sections.count > 0
                                    || removeIndexPaths.count > 0
                                    || removeSections.count > 0
                                {
                                    let (newRemoveIndexPaths, addIndexPaths, reloadIndexPaths, newRemoveSections, addSections, reloadSections) = PSDTableView.compareAddAndRemoveRows(removeIndexPaths: removeIndexPaths, addIndexPaths: indexPaths, removeSections: removeSections, addSections: sections)
                                    PyrusLogger.shared.logEvent("Результат после сопоставления: удалять = \(newRemoveIndexPaths), \(newRemoveSections); \n добавлять = \(addIndexPaths), \(addSections); \n Обновлять = \(reloadIndexPaths), \(reloadSections)")
                                    self.beginUpdates()
                                    if newRemoveIndexPaths.count > 0 {
                                        self.deleteRows(at: newRemoveIndexPaths, with: .none)
                                    }
                                    if newRemoveSections.count > 0 {
                                        self.deleteSections(newRemoveSections, with: .none)
                                    }
                                    if addIndexPaths.count > 0{
                                        self.insertRows(at: addIndexPaths, with: .none)
                                    }
                                    if addSections.count > 0 {
                                        self.insertSections(addSections, with: .none)
                                    }
                                    if reloadIndexPaths.count > 0 {
                                        self.reloadRows(at: reloadIndexPaths, with: .none)
                                    }
                                    if reloadSections.count > 0 {
                                        self.reloadSections(reloadSections, with: .none)
                                    }
                                    self.endUpdates()
                                    self.scrollToBottomAfterRefresh(with: oldContentOffset, oldContentSize: oldContentSize)
                                }
                            }
                        }

                    }
                }
                DispatchQueue.main.async  {
                    self?.customRefresh.endRefreshing()
                    self?.bottomRefresh.endRefreshing()
                }
            }
    }
    ///Scrolls table to bottom after refresh, if table view was in bottom scroll position and new messages received
    private func scrollToBottomAfterRefresh(with oldOffset: CGPoint?, oldContentSize: CGSize?) {
        guard let oldOffset = oldOffset, let oldContentSize = oldContentSize else {
            return
        }
        self.setNeedsLayout()
        self.layoutIfNeeded()
        let expectedBottomOffset = oldContentSize.height - (self.frame.size.height - contentInset.top - contentInset.bottom)
        let hasChanges = oldContentSize != self.contentSize
        if expectedBottomOffset - BOTTOM_INFELICITY < oldOffset.y && hasChanges{
            self.scrollsToBottom(animated: true)
        }
    }
    lazy var noConnectionView : PSDNoConnectionView  = {
        let view = PSDNoConnectionView.init(frame: self.frame)
        view.delegate = self
        return view
    }()
    private func removeNoConnectionView(){
        if (self.superview?.subviews.contains(self.noConnectionView) ?? false){
            self.noConnectionView.removeFromSuperview()
            self.findViewController()?.becomeFirstResponder()
        }
    }
    
    ///the list off messages that wait to end scrollsToBottom animation
    private var waitingMessagesList : [PSDMessage: Bool] = [PSDMessage: Bool]()
    ///Boll to check if noew performing scrollsToBottom animation
    private var scrollAnimationPerform : Bool = false{
        didSet{
            if(!scrollAnimationPerform){
                self.performUpdatesFromMessagesList()
            }
        }
    }
    private func performUpdatesFromMessagesList(){
        for (message,changedToSent)  in waitingMessagesList{
            self.refresh(message: message, changedToSent: changedToSent)
        }
    }
    private static let delayBeforeUpdates : Int = 20//milliseconds
    ///Scroll tableview to its bottom position without animation
    private func scrollsToBottom(animated: Bool){
        self.layoutIfNeeded()

        let lastRow = self.lastIndexPath()
        if(lastRow.row>=0 || lastRow.section>=0){
            if !(lastRow.row == 0 && lastRow.section==0 ){
                self.scrollToRow(at: lastRow, at: .bottom, animated: animated)
            }
        }
    }
    ///Adds new row to table view to last index.
    ///- parameter message: PSDMessage object that need to be added.
    func addNewRow(message: PSDMessage)
    {
        if(self.numberOfRows(inSection: 0) == 0){
            self.addNewRow(){
                var lastSection = self.tableMatrix.count-1
                let lastRowDate = self.tableMatrix.date(of:lastSection)
                if(lastRowDate != nil && message.date.compareWithoutTime(with:lastRowDate!)  != .equal){//if massage has other date create new section
                    lastSection = lastSection + 1
                }
                for rowMessage in PSDObjectsCreator.parseMessageToRowMessage(message){
                    self.addRow(at: lastSection, dataForRow: rowMessage)
                }
            }
        }
        else{
            var lastSection = self.tableMatrix.count-1
            let lastRowDate = self.tableMatrix.date(of:lastSection)
            if(lastRowDate != nil && message.date.compareWithoutTime(with:lastRowDate!)  != .equal){//if massage has other date create new section
                lastSection = lastSection + 1
            }
            for rowMessage in PSDObjectsCreator.parseMessageToRowMessage(message){
                self.addRow(at: lastSection, dataForRow: rowMessage)
            }
        }
        
    }
    ///Returns last PSDChatMessageCell in tableView
    private func lastRow() -> PSDChatMessageCell?{
        return self.cellForRow(at: lastIndexPath()) as? PSDChatMessageCell
    }
    ///Add new row to tableMatrix and insert row to tableView, than scrolls it to bottom position
    ///- parameter index: section where row will be inserted and added new element to tableMatrix
    ///- parameter dataForRow:PSDMessage object for draw in cell.
    private func addRow(at index:Int, dataForRow: PSDRowMessage)
    {
        UIView.animate(withDuration: 0.0, delay: 0, usingSpringWithDamping: 0.0, initialSpringVelocity: 0.0, options: [], animations: {
            //add new section if need
            if self.tableMatrix.count-1 < index {
                self.tableMatrix.append([PSDRowMessage]())
                self.tableMatrix[index].append(dataForRow)
                self.insertSections([index], with: .none)
            }
            else{
                //add row to last section
                self.tableMatrix[index].append(dataForRow)
                self.beginUpdates()
                self.insertRows(at: [self.lastIndexPath()], with: .none)
                self.endUpdates()
                if(self.tableMatrix[index].count==1){
                    self.reloadSections([index], with: .none)//to change header( draw date)
                }
            }
         }, completion: { complete in
            if dataForRow.message.owner.personId == PyrusServiceDesk.userId {
                    self.scrollsToBottom(animated: true)
            }
        })
  
    }
    ///Returns last IndexPath in tableView
    private func lastIndexPath()->IndexPath
    {
        let row : Int = tableMatrix.last?.count ?? 1
        let section = tableMatrix.count>0 ? tableMatrix.count-1 : 0
        let index = IndexPath(row: row>0 ? row - 1 : 0, section: section)
        return index
    }
    private func getMessage(at indexPath:IndexPath)->PSDMessage?{
        if tableMatrix.count > indexPath.section && tableMatrix[indexPath.section].count>indexPath.row{
            let rowMessage = tableMatrix[indexPath.section][indexPath.row]
            return rowMessage.message
        }
        return nil
    }
    
    private func redrawCell(at indexPath: IndexPath,with message: PSDRowMessage) {
        
        //If cell is on the screen and this is attachments cell, we should not reload it because it will restart all animation (bad looking), so just pass progress
        let cellOnScrean = self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        var needReload = true
        if cellOnScrean && message.attachment != nil {
            let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView
            if attachmentView != nil && message.message.state == .sending {
                needReload = false
                self.redrawSendingAttachmentCell(at: indexPath, with: message)
            }
            
        }
        //Redraw cell with new Data
        if needReload{
            if(cellOnScrean){
                if let cell = self.cellForRow(at: indexPath) as? PSDChatMessageCell{
                    cell.draw(message:message)
                    PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate:self, animated: false)
                }
            }
        }
    }
    ///Redraw cell with sending attachment - pass progress and download state, hide stateView
    private func redrawSendingAttachmentCell(at indexPath: IndexPath,with message:PSDRowMessage){
        let cellOnScreen = self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        guard
            cellOnScreen,
            let attachment = message.attachment
        else {
            return
        }
        guard
            let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView,
            message.message.state == .sending
        else {
            return
        }
        attachmentView.progress = attachment.uploadingProgress
        attachmentView.downloadState = message.message.state
        guard message.message.state == .sending else {
            return
        }
        let stateView = (self.cellForRow(at: indexPath) as? PSDUserMessageCell)?.messageStateView
        stateView?._messageState = .sent //hide pass animation if uploading attach now
    }
}

extension PSDChatTableView : UITableViewDelegate,UITableViewDataSource{
    //MARK: table delegate and dataSourse
    func numberOfSections(in tableView: UITableView) -> Int {
        return tableMatrix.count
    }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableMatrix.count > section ? tableMatrix[section].count : 0
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let message : PSDRowMessage
        if (tableMatrix.count > indexPath.section && tableMatrix[indexPath.section].count > indexPath.row){
            message = tableMatrix[indexPath.section][indexPath.row]
        }
        else{
            message = PSDObjectsCreator.createWelcomeMessage()
        }
        let cell : PSDChatMessageCell
        if (message.rating ?? 0) != 0 || (message.message.owner.personId == PyrusServiceDesk.userId){
            cell = self.dequeueReusableCell(withIdentifier: PSDChatTableView.userCellId, for: indexPath) as! PSDUserMessageCell
            (cell as! PSDUserMessageCell).delegate = self
            
        }  else{
            cell = self.dequeueReusableCell(withIdentifier: PSDChatTableView.supportCellId, for: indexPath) as! PSDSupportMessageCell
            (cell as! PSDSupportMessageCell).needShowAvatar = self.tableMatrix.needShowAvatar(at: indexPath)
            if((cell as! PSDSupportMessageCell).needShowAvatar){
                (cell as! PSDSupportMessageCell).avatarView.owner = message.message.owner
                PSDSupportImageSetter.setImage(for:message.message.owner, in:(cell as! PSDSupportMessageCell).avatarView ,delagate: self)
            }
            
        }
        cell.needShowName = self.tableMatrix.needShowName(at: indexPath)
        cell.firstMessageInDate = indexPath.row == 0
        cell.draw(message:message)
        PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate: self, animated: false)
        self.redrawSendingAttachmentCell(at: indexPath, with: message)
        return cell
    }
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        heightsMap[indexPath] = cell.frame.size.height
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let view = UIView()
        view.frame = CGRect(x: 0, y: 0, width: self.frame.size.width, height: headerHeight)
        let dateLabel = UILabel()
        dateLabel.textColor = CustomizationHelper.textColorForTable
        dateLabel.font = .dateLabel
        var labelFrame = view.bounds
        labelFrame.size.width = labelFrame.size.width
        dateLabel.frame = labelFrame
        dateLabel.textAlignment = .center
        let sectionDate = tableMatrix.date(of: section)
        if sectionDate != nil {
            dateLabel.text = sectionDate!.asString()
        }
        view.addSubview(dateLabel)
        dateLabel.autoresizingMask = [.flexibleWidth,.flexibleHeight]
        return view
    }
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return headerHeight
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView.init(frame: CGRect(x: 0, y: 0, width: frame.size.width, height: footerHeight))
    }
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return footerHeight
    }
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return  UITableView.automaticDimension
    }
    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return heightsMap[indexPath] ?? UITableView.automaticDimension
    }
    
    func tableView(_ tableView: UITableView, shouldShowMenuForRowAt indexPath: IndexPath) -> Bool {
        if let cell = tableView.cellForRow(at: indexPath) as? PSDChatMessageCell{
            if (cell.cloudView.messageTextView.text.count>0){
                return true
            }
        }
        return false
    }
    
    func tableView(_ tableView: UITableView, canPerformAction action: Selector, forRowAt indexPath: IndexPath, withSender sender: Any?) -> Bool {
        if(action == #selector(UIResponderStandardEditActions.copy(_:))){
            return true
        }
        return false
    }
    
    func tableView(_ tableView: UITableView, performAction action: Selector, forRowAt indexPath: IndexPath, withSender sender: Any?) {
        if let cell = tableView.cellForRow(at: indexPath) as? PSDChatMessageCell{
            UIPasteboard.general.string = cell.cloudView.messageTextView.text
        }
    }
}

//MARK: PSDNoConnectionViewDelegate
extension PSDChatTableView : PSDNoConnectionViewDelegate{
    func retryPressed(){
        removeNoConnectionView()
        reloadChat()
    }
}
//MARK: PSDGetDelegate
extension PSDChatTableView : PSDGetDelegate{
    func showNoConnectionView(){
        DispatchQueue.main.async {
            EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "while show noConnectionView")
            if !(self.superview?.subviews.contains(self.noConnectionView) ?? false) && self.lastRow() == nil{
                self.superview?.addSubview(self.noConnectionView)
                (self.findViewController()?.inputAccessoryView as? PSDMessageInputView)?.inputTextView.resignFirstResponder()
                self.findViewController()?.resignFirstResponder()
            }
        }
    }
}
//MARK: PSDChatMessageCellDelegate
extension PSDChatTableView : PSDChatMessageCellDelegate{
    ///Send message one more time.
    func sendAgainMessage(from cell:PSDChatMessageCell){
        if let indexPath = self.indexPath(for: cell){
            if let cell = cell as? PSDUserMessageCell{
                cell.messageStateView._messageState = .sending
            }
            if let message = self.getMessage(at: indexPath){
                message.state = .sending
                PSDMessageSend.pass(message, delegate: self)
            }
        }
        else{
            // print("error sendAgainMessage, no cell")
        }
    }
    ///Delete message from self and from storage
    func deleteMessage(from cell:PSDChatMessageCell)
    {
        if let indexPath = self.indexPath(for: cell), let message = self.getMessage(at: indexPath){
            remove(message: message)
        }
    }
}
//MARK: PSDPreviewSetterDelegate
extension PSDChatTableView : PSDPreviewSetterDelegate{
    ///Change message local id - redraw it if it's visible. Didn't call reloadCell - so can't change cell height, or break animation.
    func reloadCells(with attachmentId: String)
    {
        for cell in self.visibleCells{
            if let cell = cell as? PSDChatMessageCell{
                if let indexPath = self.indexPath(for: cell){
                    if tableMatrix.has(indexPath:indexPath){
                         let message : PSDRowMessage = tableMatrix[indexPath.section][indexPath.row]
                        if(message.attachment?.localId == attachmentId){
                            PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate: nil, animated: true)
                        }
                    }
                }
                
            }
        }
    }
}
//MARK: PSDSupportImageSetterDelegate
extension PSDChatTableView : PSDSupportImageSetterDelegate{
    ///Change message's user's avatar - redraw it if it's visible. Didn't call reloadCell - so can't change cell height, or break animation.
    func reloadCells(with owner:PSDUser)
    {
        for cell in self.visibleCells{
            if let cell = cell as? PSDSupportMessageCell{
                if cell.avatarView.owner == owner && cell.avatarView.owner != nil{
                    PSDSupportImageSetter.setImage(for: cell.avatarView.owner!, in: cell.avatarView, delagate: nil)
                }
            }
        }
    }
}
//MARK: PSDMessageSendDelegate
extension PSDChatTableView : PSDMessageSendDelegate{
    func remove(message:PSDMessage){
        let indexPathsAndRows = self.tableMatrix.findIndexPath(ofMessage: message.clientId).keys.sorted(by:{$0 > $1})
        var indexPaths = [IndexPath]()
        for indexPath in indexPathsAndRows{
            if tableMatrix.has(indexPath: indexPath){
                tableMatrix[indexPath.section].remove(at: indexPath.row)
                indexPaths.append( indexPath)
            }
        }
        DispatchQueue.main.async {
            PSDMessagesStorage.removeFromStorage(messageId: message.clientId)
            DispatchQueue.main.async {
                 self.showRateIfNeed()
            }
        }
        if indexPaths.count > 0{
            let section = indexPaths[0].section
            self.deleteRows(at: indexPaths, with: .none)
            if tableView(self, numberOfRowsInSection: section) == 0{
                self.reloadSections([section], with: .none)
            }
        }
    }
    ///Change massage in tableMatrix and redraw Cell with new message data if it's visible.
    ///Didn't call reloadCell - so can't change cell height, or break animation.
    func refresh(message:PSDMessage, changedToSent: Bool){
        DispatchQueue.main.async{
            [weak self] in
            self?.chatDelegate?.restartTimer()
            if !(self?.scrollAnimationPerform ?? false){
                guard let self = self else{
                    return
                }
                let indexPathsAndMessages = self.tableMatrix.findIndexPath(ofMessage: message.clientId)
                guard  indexPathsAndMessages.count > 0 else{
                    EventsLogger.logEvent(.didNotFindMessageAfterUpdate)
                    return
                }
                var lastIndexPath: [IndexPath]? = nil
                
                if changedToSent && message.fromStrorage {
                    //is state was changed need to move sendded message up to sent block
                    lastIndexPath = self.tableMatrix.indexPathsAfterSent(for: message)
                    if let lastIndexPath = lastIndexPath, lastIndexPath.count > 0{
                        let newSection = lastIndexPath[0].section
                        if self.tableMatrix.count-1 < newSection {
                            self.tableMatrix.append([PSDRowMessage]())
                            if #available(iOS 11.0, *) {
                                self.performBatchUpdates({
                                    self.insertSections(IndexSet(arrayLiteral: newSection), with: .none)
                                }, completion: nil)
                            } else {
                                self.beginUpdates()
                                self.insertSections(IndexSet(arrayLiteral: newSection), with: .none)
                                self.endUpdates()
                            }
                        }
                    }
                }
                var oldSection = 0
                let indexPaths = indexPathsAndMessages.keys.sorted(by: {$0 < $1})
                var movedRows = 0
                for (i,indexPath) in indexPaths.enumerated(){
                    oldSection = indexPath.section
                    let movedIndexPath = IndexPath(row: indexPath.row - movedRows, section: indexPath.section)
                    guard self.tableMatrix.has(indexPath: movedIndexPath), let rowMessage = indexPathsAndMessages[indexPath] else{
                        continue
                    }
                    rowMessage.updateWith(message: message)
                    self.redrawCell(at:movedIndexPath,with: rowMessage)
                    if let lastIndexPath = lastIndexPath, lastIndexPath.count > i{
                        let newIndexPath = lastIndexPath[i]
                        if newIndexPath != indexPath{
                            movedRows = movedRows + 1
                            self.tableMatrix[movedIndexPath.section].remove(at: movedIndexPath.row)
                            self.tableMatrix[newIndexPath.section].insert(rowMessage, at:newIndexPath.row)
                            if #available(iOS 11.0, *) {
                                self.performBatchUpdates({
                                    self.moveRow(at: movedIndexPath, to: newIndexPath)
                                }, completion: nil)
                            } else {
                                self.beginUpdates()
                                self.moveRow(at: movedIndexPath, to: newIndexPath)
                                self.endUpdates()
                            }
                        }
                    }
                }
                if self.tableMatrix[oldSection].count == 0{
                    self.tableMatrix.remove(at: oldSection)
                    if #available(iOS 11.0, *) {
                        self.performBatchUpdates({
                            self.deleteSections(IndexSet(arrayLiteral: oldSection), with: .none)
                        }, completion: nil)
                    } else {
                        self.beginUpdates()
                        self.deleteSections(IndexSet(arrayLiteral: oldSection), with: .none)
                        self.endUpdates()
                    }
                }
                if let index = self.waitingMessagesList.index(forKey: message){
                    self.waitingMessagesList.remove(at: index )
                }
            }
            else{
                self?.waitingMessagesList[message] = changedToSent
            }
        }
    }
    
}
private extension UIFont {
    static let dateLabel = CustomizationHelper.systemFont(ofSize: 16.0)
}
