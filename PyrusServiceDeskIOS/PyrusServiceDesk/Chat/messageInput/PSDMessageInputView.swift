
import UIKit

protocol PSDMessageInputViewDelegate: class {
    func send(_ message: String, _ attachments: [PSDAttachment])
    func sendRate(_ rateValue: Int)
    func addButtonTapped()
    func addAttachment()
    func recordStart()
    func recordStop()
}
let DEFAULT_LAYOUT_MARGINS: CGFloat = 8
let BUTTONS_CORNER_RADIUS: CGFloat = 8
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
    private var attachmentsCollection: AttachmentCollectionView!
    private var attachmentsPresenter: AttachmentCollectionViewPresenterProtocol!
    let distAddToText: CGFloat = 6
    let distTextToSend: CGFloat = 6
    let distToAdd: CGFloat = 21
    let distToSend: CGFloat = 15
    static let attachmentsHeight : CGFloat = 80
    
    var isRecording: Bool = false
    
    private(set) var recordingObject: AudioRecordingObject?
    var recordButton: VoiceRecordButton?
    private var centerXConstraint: NSLayoutConstraint!
    private var centerYConstraint: NSLayoutConstraint!
    var voiceRecordView: VoiceRecordView?
    var recordBuble: BubleTextView?
    var lockRecordView: LockView?
    
    private(set) lazy var addVoiceMessageButton: VoiceRecordButton = {
        let button = VoiceRecordButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        //        button.addTarget(self, action: #selector(buttonPressed(_:)), for: .touchUpInside)
        return button
    }()
    
    private lazy var audioInputView: AudioInputView = {
        let view = AudioInputView(frame: .zero)
        view.translatesAutoresizingMaskIntoConstraints = false
        view.alpha = 0
        return view
    }()
    
    private var audioLeadingConstraint: NSLayoutConstraint?
    
    private lazy var deleteAudioButton: UIButton = {
        let button = UIButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setImage(UIImage.PSDImage(name: "deleteAudio"), for: .normal)
        button.alpha = 0
        button.addTarget(self, action: #selector(deleteAudioButtonTapped), for: .touchUpInside)
        return button
    }()
    
    private lazy var cancelButton: UIButton = {
        let button = UIButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setTitle("  " + "Cancel".localizedPSD(), for: .normal)
        button.setTitleColor(PyrusServiceDesk.mainController?.customization?.themeColor, for: .normal)
        button.setImage(UIImage.PSDImage(name: "arrowsLeft"), for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        button.alpha = 0
        button.addTarget(self, action: #selector(cancel), for: .touchUpInside)
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
        lockRecordView = LockView(frame: .zero)
        
        sendButton = PSDMessageSendButton.init()
        sendButton.frame = CGRect(x: 0, y: 0, width: 44, height: 44)
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
        backgroundView.addSubview(cancelButton)
        backgroundView.addSubview(sendButton)
        backgroundView.addSubview(attachmentsCollection)
        backgroundView.addSubview(rateView)
        backgroundView.addSubview(lockRecordView!)
        backgroundView.addSubview(recordButton!)
        backgroundView.addSubview(audioInputView)
        backgroundView.addSubview(deleteAudioButton)
        backgroundView.clipsToBounds = false
        clipsToBounds = false
        
        rateView.isHidden = !showRate
        
        addConstraints()
        
        self.recordingObject = AudioRecordingObject.init()
        self.recordingObject?.createWith(self)
        
        let stopView = UIImageView(image: UIImage.PSDImage(name: "stopRecord"))
        stopView.backgroundColor = .psdBackground
        stopView.layer.cornerRadius = 22
    }
    
    func setupAudioInputView() {
        backgroundView.addSubview(audioInputView)
        audioInputView.translatesAutoresizingMaskIntoConstraints = false
        audioLeadingConstraint = audioInputView.widthAnchor.constraint(equalToConstant: 0)
        audioLeadingConstraint?.isActive = true
        NSLayoutConstraint.activate([
            audioInputView.trailingAnchor.constraint(equalTo: sendButton.leadingAnchor, constant: 0),
            audioInputView.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor, constant: 0),
        ])
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
        return (view == self || view == lockRecordView) ? nil : view
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override var intrinsicContentSize: CGSize {
        return CGSize.zero
    }
    
    func setToDefault() {
        if inputTextView.text.count == 0 {
            UIView.animate(withDuration: 0.1, animations: {
                self.sendButton.alpha = 0
                self.recordButton?.alpha = 1
            })
        } else{
            UIView.animate(withDuration: 0.1, animations: {
                self.sendButton.alpha = 1
                self.recordButton?.alpha = 0
            })
        }
    }
    
    func stopRecord() {
        recordButton?.stopRecordButtonTapped()
    }
    
    ///Clear text.
    func clearAll(clearInput: Bool = true) {
        if clearInput {
            inputTextView.text = ""
        }
        inputTextView.textViewDidChange(inputTextView)
        attachmentsPresenter?.cleanAll()
        checkCollectionHeight()
        checkSendButton()
        
        UIView.animate(withDuration: 0.1, animations: {
            self.audioInputView.alpha = 0
            self.deleteAudioButton.alpha = 0
            self.attachmentsAddButton.alpha = 1
            self.inputTextView.alpha = 1
            self.cancelButton.alpha = 0
        }, completion: {_ in
            self.audioLeadingConstraint?.constant = 0
            OpusPlayer.shared.stopAllPlay()
            self.audioInputView.state = .stopped
            self.audioInputView.layoutIfNeeded()
            self.audioInputView.setNeedsLayout()
            self.audioInputView.state = .stopped
            self.audioInputView.layoutIfNeeded()
            self.audioInputView.setNeedsLayout()
            self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft"), for: .normal)
        })
    }
    
    //MARK: Delegate methods
    func textViewChanged() {
        if !isRecording {
            checkSendButton()
        }
    }
    
    func sendMessage() {
        if attachmentsPresenter.attachmentsNumber() == 1 && attachmentsPresenter.attachmentsForSend().first?.isAudio ?? false {
            sendAudioMessage()
            return
        }
        inputTextView.text = inputTextView.text.trimmingCharacters(in: .whitespacesAndNewlines)
        inputTextView.textViewDidChange(inputTextView)
        if(inputTextView.text.count > 0 || attachmentsPresenter.attachmentsNumber() > 0) {
            self.delegate?.send(inputTextView.text, attachmentsPresenter.attachmentsForSend())
            inputTextView.text = ""
            inputTextView.textViewDidChange(inputTextView)
            attachmentsPresenter?.cleanAll()
            checkCollectionHeight()
            UIView.animate(withDuration: 0.1, animations: {
                self.audioInputView.alpha = 0
                self.deleteAudioButton.alpha = 0
                self.attachmentsAddButton.alpha = 1
                self.inputTextView.alpha = 1
                self.cancelButton.alpha = 0
            }, completion: {_ in
                self.audioLeadingConstraint?.constant = 0
                self.audioInputView.state = .stopped
                self.audioInputView.layoutIfNeeded()
                self.audioInputView.setNeedsLayout()
                self.audioInputView.state = .stopped
                self.audioInputView.layoutIfNeeded()
                self.audioInputView.setNeedsLayout()
                self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft"), for: .normal)
            })
        }
        checkSendButton()
    }
    
    func sendAudioMessage() {
        self.delegate?.send("", attachmentsPresenter.attachmentsForSend())
            attachmentsPresenter?.cleanAll()
            checkCollectionHeight()
            UIView.animate(withDuration: 0.1, animations: {
                self.audioInputView.alpha = 0
                self.deleteAudioButton.alpha = 0
                self.attachmentsAddButton.alpha = 1
                self.inputTextView.alpha = 1
                self.cancelButton.alpha = 0
            }, completion: {_ in
                self.audioLeadingConstraint?.constant = 0
                self.audioInputView.state = .stopped
                self.audioInputView.layoutIfNeeded()
                self.audioInputView.setNeedsLayout()
                self.audioInputView.state = .stopped
                self.audioInputView.layoutIfNeeded()
                self.audioInputView.setNeedsLayout()
                self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft"), for: .normal)
            })
        
        OpusPlayer.shared.stopAllPlay()
        checkSendButton()
    }
    ///Check is send button need to enabled or not
    private func checkSendButton() {
        UIView.animate(withDuration: 0.1, animations: {
            self.sendButton.alpha = !(self.inputTextView.text.count != 0 || self.attachmentsPresenter.attachmentsNumber() > 0) ? 0 : 1
            self.recordButton?.alpha = (self.inputTextView.text.count != 0 || self.attachmentsPresenter.attachmentsNumber() > 0) ? 0 : 1
        })
    }
    
    private func checkCollectionHeight() {
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
        lockRecordView?.translatesAutoresizingMaskIntoConstraints = false
        attachmentsAddButton.translatesAutoresizingMaskIntoConstraints = false
        
        recordButton?.isUserInteractionEnabled = true
        guard let recordButton, let lockRecordView else { return }
        
//        audioLeadingConstraint = audioInputView.leadingAnchor.constraint(equalTo: attachmentsAddButton.trailingAnchor, constant: frame.width - 94)
        audioLeadingConstraint = audioInputView.widthAnchor.constraint(equalToConstant: 0)
        audioLeadingConstraint?.isActive = true
        NSLayoutConstraint.activate([
            // inputTextView
            inputTextView.topAnchor.constraint(equalTo: attachmentsCollection.bottomAnchor, constant: DEFAULT_LAYOUT_MARGINS),
            inputTextView.leadingAnchor.constraint(equalTo: attachmentsAddButton.trailingAnchor, constant: distAddToText),
            inputTextView.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            inputTextView.heightAnchor.constraint(greaterThanOrEqualToConstant: defaultTextHeight),
            inputTextView.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: -60),
            // attachmentsAddButton
            attachmentsAddButton.leadingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.leadingAnchor),
            attachmentsAddButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            attachmentsAddButton.widthAnchor.constraint(equalToConstant: 44),
            attachmentsAddButton.heightAnchor.constraint(equalToConstant: 44),
            
            deleteAudioButton.leadingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.leadingAnchor),
            deleteAudioButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            deleteAudioButton.widthAnchor.constraint(equalToConstant: 44),
            deleteAudioButton.heightAnchor.constraint(equalToConstant: 44),
            
            //recordButton.leadingAnchor.constraint(equalTo: inputTextView.trailingAnchor),
//            recordButton.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: 0),
            recordButton.centerXAnchor.constraint(equalTo: sendButton.centerXAnchor),
            recordButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor),
            lockRecordView.centerXAnchor.constraint(equalTo: recordButton.centerXAnchor),
            lockRecordView.bottomAnchor.constraint(equalTo: recordButton.topAnchor, constant: -10),
            
            cancelButton.centerXAnchor.constraint(equalTo: centerXAnchor),
            cancelButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor),
            
//            recordButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
//            recordButton.widthAnchor.constraint(equalToConstant: 44),
//            recordButton.heightAnchor.constraint(equalToConstant: 44),
            
            // sendButton
            // sendButton.leadingAnchor.constraint(equalTo: inputTextView.trailingAnchor, constant: distTextToSend),
            sendButton.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: 5),
            sendButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor, constant: 1),
            sendButton.widthAnchor.constraint(equalToConstant: 60),
            sendButton.heightAnchor.constraint(equalToConstant: 44),
            
           // audioInputView.leadingAnchor.constraint(equalTo: attachmentsAddButton.trailingAnchor, constant: 6),
            audioInputView.trailingAnchor.constraint(equalTo: sendButton.leadingAnchor, constant: 0),
            audioInputView.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor, constant: 0),
        ])
        
        centerXConstraint = recordButton.centerXAnchor.constraint(equalTo: sendButton.centerXAnchor)
        centerYConstraint = recordButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor)
        NSLayoutConstraint.activate([centerXConstraint, centerYConstraint])
        //recordButton.addTarget(self, action: #selector(handlePanGesture(_:)), for: .touchDragInside)
        
        // Отдельно сохраняем heightConstraint для inputTextView
        heightConstraint = inputTextView.heightAnchor.constraint(lessThanOrEqualToConstant: inputTextView.maxVerticalHeight())
        heightConstraint?.isActive = true
    }
    
    @objc private func deleteAudioButtonTapped() {
        clearAll(clearInput: false)
    }
    
    @objc private func cancel() {
        recordButton?.cancelRecording()
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

extension PSDMessageInputView: AttachmentCollectionViewDelegateProtocol {
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
    func lockRecord() {
        UIView.animate(withDuration: 0.2, animations: {
            self.cancelButton.setImage(nil, for: .normal)
        })
    }
    
    
    func cancelRecording() {
        isRecording = false
        delegate?.recordStop()
        clearAll(clearInput: false)
    }
    
    func didStartRecord() {
        isRecording = true
        OpusPlayer.shared.stopAllPlay()
        delegate?.recordStart()
        UIView.animate(withDuration: 0.2, animations: {
            self.attachmentsAddButton.alpha = 0
            self.inputTextView.alpha = 0
            self.cancelButton.alpha = 1
        })
    }
    
    func didEndRecord() {
        isRecording = false
        delegate?.recordStop()
        UIView.animate(withDuration: 0.2, animations: {
            self.attachmentsAddButton.alpha = 1
            self.inputTextView.alpha = 1
            self.cancelButton.alpha = 0
        })
    }
    
    func sendAudio(attachment: PSDAttachment) {
        attachmentsPresenter.addAttachment(attachment)
        sendMessage()
        delegate?.recordStop()
    }
    
    func didCreateFile(attachment: PSDAttachment, url: URL) {
        inputTextView.resignFirstResponder()
        attachmentsPresenter.addAttachment(attachment)
        checkSendButton()
        audioInputView.removeFromSuperview()
        audioInputView = AudioInputView()
        audioInputView.alpha = 0
        setupAudioInputView()
        
        let audioPlayerPresenter = AudioPlayerPresenter(view: audioInputView, fileUrl: URL(fileURLWithPath: attachment.localPath ?? ""), attachmentId: attachment.localId, attachment: attachment)
        audioInputView.presenter = audioPlayerPresenter
        
        UIView.animate(withDuration: 0.2, animations: {
            self.audioInputView.alpha = 1
            self.deleteAudioButton.alpha = 1
            self.attachmentsAddButton.alpha = 0
            self.inputTextView.alpha = 0
            self.cancelButton.alpha = 0
            self.audioLeadingConstraint?.constant = self.inputTextView.frame.width + 4//self.frame.width - 100
            self.setNeedsLayout()
            self.layoutIfNeeded()
        })
    }
    
    func lastCellRect() -> CGRect {
        return CGRect.zero
    }
    
    func setSourseAndShowMicrophoneAlert(_ alert: UIAlertController) { }
    
    func getAccountId() -> NSInteger {
        return 0
    }
}
