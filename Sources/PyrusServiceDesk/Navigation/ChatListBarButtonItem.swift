
import UIKit

///Name for notification when chats' number was received.
let CHATS_NOTIFICATION_NAME = Notification.Name.init("PSDChatsChanging")
///Name for notification when new messages was received.
let MESSAGES_NUMBER_NOTIFICATION_NAME = Notification.Name.init("PSDMessagesNumberChanging")


///UIBarButtonItem, that observers number of chats and new messages, and change self view according to its data. If no messages and chats this button is not enabled.
class ChatListBarButtonItem: UIBarButtonItem {
    private var chatsCount : Int = PyrusServiceDesk.chatsCount
    {
        didSet (oldValue){
            if oldValue != chatsCount{
                 redraw()
            }
           
        }
    }
    private var newMessagesCount : Int = PyrusServiceDesk.newMessagesCount
    {
        didSet (oldValue){
            if oldValue != newMessagesCount{
                redraw()
            }
        }
    }
    private var hasInfo : Bool = false
    {
        didSet(oldValue){
            if !oldValue && hasInfo{
                self.customView = nil
            }
            if oldValue != hasInfo{
                redraw()
            }
        }
    }
    
    ///type of ChatListBarButtonItem
    private enum itemMode {
        ///"No chats" type, item is not enabled
        case empty
        ///"Default" type, item is enabled, no new messages
        case Default
        ///Type with new messages, item is enabled, has info about new messages count
        case hasNewMessage
        ///type with activityView
        case noInfo
    }
    ///Redraws ChatListBarButtonItem. Detect itemMode according to number of chats  and newMessages number and call redraw(mode: itemMode)
    private func redraw()
    {
        if(!hasInfo){
            self.redraw(mode: .noInfo)
        }
        if(self.chatsCount == 0){
            self.redraw(mode: .empty)
        }
        else{
            if(self.newMessagesCount == 0){
                self.redraw(mode: .Default)
            }
            else{
                self.redraw(mode: .hasNewMessage)
            }
        }
    }
    ///Redraws ChatListBarButtonItem according to its itemMode
    private func redraw(mode: itemMode)
    {
        switch mode {
        case .empty:
            drawNoChats()
        case .hasNewMessage:
            drawNewMessage()
        case .noInfo:
            drawNoInfo()
        default:
            drawDefault()
        }
    }
    //Redraws ChatListBarButtonItem with itemMode .noInfo. Make ChatListBarButtonItem enabled, and show UIActivityIndicatorView
    private func drawNoInfo()
    {
        self.isEnabled = true
        let activity : UIActivityIndicatorView = UIActivityIndicatorView()
        activity.frame = CGRect(x: 0, y: 0, width: 66, height: 66)
        activity.color = .darkAppColor
        activity.startAnimating()
        self.customView = activity
    }
    ///Redraws ChatListBarButtonItem with itemMode .empty. Make ChatListBarButtonItem not enabled
    private func drawNoChats()
    {
        self.isEnabled = false
    }
    ///Redraws ChatListBarButtonItem with itemMode .Default. Make ChatListBarButtonItem enabled
    private func drawDefault()
    {
        redraw(image: UIImage.PSDImage(name: "Conversations"))
    }
    ///Redraws ChatListBarButtonItem with itemMode .hasNewMessage. Make ChatListBarButtonItem enabled
    private func drawNewMessage()
    {
        let customImage = imageChangedMessages()
        redraw(image:customImage.withRenderingMode(.alwaysOriginal))//pass original - colored image to BarButtomItem, or it will change it color and we will not see text on it
    }
    
    ///Redraw image named:"ConversationsNew" with new messages count.
    private func imageChangedMessages()->UIImage{
        var initialImage = UIImage.PSDImage(name: "ConversationsNew")
        initialImage = initialImage?.imageWith(color: .darkAppColor) ?? initialImage
        
        let imgeView = UIImageView()
        imgeView.frame = CGRect(x: 0, y: 0, width: initialImage?.size.width ?? 0, height: initialImage?.size.height ?? 0)
        imgeView.image = initialImage
        imgeView.contentMode = .left
        
        
        var messagesString = "\(newMessagesCount)"
        if(newMessagesCount>99){
            messagesString = "99+"
        }
        messagesCountLabel.text = messagesString
        messagesCountLabel.sizeToFit()
        let labelHeight : CGFloat = (initialImage?.size.height ?? 0)/1.4
        messagesCountLabel.frame = CGRect(x: imgeView.frame.size.width - (labelHeight/2.3), y: 0, width: max(messagesCountLabel.frame.size.width + 6,labelHeight), height: labelHeight)
        messagesCountLabel.layer.cornerRadius = min(messagesCountLabel.frame.size.height/2, messagesCountLabel.frame.size.width/2)
        messagesCountLabel.clipsToBounds = true
        imgeView .addSubview(messagesCountLabel)
        
        imgeView.frame = CGRect(x: 0, y: 0, width: imgeView.frame.size.width+messagesCountLabel.frame.size.width, height: initialImage?.size.height ?? 0)
        messagesCountLabel.center = CGPoint(x: messagesCountLabel.center.x, y: imgeView.frame.size.height/2)
        return imgeView.asImage()
    }
    ///Label with new messages count. Using to create new image for item.
    private lazy var messagesCountLabel : UILabel = {
        let label = UILabel()
        label.textColor = UIColor.darkAppColor.isDarkColor ? .white : .black
        label.tintColor = label.textColor
        label.backgroundColor = .darkAppColor
        label.font = .messagesCountLabel
        label.textAlignment = .center
        return label
    }()
    
    ///Redraws ChatListBarButtonItem with image
    ///- parameter image: UIImage to set to ChatListBarButtonItem
    private func redraw(image: UIImage?)
    {
        self.isEnabled = true
        self.image = image
    }
    
    
    
    ///Draw ChatListBarButtonItem and set observers.
    override init() {
        super.init()
        self.hasInfo=checkAvailability()
        redraw()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(changingChats),
            name: CHATS_NOTIFICATION_NAME,
        object: nil)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(changingMessagesNumber),
            name: MESSAGES_NUMBER_NOTIFICATION_NAME,
            object: nil)
    }
    private func checkAvailability()->Bool{
        if(PyrusServiceDesk.newMessagesCount == 0 && PyrusServiceDesk.chatsCount==0 && !PyrusServiceDesk.hasInfo){
            return false
        }
        return true
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    ///Observer for changes in chats number
    @objc private func changingChats(notification: NSNotification){
        self.hasInfo = true
        if let receivedChats = notification.object as? Int {
            self.chatsCount = receivedChats
        }
        
        
    }
    
    ///Observer for changes in new messages number
    @objc private func changingMessagesNumber(notification: NSNotification){
        self.hasInfo = true
        if let messages = notification.object as? Int {
            self.newMessagesCount = messages
        }
    }
    deinit {
        NotificationCenter.default.removeObserver(self, name: CHATS_NOTIFICATION_NAME, object: nil)
        NotificationCenter.default.removeObserver(self, name: MESSAGES_NUMBER_NOTIFICATION_NAME, object: nil)
    }
}
private extension UIFont {
    static let messagesCountLabel = CustomizationHelper.systemFont(ofSize: 12.0)
}
