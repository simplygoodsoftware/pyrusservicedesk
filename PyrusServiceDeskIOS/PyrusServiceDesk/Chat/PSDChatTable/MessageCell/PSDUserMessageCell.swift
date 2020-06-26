
import UIKit
protocol PSDChatMessageCellDelegate : class{
    ///Pass to delegate  that PSDMessageStateButton was pressed while messageState = .cantSend, so need to try send one more time
    func sendAgainMessage(from cell:PSDChatMessageCell)
    ///Pass to delegate  that PSDMessageStateButton was pressed while messageState = .cantSend, so need to delete message from local storage
    func deleteMessage(from cell:PSDChatMessageCell)
}
class PSDUserMessageCell: PSDChatMessageCell {
    weak var delegate: PSDChatMessageCellDelegate?
    private weak var message : PSDRowMessage?
    ///A view that displays the current status of the message (see messageState)
    let messageStateView :PSDMessageStateButton = {
        let button = PSDMessageStateButton()
        return button;
    }()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
       
        cloudView.color = .brightColor
        messageStateView.delegate = self
        self.contentView.addSubview(messageStateView)
        
        cloudView.delegate = self
        
        userMessageConstraints()
        stateConstraints()
    }
        
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func draw(message:PSDRowMessage)
    {
        super.draw(message: message)
        self.message = message
        messageStateView._messageState = (message.text.count > 0 || message.rating != nil) ? message.message.state : .sent
        updateTopMessageConstrint()
    }
    private func cancelMessage(){
        self.delegate?.deleteMessage(from: self)
    }
    private func retryMessage(){
        self.delegate?.sendAgainMessage(from:self)
    }
    
    
    //restart animation
    override func prepareForReuse() {
        super.prepareForReuse()
        messageStateView.restart()
    }
    private func stateConstraints(){
        self.messageStateView.translatesAutoresizingMaskIntoConstraints = false
        self.contentView.addConstraint(NSLayoutConstraint(
            item: messageStateView,
            attribute: .trailing,
            relatedBy: .equal,
            toItem: self.cloudView,
            attribute: .leading,
            multiplier: 1,
            constant:-bottomDistance))
        self.contentView.addConstraint(NSLayoutConstraint(
            item: messageStateView,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: self.cloudView,
            attribute: .bottom,
            multiplier: 1,
            constant:-bottomDistance))
        messageStateView.addSizeConstraint([.height,.width],constant:messageStateView.stateSize)
    }
    private func userMessageConstraints(){
        self.contentView.addConstraint(NSLayoutConstraint(
            item: cloudView,
            attribute: .trailing,
            relatedBy: .equal,
            toItem: timeLabel,
            attribute: .leading,
            multiplier: 1,
            constant:-TO_BOARD_DISTANCE))
        self.contentView.addConstraint(NSLayoutConstraint(
            item: cloudView,
            attribute: .leading,
            relatedBy: .greaterThanOrEqual,
            toItem: self.contentView,
            attribute: .leading,
            multiplier: 1,
            constant:AVATAR_SIZE+(TO_BOARD_DISTANCE*2)))
    }
    private func updateTopMessageConstrint(){
         self.topMessageConstraint?.constant = needShowName && !firstMessageInDate ? PSDChatMessageCell.nameTopDistance : bottomDistance
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }   
}
extension PSDUserMessageCell: PSDRetryActionDelegate {
    ///Sending not passed message, send one more time
    func tryShowRetryAction(){
        guard let message = message, message.message.state == .cantSend, let viewController = self.findViewController() else{
            return
        }
        guard message.attachment != nil || message.text.count > 0 || message.rating != nil else{
            return
        }
        let actions : [UIAlertAction] = [
            UIAlertAction(title: "RetryButton".localizedPSD(), style: .default, handler: { (action) -> Void in self.retryMessage()}),
            UIAlertAction(title: "DeleteButton".localizedPSD(), style: .destructive, handler: { (action) -> Void in self.cancelMessage()}),
        ]
        if let vc  =  UIApplication.topViewController() as? PSDChatViewController{
            (vc.inputAccessoryView as? PSDMessageInputView)?.prepairToShowAlert()
        }
        showMenuAlert(actions, on: viewController, sourseView: nil)
    }
}
