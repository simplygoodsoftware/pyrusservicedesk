import UIKit
protocol PSDChatTableViewDelegate: NSObjectProtocol {
    func needShowRate(_ showRate: Bool)
}
class PSDChatTableView: PSDDetailTableView{
    ///The id of chat that is shown in table view
    var chatId :String = ""
    weak var chatDelegate: PSDChatTableViewDelegate?
    private let footerHeight : CGFloat = 10.0
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
        refreshControl.addTarget(self, action: #selector(refreshChat), for: .valueChanged)
        return refreshControl
    }()
    private lazy var bottomRefresh : PSDRefreshControl = {
        let refreshControl = PSDRefreshControl.init(frame: self.bounds)
        refreshControl.position = .bottom
        refreshControl.addTarget(self, action: #selector(refreshChat), for: .valueChanged)
        return refreshControl
    }()
    @objc private func refreshChat() {
        if !(self.superview?.subviews.contains(self.noConnectionView) ?? false) && !PSDGetChat.isActive(){
            updateChat(needProgress: true)
        }
        
    }
    deinit {
        self.removeRefreshControls()
    }
    func forceBottomRefresh() {
        if contentSize.height > frame.size.height{
            scrollsToBottom(animated: true)
            bottomRefresh.forceRefresh()
        }else{
            customRefresh.forceRefresh()
        }
        updateChat(needProgress: true)
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
     If(!PSDChatTableView.isNewChat):
     Reloads ChatTableView according to its chatId. If chatId == "" - this is a new chat, and sending a message will generate .createNew RequestType, and auto reloading not working. If chatId not empty String, it is considered that there is a chat with messages, auto reloading is on, "pullToRefresh" is on, and RequestType is .update.
 */
    func reloadChat()  {
        self.bottomPSDRefreshControl.insetHeight = 0.0
        self.removeRefreshControls()
        heightsMap = [IndexPath : CGFloat]()
        if(!PSDChatTableView.isNewChat(chatId)){
            tableMatrix = [[PSDRowMessage]()] // clean old chat
            DispatchQueue.main.async {
                self.reloadData()
                self.isLoading = true
            }
            let chatIdWeak : String  = self.chatId
            DispatchQueue.global().async {
                [weak self] in
                PSDGetChat.get(chatIdWeak, needShowError:true, delegate: self){
                    (chat : PSDChat?) in
                    DispatchQueue.main.async {
                        if self != nil{
                            self?.needShowRating = chat?.showRating ?? false
                            self?.showRateIfNeed()
                            
                            
                            self?.isLoading = false
                            if((chat) != nil){
                                self?.tableMatrix.create(from: chat!)
                                
                                self?.lastMessageFromServer = chat?.messages.last
                                self?.setLastActivityDate()
                                self?.reloadData()
                                
                                self?.removeNoConnectionView()
                                
                                UIView.setAnimationsEnabled(false)
                                    self?.scrollsToBottom(animated: false)
                                    self?.setNeedsLayout()
                                    self?.layoutIfNeeded()
                                    self?.scrollsToBottom(animated: false)
                                    self?.layoutIfNeeded()
                                    self?.scrollsToBottom(animated: false)
                                UIView.setAnimationsEnabled(true)
                                
                            }
                            
                            self?.addRefreshControls()

                        }
                    }
                }
            }
        }
        else{
            DispatchQueue.main.async {
                self.removeNoConnectionView()
                self.tableMatrix.defaultMatrix()
                self.reloadData()
            }
            
        }
    }
    private func setLastActivityDate(){
        if let lastMessage = self.lastMessageFromServer{
            if PyrusServiceDesk.setLastActivityDate(lastMessage.date){
                PyrusServiceDesk.restartTimer()
            }
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
    ///Is chat is new. Chat is new if it has no id or its id is 0. One-chat mode has no id at all, so chat is always not new.
    ///- parameter idChat: is id of chat that need to check.
    static func isNewChat(_ idChat:String)->Bool{
        if((idChat == "" || idChat == "0") && !PyrusServiceDesk.oneChat)
        {
            return true
        }
        return false
    }
    ///update taable matrix
    ///- parameter needProgress: Determines whether the view should respond to updating(need to show error) 
    func updateChat(needProgress:Bool){
        let chatIdWeak : String  = self.chatId
        DispatchQueue.global().async {
            [weak self] in
            PSDGetChat.get(chatIdWeak, needShowError:needProgress, delegate: nil){
                (chat : PSDChat?) in
                if((chat) != nil){
                    DispatchQueue.main.async  {
                        self?.needShowRating = chat?.showRating ?? false
                        self?.showRateIfNeed()
                    }
                    //compare number of messages it two last sections
                    
                    if self?.tableMatrix != nil {
                        self?.tableMatrix.complete(from: chat!, startMessage:self?.lastMessageFromServer){
                            (indexPaths: [IndexPath], sections:IndexSet, _) in
                            DispatchQueue.main.async  {
                                self?.removeNoConnectionView()
                                self?.lastMessageFromServer = chat?.messages.last
                                self?.setLastActivityDate()
                                
                                if indexPaths.count>0 || sections.count>0{
                                    self?.beginUpdates()
                                    if(sections.count>0){
                                        self?.insertSections(sections, with: .none)
                                    }
                                    if(indexPaths.count>0){
                                        self?.insertRows(at: indexPaths, with: .none)
                                    }
                                    self?.endUpdates()
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
    
    private func redrawCell(at indexPath: IndexPath,with message:PSDRowMessage){
        
        //If cell is on the screen and this is attachments cell, we should not reload it because it will restart all animation (bad looking), so just pass progress
        let cellOnScrean :Bool = self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        var needReload = true
        if cellOnScrean && message.attachment != nil{
            let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView
            if attachmentView != nil && message.message.state == .sending{
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
        let cellOnScrean :Bool = self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        if cellOnScrean && message.attachment != nil{
            let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView
            if attachmentView != nil && message.message.state == .sending{
                attachmentView!.progress = message.attachment!.uploadingProgress
                attachmentView?.downloadState = message.message.state
                if(message.message.state == .sending){
                    let stateView = (self.cellForRow(at: indexPath) as? PSDUserMessageCell)?.messageStateView
                    stateView?._messageState = .sent //hide pass animation if uploading attach now
                }
            }
            
        }
        
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
        dateLabel.textColor = .psdLabel
        dateLabel.font = UIFont.systemFont(ofSize: 16.0)
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
                PSDMessageSend.pass(message, to: self.chatId, delegate: self)
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
    func change(_ chatId:String){
        self.chatId = chatId
    }
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
                        print(newIndexPath, movedIndexPath)
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

