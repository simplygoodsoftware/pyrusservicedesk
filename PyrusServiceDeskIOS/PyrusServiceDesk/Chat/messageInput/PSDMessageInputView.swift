
import UIKit

protocol PSDMessageInputViewDelegate: class {
    func send(_ message:String,_ attachments:[PSDAttachment])
    func sendRate(_ rateValue:Int)
}
let DEFAULT_LAYOUT_MARGINS : CGFloat = 8
let BUTTONS_CORNER_RADIUS : CGFloat = 8
class PSDMessageInputView: UIView, PSDMessageTextViewDelegate, PSDMessageSendButtonDelegate {
    private let RATE_HEIGHT : CGFloat = 92
    private static let heightForAttach : CGFloat = 30
    private static let interItemSpaceForAttach : CGFloat = 0.1
    weak var delegate: PSDMessageInputViewDelegate?
    
    ///Button that open attachment menu
    var attachmentsAddButton :AttachmentsAddButton!
    ///Text view that change its height
    var inputTextView : PSDMessageTextView!
    ///Button, if has no text - is not enabled, if has - send input text.
    var sendButton : PSDMessageSendButton!
    ///Top separator
    var topGrayLine : UIView!
    ///Visible part of input
    var backgroundView : UIView!
    ///The view with rate buttons
    private var textRateView: RateViewProtocol!
    private var emojiRateView: RateViewProtocol!
    private var rateHeight: CGFloat = 0
    private lazy var rateLabel: UILabel = {
        let label = UILabel()
        label.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(0.6)
        label.font = .systemFont(ofSize: 12)
        label.textAlignment = .center
        label.text = "PleaseEvaluateQuality".localizedPSD()
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var textRateLabel: UILabel = {
        let label = UILabel()
        label.textColor = CustomizationHelper.textColorForTable.withAlphaComponent(0.6)
        label.font = .systemFont(ofSize: 12)
        label.textAlignment = .center
        label.text = "PleaseEvaluateQuality".localizedPSD()
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    ///Stack with attachments
    private var attachmentsCollection : AttachmentCollectionView!
    private var attachmentsPresenter : AttachmentCollectionViewPresenterProtocol!
    let distAddToText : CGFloat = 6
    let distTextToSend : CGFloat = 6
    let distToAdd : CGFloat = 21
    let distToSend : CGFloat = 15
    private static let attachmentsHeight : CGFloat = 80
    var showRate = false {
        didSet {
            switch RatingType(rawValue: PyrusServiceDesk.ratingSettings.type) {
            case .text:
                let size = CGFloat(PyrusServiceDesk.ratingSettings.size)
                let rateItemsHeihgt = size * 40.0 + (size - 1) * 8.0
                rateHeight = 12 + 36 + rateItemsHeihgt
                textRateView.configure(with: PyrusServiceDesk.ratingSettings.ratingTextValues ?? [])
                rateHeightConstraint?.constant = showRate ? rateHeight : 0
                rateTopConstraint?.isActive = true
                textRateTopConstraint?.isActive = false
                textRateHeightConstraint?.constant = 0
                textRateView.isHidden = !showRate
                emojiRateView.isHidden = true
            default:
                rateHeight = 92
                emojiRateView.configure(with: RatingType(rawValue: PyrusServiceDesk.ratingSettings.type)?.rateArray(size: PyrusServiceDesk.ratingSettings.size).reversed() ?? [])
                textRateHeightConstraint?.constant = showRate ? rateHeight : 0
                rateTopConstraint?.isActive = false
                textRateTopConstraint?.isActive = true
                rateHeightConstraint?.constant = 0
                emojiRateView.isHidden = !showRate
                textRateView.isHidden = true
            }
            guard oldValue != showRate
            else {
                return
            }
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        
        self.backgroundColor = UIColor.clear
        
        self.backgroundView = UIView()
        self.backgroundView.frame = frame
        
        topGrayLine = UIView.init(frame: CGRect(x: 0, y: 0, width: frame.size.width, height: 0.5))
        topGrayLine.backgroundColor = UIColor.psdSeparator
        
        var x : CGFloat = distToAdd
        
        let collectionLayoyt = UICollectionViewFlowLayout.init()
        collectionLayoyt.scrollDirection = .horizontal
        collectionLayoyt.itemSize = CGSize(width: PSDMessageInputView.attachmentsHeight, height: PSDMessageInputView.attachmentsHeight + AttachmentCollectionViewCell.distToBoard)
        collectionLayoyt.minimumInteritemSpacing = PSDMessageInputView.interItemSpaceForAttach
        collectionLayoyt.minimumLineSpacing = PSDMessageInputView.interItemSpaceForAttach
        attachmentsCollection = AttachmentCollectionView.init(frame: CGRect.zero, collectionViewLayout: collectionLayoyt)
        attachmentsCollection.attachmentChangeDelegate = self
        attachmentsPresenter = AttachmentCollectionViewPresenter.init(view: attachmentsCollection)
        attachmentsCollection.presenter = attachmentsPresenter
        
        attachmentsAddButton = AttachmentsAddButton.init(frame: CGRect(x: x, y: 0, width: defaultTextHeight, height: defaultTextHeight))
        attachmentsAddButton.delegate = self
        x = x + attachmentsAddButton.frame.size.width + distAddToText

        
        sendButton = PSDMessageSendButton.init()
        sendButton.frame=CGRect(x: frame.size.width - sendButton.frame.size.width - distToSend , y: 0, width: sendButton.frame.size.width, height: defaultTextHeight)
        sendButton.delegate = self
        
        
        inputTextView = PSDMessageTextView(frame: CGRect(x: x, y: 0, width: sendButton.frame.origin.x - distTextToSend - x, height: defaultTextHeight))
        inputTextView.messageDelegate = self
       
        textRateView = PSDTextRateView(frame: .zero)
        emojiRateView = PSDEmojiRateView(frame: .zero)
        textRateView.tapDelegate = self
        emojiRateView.tapDelegate = self
    
        self.addSubview(backgroundView)
        backgroundView.addSubview(topGrayLine)
        backgroundView.addSubview(attachmentsAddButton)
        backgroundView.addSubview(inputTextView)
        backgroundView.addSubview(sendButton)
        backgroundView.addSubview(attachmentsCollection)
        backgroundView.addSubview(textRateView)
        backgroundView.addSubview(emojiRateView)
        textRateView.addSubview(rateLabel)
        emojiRateView.addSubview(textRateLabel)
        textRateView.isHidden = !showRate
        emojiRateView.isHidden = !showRate
        addConstraints()
    }
    ///In current version this function replase added to message attachments
    func addAttachment(_ attachment : PSDAttachment){
        attachmentsHeightConstraint?.constant = PSDMessageInputView.attachmentsHeight
        setNeedsLayout()
        attachmentsPresenter.addAttachment(attachment)
        checkSendButton()
    }
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let view = super.hitTest(point, with: event)
        return view == self ? nil : view
    }
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    override var intrinsicContentSize: CGSize {
        return CGSize.zero
    }
    func setToDefault(){
        if inputTextView.text.count == 0{
            sendButton.isEnabled = false
        }
        else{
            sendButton.isEnabled = true
        }
        
    }
    ///Clear text.
    func clearAll(){
        inputTextView.text = ""
        inputTextView.textViewDidChange(inputTextView)
        attachmentsPresenter?.cleanAll()
        checkCollectionHeight()
        checkSendButton()
    }
    
    //MARK: Delegate methods
    func textViewChanged()
    {
        checkSendButton()
    }
    func sendMessage()
    {
        inputTextView.text = inputTextView.text.trimmingCharacters(in: .whitespacesAndNewlines)
        inputTextView.textViewDidChange(inputTextView)
        if(inputTextView.text.count>0 || attachmentsPresenter.attachmentsNumber() > 0){
            self.delegate?.send(inputTextView.text, attachmentsPresenter.attachmentsForSend())
            inputTextView.text = ""
            inputTextView.textViewDidChange(inputTextView)
            attachmentsPresenter?.cleanAll()
            checkCollectionHeight()
        }
        checkSendButton()
    }
    ///Check is send button need to enabled or not
    private func checkSendButton(){
        sendButton.isEnabled = (inputTextView.text.count != 0 || attachmentsPresenter.attachmentsNumber() > 0)
    }
    private func checkCollectionHeight(){
        attachmentsHeightConstraint?.constant = attachmentsPresenter.attachmentsNumber() > 0 ? PSDMessageInputView.attachmentsHeight : 0
    }
    
    
    //MARK: drawing constraints
    
    let defaultTextHeight : CGFloat = 36.0
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if UIDevice.current.orientation.isLandscape{
            heightConstraint?.constant = inputTextView.maxHorizontalHeight()
        }
        else{
            heightConstraint?.constant = inputTextView.maxVerticalHeight()
        }
    }
    
    ///Text field height constraint. Change when isLandscape.
    var heightConstraint  : NSLayoutConstraint?
    private var attachmentsHeightConstraint :NSLayoutConstraint?
    private var rateHeightConstraint: NSLayoutConstraint?
    private var textRateHeightConstraint: NSLayoutConstraint?
    private var rateTopConstraint: NSLayoutConstraint?
    private var textRateTopConstraint: NSLayoutConstraint?
    
    func addConstraints() {
        guard let inputTextView = inputTextView, let sendButton = sendButton else{
            return
        }
        self.autoresizingMask = [.flexibleHeight, .flexibleWidth, .flexibleBottomMargin]
        addBackgroundViewConstraints()
        addTopGrayLineConstraints()
        
        textRateView.translatesAutoresizingMaskIntoConstraints = false
        textRateView.addZeroConstraint([.left,.right])
        rateHeightConstraint = textRateView.heightAnchor.constraint(equalToConstant: showRate ? rateHeight : 0)
        rateTopConstraint = textRateView.topAnchor.constraint(equalTo: superview?.topAnchor ?? backgroundView.topAnchor, constant: 0)
        rateHeightConstraint?.isActive = true
        rateTopConstraint?.isActive = true
        
        emojiRateView.translatesAutoresizingMaskIntoConstraints = false
        emojiRateView.addZeroConstraint([.left,.right])
        textRateHeightConstraint = emojiRateView.heightAnchor.constraint(equalToConstant: showRate ? rateHeight : 0)
        textRateTopConstraint = emojiRateView.topAnchor.constraint(equalTo: superview?.topAnchor ?? backgroundView.topAnchor, constant: 0)
        textRateHeightConstraint?.isActive = true
        
        attachmentsCollection.translatesAutoresizingMaskIntoConstraints = false
        attachmentsCollection.addZeroConstraint([.left,.right])
        attachmentsCollection.topAnchor.constraint(equalTo: topGrayLine.bottomAnchor, constant: 0).isActive = true
        attachmentsHeightConstraint = attachmentsCollection.heightAnchor.constraint(equalToConstant: 0)
        attachmentsHeightConstraint?.isActive = true
        
        let distToText : CGFloat = DEFAULT_LAYOUT_MARGINS
        
        
        inputTextView.translatesAutoresizingMaskIntoConstraints = false
        sendButton.translatesAutoresizingMaskIntoConstraints = false
        attachmentsAddButton.translatesAutoresizingMaskIntoConstraints = false
        
        
        NSLayoutConstraint.activate([
            textRateLabel.topAnchor.constraint(equalTo: emojiRateView.topAnchor, constant: 12),
            textRateLabel.centerXAnchor.constraint(equalTo: emojiRateView.centerXAnchor),
            
            rateLabel.topAnchor.constraint(equalTo: textRateView.topAnchor, constant: 12),
            rateLabel.centerXAnchor.constraint(equalTo: textRateView.centerXAnchor),
            
            emojiRateView.bottomAnchor.constraint(equalTo: textRateView.topAnchor),
            
        ])
        superview?.topAnchor.constraint(equalTo: emojiRateView.topAnchor).isActive = true

        backgroundView.addConstraint(NSLayoutConstraint(
            item: inputTextView,
            attribute: .top,
            relatedBy: .equal,
            toItem: attachmentsCollection,
            attribute: .bottom,
            multiplier: 1,
            constant:distToText))
        
        attachmentsAddButton.leftAnchor.constraint(
            equalTo: backgroundView.layoutMarginsGuide.leftAnchor,
            constant: 0
            ).isActive = true
        attachmentsAddButton.bottomAnchor.constraint(
            equalTo: backgroundView.layoutMarginsGuide.bottomAnchor,
            constant: 0
            ).isActive = true
        
        attachmentsAddButton.addSizeConstraint([.width,.height], constant: defaultTextHeight)
        
        
        backgroundView.addConstraint(NSLayoutConstraint(
            item: inputTextView,
            attribute: .height,
            relatedBy: .greaterThanOrEqual,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:defaultTextHeight))
        
        heightConstraint = NSLayoutConstraint(
            item: inputTextView,
            attribute: .height,
            relatedBy: .lessThanOrEqual,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:inputTextView.maxVerticalHeight())
        backgroundView.addConstraint(heightConstraint!)
        
        inputTextView.bottomAnchor.constraint(
            equalTo: backgroundView.layoutMarginsGuide.bottomAnchor,
            constant:0
            ).isActive = true
        backgroundView.addConstraint(NSLayoutConstraint(
            item: inputTextView,
            attribute: .leading,
            relatedBy: .equal,
            toItem: attachmentsAddButton,
            attribute: .trailing,
            multiplier: 1,
            constant:distAddToText))
        backgroundView.addConstraint(NSLayoutConstraint(
            item: sendButton,
            attribute: .leading,
            relatedBy: .equal,
            toItem: inputTextView,
            attribute: .trailing,
            multiplier: 1,
            constant:distTextToSend))
        backgroundView.addConstraint(NSLayoutConstraint(
            item: sendButton,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:defaultTextHeight))
        backgroundView.addConstraint(NSLayoutConstraint(
            item: sendButton,
            attribute: .width,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1,
            constant:sendButton.frame.size.width))
        sendButton.rightAnchor.constraint(
            equalTo: backgroundView.layoutMarginsGuide.rightAnchor,
            constant: -7.0
            ).isActive = true
        sendButton.bottomAnchor.constraint(
            equalTo: backgroundView.layoutMarginsGuide.bottomAnchor,
            constant: 0
            ).isActive = true
        
    }
    private func addBackgroundViewConstraints(){
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        backgroundView.addZeroConstraint([.top,.bottom])
        backgroundView.addZeroConstraint([.left,.right])
    }
    private func addTopGrayLineConstraints(){
        topGrayLine.translatesAutoresizingMaskIntoConstraints = false
        topGrayLine.addZeroConstraint([.left,.right])
        topGrayLine.addSizeConstraint([.height], constant: 0.5)
        topGrayLine.topAnchor.constraint(equalTo: textRateView.bottomAnchor).isActive = true
    }
}

extension PSDMessageInputView : AttachmentCollectionViewDelegateProtocol{
    func attachmentRemoved(){
        checkCollectionHeight()
        checkSendButton()
    }
}
extension PSDMessageInputView: AttachmentsAddButtonDelegate{
    func attachmentChoosed(_ data:Data, _ url:URL?)
    {
        let attachment = PSDObjectsCreator.createAttachment(data,url)
        addAttachment(attachment)
    }
}
extension PSDMessageInputView: PSDRateViewDelegate {
    func didTapRate(_ rateValue: Int) {
        delegate?.sendRate(rateValue)
        showRate = false
    }
}
