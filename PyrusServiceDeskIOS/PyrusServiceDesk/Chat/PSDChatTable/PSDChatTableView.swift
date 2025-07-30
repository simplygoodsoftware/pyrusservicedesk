import UIKit
protocol PSDChatTableViewDelegate: NSObjectProtocol {
    func showLinkOpenAlert(_ linkString: String)
    func send(_ message:String,_ attachments:[PSDAttachment])
    func sendAgainMessage(indexPath: IndexPath)
    func deleteMessage(indexPath: IndexPath)
    func refresh()
    func reloadChat()
    func updateNoConnectionVisible(visible: Bool)
    func updateScrollButton(isAtBottom: Bool, isDragging: Bool)
    func resignFirstResponderFromInputView()
}

class PSDChatTableView: PSDTableView {
    ///The id of chat that is shown in table view
    weak var chatDelegate: PSDChatTableViewDelegate?
    private let footerHeight : CGFloat = 10.0
    private let BOTTOM_INFELICITY : CGFloat = 30.0
    private static let userCellId = "CellUser"
    private static let supportCellId = "CellSupport"
    var tableMatrix: [[PSDRowMessage]] = [[PSDRowMessage]()]
    private var heightsMap: [IndexPath: CGFloat] = [IndexPath : CGFloat]()
    private var cellConfigurator: PSDChatCellConfigurator?
    private var customDataSource: Any?
    
    private lazy var buttonsView: ButtonsView = {
        let view = ButtonsView(frame: .zero)
        tableHeaderView = view
        view.autoresizingMask = [.flexibleHeight]
        view.tapDelegate = self
        return view
    }()
    
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
    
    lazy var noConnectionView: PSDNoConnectionView = {
        let view = PSDNoConnectionView.init(frame: self.frame)
        view.delegate = self
        return view
    }()
    
    override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: .grouped)
        self.allowsMultipleSelection = false
        self.allowsSelection = false
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        removeRefreshControls()
    }
    
    @objc private func refreshChat(sender: PSDRefreshControl) {
        if !(self.superview?.subviews.contains(self.noConnectionView) ?? false) && sender.isRefreshing {
            chatDelegate?.refresh()
        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        var newFrame = buttonsView.frame
        if newFrame.size.height != buttonsView.collectionView.contentSize.height {
            newFrame.size.width = frame.size.width
            newFrame.size.height = buttonsView.collectionView.contentSize.height
            buttonsView.frame = newFrame
            tableHeaderView = buttonsView
            buttonsView.collectionView.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
        }
    }
    
    override func recolor() {
        customRefresh.tintColor = CustomizationHelper.textColorForTable
        bottomRefresh.tintColor = CustomizationHelper.textColorForTable
        reloadAll(animated: false)
    }
    
    ///Setups needed properties to table view
    func setupTableView() {
        delegate = self
        backgroundColor = .clear
        cellConfigurator = PSDChatCellConfigurator(tableView: self)
        keyboardDismissMode = .interactive
        separatorColor = .clear
        
        if #available(iOS 13.0, *) {
            let newDataSource = KBDiffableDataSource.createDataSource(for: self, cellCreator: self)
            customDataSource = newDataSource
            dataSource = newDataSource
        } else {
            dataSource = self
        }
    }
    
    func updateRows() {
        let oldContentOffset = contentOffset
        let oldContentSize = contentSize
        
        reloadAll(animated: true)
        
        self.scrollToBottomAfterRefresh(with: oldContentOffset, oldContentSize: oldContentSize)
    }
    
    func addRow(scrollsToBottom: Bool) {
        let inset = contentInset.top - 20
        let needAnimate = contentOffset.y <= -inset
        reloadAll(animated: needAnimate)
        if scrollsToBottom {
            self.scrollsToBottom(animated: true)
        }
    }
    
    func reloadChat()  {
        self.bottomPSDRefreshControl.insetHeight = 0.0
        self.removeRefreshControls()
        heightsMap = [IndexPath : CGFloat]()
        tableMatrix = [[PSDRowMessage]()] // clean old chat
        DispatchQueue.main.async {
            self.reloadAll(animated: false)
            self.isLoading = true
        }
    }
    
    func drawTableWithData() {
        isLoading = false
        reloadAll(animated: false)
        removeNoConnectionView()
    }
    
    func endRefreshing() {
        customRefresh.endRefreshing()
        bottomRefresh.endRefreshing()
    }
    
    func updateButtonsView(buttons: [ButtonData]?) {
        buttonsView.updateWithButtons(buttons, width: self.frame.size.width)
        buttonsView.collectionView.collectionViewLayout.invalidateLayout()
    }
    
    func showNoConnectionView() {
        EventsLogger.logEvent(.resignFirstResponder, additionalInfo: "while show noConnectionView")
        if !(superview?.subviews.contains(noConnectionView) ?? false),
           lastRow() == nil {
            superview?.addSubview(noConnectionView)
            chatDelegate?.updateNoConnectionVisible(visible: true)
            (findViewController()?.inputAccessoryView as? PSDMessageInputView)?.inputTextView.resignFirstResponder()
            findViewController()?.resignFirstResponder()
        }
    }
    
    func removeNoConnectionView() {
        if self.superview?.subviews.contains(self.noConnectionView) ?? false {
            self.noConnectionView.removeFromSuperview()
            self.findViewController()?.becomeFirstResponder()
            chatDelegate?.updateNoConnectionVisible(visible: false)
        }
    }
        
    ///Scroll tableview to its bottom position without animation
    func scrollsToBottom(animated: Bool) {
        if tableMatrix.count > 0, tableMatrix[0].count > 0 {
            scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: animated)
            layoutIfNeeded()
            setNeedsLayout()
            scrollToRow(at: IndexPath(row: 0, section: 0), at: .top, animated: animated)
        }
        
        //chatDelegate?.updateScrollButton(isHidden: true)
    }
    
    func redrawCell(at indexPath: IndexPath, with message: PSDRowMessage) {
        //If cell is on the screen and this is attachments cell, we should not reload it because it will restart all animation (bad looking), so just pass progress
        let cellOnScrean = true //self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        var needReload = true
        if cellOnScrean && message.attachment != nil {
            let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView
            if attachmentView != nil && message.message.state == .sending {
                needReload = false
                self.redrawSendingAttachmentCell(at: indexPath, with: message)
            }
            
        }
        //Redraw cell with new Data
        if needReload {
            if cellOnScrean {
                if let cell = self.cellForRow(at: indexPath) as? PSDChatMessageCell {
                    cell.draw(message: message, width: frame.size.width)
                    PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate:self, animated: false)
                }
            }
        }
    }
    
    func reloadAll(animated: Bool) {
        if #available (iOS 13.0, *) {
            reloadWithDiffableDataSource(animated: animated)
        } else {
            reloadData()
        }
    }
}

private extension PSDChatTableView {
    @available(iOS 13.0, *)
    func reloadWithDiffableDataSource(animated: Bool, completion: (() -> Void)? = nil) {
        guard
            let dataSource = self.customDataSource as? KBDiffableDataSource
        else {
            return
        }
        var snapshot = NSDiffableDataSourceSnapshot<PSDChatSectionModel, PSDRowMessage>()
        snapshot.deleteAllItems()
        
        for (section, sectionData) in tableMatrix.enumerated() {
            let newSection = PSDChatSectionModel(title: tableMatrix.date(of: section)?.asString() ?? "")
            snapshot.appendSections([newSection])
            snapshot.appendItems(sectionData, toSection: newSection)
        }
        dataSource.apply(snapshot, animatingDifferences: animated, completion: completion)
        self.dataSource = dataSource
    }
    
    func removeRefreshControls() {
        removeTopRefreshControl()
        removeBottomRefreshControl()
    }
    
    func removeBottomRefreshControl() {
        self.bottomRefresh.endRefreshing()
        self.bottomRefresh.removeFromSuperview()
    }
    
    func removeTopRefreshControl() {
        self.customRefresh.endRefreshing()
        self.customRefresh.removeFromSuperview()
    }
    
    ///Scrolls table to bottom after refresh, if table view was in bottom scroll position and new messages received
    func scrollToBottomAfterRefresh(with oldOffset: CGPoint?, oldContentSize: CGSize?) {
        guard let oldOffset = oldOffset, let oldContentSize = oldContentSize else {
            return
        }
        self.setNeedsLayout()
        self.layoutIfNeeded()
        let expectedBottomOffset = oldContentSize.height - (self.frame.size.height - contentInset.top - contentInset.bottom)
        let hasChanges = oldContentSize != self.contentSize
        if contentOffset.y <= 0 {//expectedBottomOffset - BOTTOM_INFELICITY < oldOffset.y && hasChanges{
            self.scrollsToBottom(animated: true)
        }
    }

    ///Returns last PSDChatMessageCell in tableView
    func lastRow() -> PSDChatMessageCell? {
        return self.cellForRow(at: lastIndexPath()) as? PSDChatMessageCell
    }
    
    ///Returns last IndexPath in tableView
    func lastIndexPath() -> IndexPath {
        let row: Int = tableMatrix.last?.count ?? 1
        let section = tableMatrix.count > 0 ? tableMatrix.count - 1 : 0
        let index = IndexPath(row: row > 0 ? row - 1 : 0, section: section)
        return index
    }
    
    ///Redraw cell with sending attachment - pass progress and download state, hide stateView
    func redrawSendingAttachmentCell(at indexPath: IndexPath, with message: PSDRowMessage) {
        let cellOnScreen = self.indexPathsForVisibleRows?.contains(indexPath) ?? false
        guard cellOnScreen, let attachment = message.attachment
        else { return }
        
        guard let attachmentView = (self.cellForRow(at: indexPath) as? PSDChatMessageCell)?.cloudView.attachmentView,
              attachmentView.attachment?.localId == attachment.localId,
              message.message.state == .sending
        else {
            return
        }
        attachmentView.progress = attachment.uploadingProgress
        attachmentView.downloadState = message.message.state
        
        //Было         guard message.message.state == .sending else {
        //выглядит как баг, зачем менять с sending на sent???

        guard message.message.state == .sent else {
            return
        }
        let stateView = (self.cellForRow(at: indexPath) as? PSDUserMessageCell)?.messageStateView
        stateView?._messageState = .sent //hide pass animation if uploading attach now
    }
}

extension PSDChatTableView: UITableViewDelegate, UITableViewDataSource {
    //MARK: table delegate and dataSourse
    func numberOfSections(in tableView: UITableView) -> Int {
        return tableMatrix.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableMatrix.count > section ? tableMatrix[section].count : 0
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let message: PSDRowMessage
        if tableMatrix.count > indexPath.section && tableMatrix[indexPath.section].count > indexPath.row {
            message = tableMatrix[indexPath.section][indexPath.row]
        }
        else {
            message = PSDObjectsCreator.createWelcomeMessage()
        }
        
        let cell = cellConfigurator?.getCell(model: tableMatrix, indexPath: indexPath) ?? PSDChatMessageCell()
        if let userCell = cell as? PSDUserMessageCell {
            userCell.delegate = self
        } else if let supportCell = cell as? PSDSupportMessageCell, supportCell.needShowAvatar {
            supportCell.avatarView.owner = message.message.owner
            PSDSupportImageSetter.setImage(for: message.message.owner, in: supportCell.avatarView, delagate: self, isSupport: message.message.isSupportMessage)
        }
        
        cell.draw(message: message, width: frame.size.width)
        PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate: self, animated: false)
        self.redrawSendingAttachmentCell(at: indexPath, with: message)
        cell.cloudView.messageTextView.linkDelegate = self
        cell.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        heightsMap[indexPath] = cell.frame.size.height
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
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
        view.transform = CGAffineTransform(rotationAngle: .pi)
        return view
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return headerHeight
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return UIView.init(frame: CGRect(x: 0, y: 0, width: frame.size.width, height: footerHeight))
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
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
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
      //  guard scrollView.isDragging else { return }
        let contentOffsetY = scrollView.contentOffset.y
        let inset = contentInset.top + contentInset.bottom - 40

        let isAtBottom = contentOffsetY <= -inset
        chatDelegate?.updateScrollButton(isAtBottom: isAtBottom, isDragging: scrollView.isDragging)
    }
    
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        let contentOffsetY = scrollView.contentOffset.y
        let inset = contentInset.top + contentInset.bottom - 40

        let isAtBottom = contentOffsetY <= -inset
        chatDelegate?.updateScrollButton(isAtBottom: isAtBottom, isDragging: true)
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
extension PSDChatTableView: PSDNoConnectionViewDelegate{
    func retryPressed() {
        removeNoConnectionView()
        chatDelegate?.reloadChat()
    }
}

//MARK: PSDChatMessageCellDelegate
extension PSDChatTableView: PSDChatMessageCellDelegate {
    func showAlert() {
        chatDelegate?.resignFirstResponderFromInputView()
    }
    
    ///Send message one more time.
    func sendAgainMessage(from cell: PSDChatMessageCell) {
        if let indexPath = self.indexPath(for: cell) {
            if let cell = cell as? PSDUserMessageCell {
                cell.messageStateView._messageState = .sending
            }
            chatDelegate?.sendAgainMessage(indexPath: indexPath)
        }
    }
    
    ///Delete message from self and from storage
    func deleteMessage(from cell:PSDChatMessageCell) {
        if let indexPath = self.indexPath(for: cell) {
            chatDelegate?.deleteMessage(indexPath: indexPath)
        }
    }
}

//MARK: PSDPreviewSetterDelegate
extension PSDChatTableView: PSDPreviewSetterDelegate {
    ///Change message local id - redraw it if it's visible. Didn't call reloadCell - so can't change cell height, or break animation.
    func reloadCells(with attachmentId: String) {
        for cell in self.visibleCells {
            if let cell = cell as? PSDChatMessageCell {
                if let indexPath = self.indexPath(for: cell) {
                    if tableMatrix.has(indexPath:indexPath) {
                        let message: PSDRowMessage = tableMatrix[indexPath.section][indexPath.row]
                        if message.attachment?.localId == attachmentId {
                            PSDPreviewSetter.setPreview(of: message.attachment, in: cell.cloudView.attachmentView, delegate: nil, animated: true)
                        }
                    }
                }
            }
        }
    }
}

//MARK: PSDSupportImageSetterDelegate
extension PSDChatTableView: PSDSupportImageSetterDelegate {
    ///Change message's user's avatar - redraw it if it's visible. Didn't call reloadCell - so can't change cell height, or break animation.
    func reloadCells(with owner:PSDUser, isSupport: Bool) {
        for cell in self.visibleCells{
            if let cell = cell as? PSDSupportMessageCell {
                if cell.avatarView.owner == owner && cell.avatarView.owner != nil {
                    PSDSupportImageSetter.setImage(for: cell.avatarView.owner!, in: cell.avatarView, delagate: nil, isSupport: isSupport)
                }
            }
        }
    }
}

private extension UIFont {
    static let dateLabel = CustomizationHelper.systemFont(ofSize: 16.0)
    
}

extension PSDChatTableView: LinkDelegate {
    func showLinkOpenAlert(_ linkString: String) {
        chatDelegate?.showLinkOpenAlert(linkString)
    }
}

extension PSDChatTableView: ButtonsCollectionDelegate {
    func didTapOnButton(_ text: ButtonData) {
        if let url = text.url?.absoluteString {
            chatDelegate?.showLinkOpenAlert(url)
        } else if let text = text.string {
            chatDelegate?.send(text, [])
        }
    }
}
