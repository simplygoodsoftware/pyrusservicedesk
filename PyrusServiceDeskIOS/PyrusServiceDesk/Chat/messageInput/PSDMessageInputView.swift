
import UIKit

protocol PSDMessageInputViewDelegate: class {
    func send(_ message:String,_ attachments:[PSDAttachment])
    func sendRate(_ rateValue:Int)
    func addButtonTapped()
    func addAttachment()
}
let DEFAULT_LAYOUT_MARGINS : CGFloat = 8
let BUTTONS_CORNER_RADIUS : CGFloat = 8
class PSDMessageInputView: UIView, PSDMessageTextViewDelegate,PSDMessageSendButtonDelegate {
    
    static let RATE_HEIGHT : CGFloat = 64
    private static let heightForAttach : CGFloat = 30
    private static let interItemSpaceForAttach : CGFloat = 0.1
    weak var delegate: PSDMessageInputViewDelegate?
    
    ///Button that open attachment menu
    var attachmentsAddButton: AttachmentsAddButton!
    ///Text view that change its height
    var inputTextView: PSDMessageTextView!
    ///Button, if has no text - is not enabled, if has - send input text.
    var sendButton: PSDMessageSendButton!
    ///Top separator
    var topGrayLine : UIView!
    ///Visible part of input
    var backgroundView : UIView!
    ///The view with rate buttons
    private var rateView: PSDRateView!
    ///Stack with attachments
    private var attachmentsCollection : AttachmentCollectionView!
    private var attachmentsPresenter : AttachmentCollectionViewPresenterProtocol!
    let distAddToText : CGFloat = 6
    let distTextToSend : CGFloat = 6
    let distToAdd : CGFloat = 21
    let distToSend : CGFloat = 15
    static let attachmentsHeight : CGFloat = 80
    
    private(set) var recordingObject: AudioRecordingObject?
    var recordButton: VoiceRecordButton?
    var voiceRecordView: VoiceRecordView?
    var recordBuble: BubleTextView?
    
    private(set) lazy var addVoiceMessageButton: VoiceRecordButton = {
        let button = VoiceRecordButton()
        button.translatesAutoresizingMaskIntoConstraints = false
//        button.addTarget(self, action: #selector(buttonPressed(_:)), for: .touchUpInside)
        return button
    }()
    
    var showRate = false {
        didSet {
            guard oldValue != showRate 
            else {
                return
            }
            rateHeightConstraint?.constant = showRate ? PSDMessageInputView.RATE_HEIGHT : 0
            rateView.isHidden = !showRate
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        
        self.backgroundColor = UIColor.clear
        
        self.backgroundView = UIView()
        self.backgroundView.frame = frame
        
     //   backgroundView.backgroundColor = UIColor(hex: "#3D4043")
        
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

        recordButton = VoiceRecordButton(frame: .zero)//CGRect(x: 270, y: -3, width: 60, height: 60))
        
        
        sendButton = PSDMessageSendButton.init()
        sendButton.frame=CGRect(x: 0, y: 0, width: 44, height: 44)
        sendButton.delegate = self
        
        
        inputTextView = PSDMessageTextView(frame: CGRect(x: x, y: 0, width: sendButton.frame.origin.x - distTextToSend - x, height: defaultTextHeight))
        inputTextView.messageDelegate = self
       
        rateView = PSDRateView()
        rateView.delegate = self
    
        setupBottomView()
        self.addSubview(backgroundView)
        backgroundView.addSubview(topGrayLine)
        backgroundView.addSubview(attachmentsAddButton)
        backgroundView.addSubview(inputTextView)
        backgroundView.addSubview(sendButton)
        backgroundView.addSubview(attachmentsCollection)
        backgroundView.addSubview(rateView)
        backgroundView.addSubview(recordButton!)

        rateView.isHidden = !showRate
        
        addConstraints()
        
        self.recordingObject = AudioRecordingObject.init()
        self.recordingObject?.createWith(self)
    }
    
    func setupBottomView() {
        let bottomView = UIView()
        bottomView.backgroundColor = .psdBackgroundColor
        bottomView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bottomView)
        bottomView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        if #available(iOS 15.0, *) {
            bottomView.bottomAnchor.constraint(equalTo: keyboardLayoutGuide.bottomAnchor).isActive = true
        } else {
            bottomView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        }
        bottomView.widthAnchor.constraint(equalTo: widthAnchor).isActive = true
        bottomView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
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
    private var heightConstraint: NSLayoutConstraint?
    private var attachmentsHeightConstraint: NSLayoutConstraint?
    private var rateHeightConstraint: NSLayoutConstraint?

    func addConstraints() {
        guard let inputTextView = inputTextView, let sendButton = sendButton else { return }
        
        self.autoresizingMask = [.flexibleHeight, .flexibleWidth, .flexibleBottomMargin]
        addBackgroundViewConstraints()
        addTopGrayLineConstraints()
        
        // Устанавливаем constraints для rateView
        rateView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            rateView.leadingAnchor.constraint(equalTo: leadingAnchor),
            rateView.trailingAnchor.constraint(equalTo: trailingAnchor),
            rateView.topAnchor.constraint(equalTo: topAnchor)
        ])
        
        rateHeightConstraint = rateView.heightAnchor.constraint(equalToConstant: showRate ? PSDMessageInputView.RATE_HEIGHT : 0)
        rateHeightConstraint?.isActive = true
        
        // Устанавливаем constraints для attachmentsCollection
        attachmentsCollection.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            attachmentsCollection.leadingAnchor.constraint(equalTo: leadingAnchor),
            attachmentsCollection.trailingAnchor.constraint(equalTo: trailingAnchor),
            attachmentsCollection.topAnchor.constraint(equalTo: topGrayLine.bottomAnchor)
        ])
        
        attachmentsHeightConstraint = attachmentsCollection.heightAnchor.constraint(equalToConstant: 0)
        attachmentsHeightConstraint?.isActive = true
        
        // Устанавливаем constraints для остальных элементов
        inputTextView.translatesAutoresizingMaskIntoConstraints = false
        sendButton.translatesAutoresizingMaskIntoConstraints = false
        recordButton?.translatesAutoresizingMaskIntoConstraints = false
        attachmentsAddButton.translatesAutoresizingMaskIntoConstraints = false
        
        guard let recordButton else { return }
        
        NSLayoutConstraint.activate([
            // inputTextView
            inputTextView.topAnchor.constraint(equalTo: attachmentsCollection.bottomAnchor, constant: DEFAULT_LAYOUT_MARGINS),
            inputTextView.leadingAnchor.constraint(equalTo: recordButton.trailingAnchor, constant: distAddToText),
            inputTextView.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            inputTextView.heightAnchor.constraint(greaterThanOrEqualToConstant: defaultTextHeight),
            inputTextView.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: -60),
            // attachmentsAddButton
            attachmentsAddButton.leadingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.leadingAnchor),
            attachmentsAddButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            attachmentsAddButton.widthAnchor.constraint(equalToConstant: 44),
            attachmentsAddButton.heightAnchor.constraint(equalToConstant: 44),
            
            recordButton.leadingAnchor.constraint(equalTo: attachmentsAddButton.trailingAnchor),
            recordButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            recordButton.widthAnchor.constraint(equalToConstant: 44),
            recordButton.heightAnchor.constraint(equalToConstant: 44),
            
            
            // sendButton
           // sendButton.leadingAnchor.constraint(equalTo: inputTextView.trailingAnchor, constant: distTextToSend),
            sendButton.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: 0),
            sendButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor, constant: 1),
            sendButton.widthAnchor.constraint(equalToConstant: 60),
            sendButton.heightAnchor.constraint(equalToConstant: 44)
        ])
        
        // Отдельно сохраняем heightConstraint для inputTextView
        heightConstraint = inputTextView.heightAnchor.constraint(lessThanOrEqualToConstant: inputTextView.maxVerticalHeight())
        heightConstraint?.isActive = true
    }

    private func addBackgroundViewConstraints() {
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            backgroundView.topAnchor.constraint(equalTo: topAnchor),
            backgroundView.bottomAnchor.constraint(equalTo: bottomAnchor),
            backgroundView.leadingAnchor.constraint(equalTo: leadingAnchor),
            backgroundView.trailingAnchor.constraint(equalTo: trailingAnchor)
        ])
    }

    private func addTopGrayLineConstraints() {
        topGrayLine.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            topGrayLine.leadingAnchor.constraint(equalTo: leadingAnchor),
            topGrayLine.trailingAnchor.constraint(equalTo: trailingAnchor),
            topGrayLine.topAnchor.constraint(equalTo: rateView.bottomAnchor),
            topGrayLine.heightAnchor.constraint(equalToConstant: 0.5)
        ])
    }
}

extension PSDMessageInputView : AttachmentCollectionViewDelegateProtocol{
    func attachmentRemoved(){
        checkCollectionHeight()
        checkSendButton()
    }
}
extension PSDMessageInputView: AttachmentsAddButtonDelegate{
    func addButtonPressed() {
        delegate?.addButtonTapped()
//        self.becomeFirstResponder()
//        inputTextView.becomeFirstResponder()
    }
    
    func attachmentChoosed(_ data:Data, _ url:URL?)
    {
        let attachment = PSDObjectsCreator.createAttachment(data,url)
        addAttachment(attachment)
        if attachmentsPresenter.attachmentsNumber() == 1 {
            delegate?.addAttachment()
        }
    }
}
extension PSDMessageInputView: PSDRateViewDelegate {
    func didTapRate(_ rateValue: Int) {
        showRate = false
        delegate?.sendRate(rateValue)
    }
}

extension PSDMessageInputView: RecordableViewProtocol, AudioRecordingObjectDelegate {
    
    func didStartRecord() {
        
    }
    
    func didEndRecord() {
        
    }
    
    func didCreateFile(attachment: PSDAttachment, url: URL) {
        addAttachment(attachment)
    }
    
    func lastCellRect() -> CGRect {
        return CGRect.zero
    }
    
    func setSourseAndShowMicrophoneAlert(_ alert: UIAlertController) {
        
    }
    
    func getAccountId() -> NSInteger {
        return 0
    }
}
