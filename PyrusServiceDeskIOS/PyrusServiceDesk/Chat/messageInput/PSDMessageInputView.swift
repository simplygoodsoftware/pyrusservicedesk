import UIKit

protocol PSDMessageInputViewDelegate: AnyObject {
    func send(_ message: String, _ attachments: [PSDAttachment])
    func sendRate(_ rateValue: Int)
    func addButtonTapped()
    func addAttachment()
    func recordStart()
    func recordStop()
    func deletedAllAttachments()
    ///Контроллер, от имени которого панель показывает свои меню и алерты.
    func presenterForInputMenus() -> UIViewController?
}

let DEFAULT_LAYOUT_MARGINS: CGFloat = 8
let BUTTONS_CORNER_RADIUS: CGFloat = 8
class PSDMessageInputView: UIView, PSDMessageTextViewDelegate,PSDMessageSendButtonDelegate {
    
    static let RATE_HEIGHT : CGFloat = 92
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
    private var attachmentsCollection: AttachmentCollectionView!
    private var attachmentsPresenter: AttachmentCollectionViewPresenterProtocol!
    let distAddToText: CGFloat = 6
    let distTextToSend: CGFloat = 6
    let distToAdd: CGFloat = 21
    let distToSend: CGFloat = 15
    static let attachmentsHeight : CGFloat = 80
    
    var isRecording: Bool = false
    
//    private(set) var recordingObject: AudioRecordingObject?
//    var recordButton: VoiceRecordButton?
//    private var centerXConstraint: NSLayoutConstraint!
//    private var centerYConstraint: NSLayoutConstraint!
//    var voiceRecordView: VoiceRecordView?
//    var recordBuble: BubleTextView?
//    var lockRecordView: LockView?
//
//    private(set) lazy var addVoiceMessageButton: VoiceRecordButton = {
//        let button = VoiceRecordButton()
//        button.translatesAutoresizingMaskIntoConstraints = false
//        //        button.addTarget(self, action: #selector(buttonPressed(_:)), for: .touchUpInside)
//        return button
//    }()
//
//    private lazy var audioInputView: AudioInputView = {
//        let view = AudioInputView(frame: .zero)
//        view.translatesAutoresizingMaskIntoConstraints = false
//        view.alpha = 0
//        return view
//    }()
    
    private var audioLeadingConstraint: NSLayoutConstraint?
    
    //MARK: Liquid Glass
    
    ///Размеры и отступы стеклянной раскладки панели ввода.
    private enum GlassLayout {
        ///Диаметр круглых кнопок по бокам (вложения, микрофон).
        static let sideButtonSize: CGFloat = 44
        ///Минимальная высота капсулы ввода.
        static let capsuleMinHeight: CGFloat = 44
        ///Расстояние между кнопками и капсулой.
        static let itemSpacing: CGFloat = 8
        ///Отступ текста от левого края капсулы.
        static let textHorizontalInset: CGFloat = 14
        ///Отступ текста от верха и низа капсулы.
        static let textVerticalInset: CGFloat = 4
        ///Сторона кнопки отправки внутри капсулы — та же, что у боковых кнопок,
        ///чтобы глиф отправки был одного размера со скрепкой.
        static let sendButtonSize: CGFloat = sideButtonSize
        ///Отступ кнопки отправки от правого и нижнего края капсулы.
        ///Ноль: при минимальной высоте капсулы кнопка занимает её целиком.
        static let sendButtonInset: CGFloat = 0
        ///Расстояние от текста до кнопки отправки.
        static let textToSendSpacing: CGFloat = 6
        ///Плотность цветовой подмешки блюр-подложки панели.
        static let backdropDimAlpha: CGFloat = 0.45
    }
    
    ///Стеклянный круг под кнопкой вложений.
    private lazy var attachGlassBackground = PSDGlassBackgroundView(isInteractive: true)
    ///Стеклянная капсула поля ввода. Кнопка отправки живёт внутри неё.
    private lazy var inputGlassCapsule = PSDGlassBackgroundView()
    ///Стеклянный круг под кнопкой микрофона.
    private lazy var micGlassBackground = PSDGlassBackgroundView(isInteractive: true)
    
    ///Кнопка записи голосового сообщения. Видна, только когда включены голосовые
    ///и отправлять пока нечего, — см. `updateMicButtonVisibility`.
    private(set) lazy var micButton: UIButton = {
        let button = UIButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        if #available(iOS 13.0, *) {
            button.setImage(UIImage(systemName: "mic")?.withRenderingMode(.alwaysTemplate), for: .normal)
        }
        button.tintColor = PSDLiquidGlassStyle.iconColor
        button.addTarget(self, action: #selector(micButtonTapped), for: .touchUpInside)
        return button
    }()
    
    ///Блюр-подложка всей панели: затухает вверх, к переписке.
    private lazy var inputBackdropView = BlurBackdropView(fadeEdge: .top,
                                                          dimColor: Self.backdropDimColor)
    
    ///Два взаимоисключающих правых края капсулы: до микрофона либо до края панели.
    ///Когда микрофон скрыт, капсула дотягивается до правого края.
    private var capsuleTrailingToMic: NSLayoutConstraint?
    private var capsuleTrailingToEdge: NSLayoutConstraint?
    
    ///Текущее состояние микрофона. `nil` — раскладка ещё не применялась.
    private var isMicButtonVisible: Bool?
    
    ///Микрофон нужен, когда включены голосовые сообщения и отправлять пока нечего.
    private var shouldShowMicButton: Bool {
        PyrusServiceDesk.voiceMessages
            && inputTextView.text.count == 0
            && attachmentsPresenter.attachmentsNumber() == 0
    }
    
    ///Прозрачность кнопки отправки, когда отправлять нечего.
    ///В Liquid Glass кнопка исчезает; в прежнем оформлении остаётся бледной,
    ///если голосовые сообщения выключены, и исчезает, если включены.
    private var idleSendButtonAlpha: CGFloat {
        if PSDLiquidGlassStyle.isEnabled {
            return 0
        }
        return PyrusServiceDesk.voiceMessages ? 0 : 0.4
    }
    
    ///Цвет подмешки берётся из кастомизации фона, а не из темы системы.
    private static var backdropDimColor: UIColor {
        let base = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackgroundColor
        return base.withAlphaComponent(GlassLayout.backdropDimAlpha)
    }
    
    @objc private func micButtonTapped() {
        //Точка входа записи голосового сообщения. Пайплайн записи (VoiceRecordButton,
        //AudioRecordingObject) в текущей версии закомментирован — при его возвращении
        //запуск записи подключается сюда.
    }
    
    ///Показывает или прячет микрофон и перетягивает правый край капсулы.
    private func updateMicButtonVisibility(animated: Bool) {
        guard PSDLiquidGlassStyle.isEnabled else { return }
        let showMic = shouldShowMicButton
        guard showMic != isMicButtonVisible else { return }
        isMicButtonVisible = showMic
        
        //Сначала деактивация, потом активация — иначе конфликт констрейнтов.
        capsuleTrailingToMic?.isActive = false
        capsuleTrailingToEdge?.isActive = false
        (showMic ? capsuleTrailingToMic : capsuleTrailingToEdge)?.isActive = true
        
        let changes = {
            self.micGlassBackground.alpha = showMic ? 1 : 0
            self.micButton.alpha = showMic ? 1 : 0
            self.backgroundView.layoutIfNeeded()
        }
        guard animated else {
            changes()
            return
        }
        UIView.animate(withDuration: 0.2, animations: changes)
    }
    
    private lazy var deleteAudioButton: UIButton = {
        let button = UIButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        if let color = PyrusServiceDesk.mainController?.customization?.barButtonTintColor {
            button.setImage(UIImage.PSDImage(name: "deleteAudio")?.imageWith(color: color), for: .normal)
        } else {
            button.setImage(UIImage.PSDImage(name: "deleteAudio"), for: .normal)
        }
        button.alpha = 0
        button.addTarget(self, action: #selector(deleteAudioButtonTapped), for: .touchUpInside)
        return button
    }()
    
    private lazy var cancelButton: UIButton = {
        let button = UIButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setTitle("  " + "Cancel".localizedPSD(), for: .normal)
        button.setTitleColor(CustomizationHelper.recordImagesColors, for: .normal)
        button.setImage(UIImage.PSDImage(name: "arrowsLeft")?.imageWith(color: CustomizationHelper.recordImagesColors), for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        button.alpha = 0
        button.addTarget(self, action: #selector(cancel), for: .touchUpInside)
        return button
    }()
    
    var showRate = false {
        didSet {
            switch RatingType(rawValue: CustomizationHelper.ratingSettings.type) {
            case .text:
                let size = CGFloat(CustomizationHelper.ratingSettings.size)
                let rateItemsHeihgt = size * 40.0 + (size - 1) * 8.0
                rateHeight = 12 + 36 + rateItemsHeihgt
                textRateView.configure(with: CustomizationHelper.ratingSettings.ratingTextValues ?? [])
                rateHeightConstraint?.constant = showRate ? rateHeight : 0
                rateTopConstraint?.isActive = true
                textRateTopConstraint?.isActive = false
                textRateHeightConstraint?.constant = 0
                textRateView.isHidden = !showRate
                emojiRateView.isHidden = true
            default:
                rateHeight = 92
                emojiRateView.configure(with: RatingType(rawValue: CustomizationHelper.ratingSettings.type)?.rateArray(size: CustomizationHelper.ratingSettings.size).reversed() ?? [])
                textRateHeightConstraint?.constant = showRate ? rateHeight : 0
                rateTopConstraint?.isActive = false
                textRateTopConstraint?.isActive = true
                rateHeightConstraint?.constant = 0
                emojiRateView.isHidden = !showRate
                textRateView.isHidden = true
            }
        }
    }
    
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft")?.imageWith(color: CustomizationHelper.recordImagesColors), for: .normal)
        inputTextView.keyboardAppearance = CustomizationHelper.keyboardStyle
        if PSDLiquidGlassStyle.isEnabled {
            //Цвет иконки динамический и переключается сам; пересобирать надо только подложку.
            inputBackdropView.dimColor = Self.backdropDimColor
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
     //   self.backgroundColor = .psdBackground//UIColor.clear
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
        
//        recordButton = VoiceRecordButton(frame: .zero)//CGRect(x: 270, y: -3, width: 60, height: 60))
//        lockRecordView = LockView(frame: .zero)
        
        sendButton = PSDMessageSendButton.init()
        sendButton.frame = CGRect(x: 0, y: 0, width: 44, height: 44)
        sendButton.delegate = self
        
        inputTextView = PSDMessageTextView(frame: CGRect(x: x, y: 0, width: sendButton.frame.origin.x - distTextToSend - x, height: defaultTextHeight))
        inputTextView.messageDelegate = self
        
        textRateView = PSDTextRateView(frame: .zero)
        emojiRateView = PSDEmojiRateView(frame: .zero)
        textRateView.tapDelegate = self
        emojiRateView.tapDelegate = self
        
        setupBottomView()
        self.addSubview(backgroundView)
        backgroundView.addSubview(topGrayLine)
        backgroundView.addSubview(attachmentsAddButton)
        backgroundView.addSubview(inputTextView)
        backgroundView.addSubview(cancelButton)
        backgroundView.addSubview(sendButton)
        backgroundView.addSubview(attachmentsCollection)
        backgroundView.addSubview(textRateView)
        backgroundView.addSubview(emojiRateView)
        textRateView.addSubview(rateLabel)
        emojiRateView.addSubview(textRateLabel)
        textRateView.isHidden = !showRate
        emojiRateView.isHidden = !showRate
//        backgroundView.addSubview(lockRecordView!)
//        backgroundView.addSubview(recordButton!)
//        backgroundView.addSubview(audioInputView)
        backgroundView.addSubview(deleteAudioButton)
        backgroundView.clipsToBounds = false
        clipsToBounds = false
        
        if PSDLiquidGlassStyle.isEnabled {
            embedControlsIntoGlass()
        }
        addConstraints()
        
//        self.recordingObject = AudioRecordingObject.init()
//        self.recordingObject?.createWith(self)
        
        let stopView = UIImageView(image: UIImage.PSDImage(name: "stopRecord"))
        stopView.backgroundColor = .psdBackground
        stopView.layer.cornerRadius = 22
        inputTextView.keyboardAppearance = CustomizationHelper.keyboardStyle
    }
    
    func setupAudioInputView() {
//        backgroundView.addSubview(audioInputView)
//        audioInputView.translatesAutoresizingMaskIntoConstraints = false
//        audioLeadingConstraint = audioInputView.widthAnchor.constraint(equalToConstant: 0)
//        audioLeadingConstraint?.isActive = true
//        NSLayoutConstraint.activate([
//            audioInputView.trailingAnchor.constraint(equalTo: sendButton.leadingAnchor, constant: 0),
//            audioInputView.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor, constant: 0),
//        ])
    }
    
    func setupBottomView() {
        let bottomView = UIView()
        //В Liquid Glass фон панели рисует блюр-подложка, сплошная заливка не нужна.
        bottomView.backgroundColor = PSDLiquidGlassStyle.isEnabled ? .clear : .psdBackgroundColor
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
        
        guard PSDLiquidGlassStyle.isEnabled else { return }
        //Подложка занимает ту же область, что и прежняя заливка, — вплоть до клавиатуры,
        //и лежит ниже всего содержимого панели.
        insertSubview(inputBackdropView, at: 0)
        inputBackdropView.translatesAutoresizingMaskIntoConstraints = false
        inputBackdropView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        if #available(iOS 15.0, *) {
            inputBackdropView.bottomAnchor.constraint(equalTo: keyboardLayoutGuide.bottomAnchor).isActive = true
        } else {
            inputBackdropView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        }
        inputBackdropView.widthAnchor.constraint(equalTo: widthAnchor).isActive = true
        inputBackdropView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
    }
    
    ///In current version this function replase added to message attachments
    func addAttachment(_ attachment : PSDAttachment){
        attachmentsHeightConstraint?.constant = PSDMessageInputView.attachmentsHeight
        setNeedsLayout()
        attachmentsPresenter.addAttachment(attachment)
        checkSendButton()
    }
    
//    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
//        let view = super.hitTest(point, with: event)
//        return (view == self || view == lockRecordView) ? nil : view
//    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override var intrinsicContentSize: CGSize {
        return CGSize.zero
    }
    
    func setToDefault() {
        if inputTextView.text.count == 0 {
            UIView.animate(withDuration: 0.1, animations: {
                //Прежнее значение `false || voiceMessages` сохранено для старого оформления;
                //в Liquid Glass невидимая кнопка не должна ловить касания.
                self.sendButton.isUserInteractionEnabled = !PSDLiquidGlassStyle.isEnabled && PyrusServiceDesk.voiceMessages
                self.sendButton.alpha = self.idleSendButtonAlpha
//                self.recordButton?.alpha = PyrusServiceDesk.voiceMessages ? 1 : 0
            })
        } else{
            UIView.animate(withDuration: 0.1, animations: {
                self.sendButton.isUserInteractionEnabled = true
                self.sendButton.alpha = 1
//                self.recordButton?.alpha = 0
            })
        }
        updateMicButtonVisibility(animated: false)
    }
    
    func stopRecord() {
//        recordButton?.stopRecordButtonTapped()
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
//            self.audioInputView.alpha = 0
            self.deleteAudioButton.alpha = 0
            self.attachmentsAddButton.alpha = 1
            self.inputTextView.alpha = 1
            self.cancelButton.alpha = 0
        }, completion: {_ in
            self.audioLeadingConstraint?.constant = 0
//            OpusPlayer.shared.stopAllPlay()
//            self.audioInputView.state = .stopped
//            self.audioInputView.layoutIfNeeded()
//            self.audioInputView.setNeedsLayout()
//            self.audioInputView.state = .stopped
//            self.audioInputView.layoutIfNeeded()
//            self.audioInputView.setNeedsLayout()
            self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft")?.imageWith(color: CustomizationHelper.recordImagesColors), for: .normal)
        })
    }
    
    //MARK: Delegate methods
    func textViewChanged() {
        inputTextView.keyboardAppearance = CustomizationHelper.keyboardStyle
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
//                self.audioInputView.alpha = 0
                self.deleteAudioButton.alpha = 0
                self.attachmentsAddButton.alpha = 1
                self.inputTextView.alpha = 1
                self.cancelButton.alpha = 0
            }, completion: {_ in
                self.audioLeadingConstraint?.constant = 0
//                self.audioInputView.state = .stopped
//                self.audioInputView.layoutIfNeeded()
//                self.audioInputView.setNeedsLayout()
//                self.audioInputView.state = .stopped
//                self.audioInputView.layoutIfNeeded()
//                self.audioInputView.setNeedsLayout()
                self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft")?.imageWith(color: CustomizationHelper.recordImagesColors), for: .normal)
            })
        }
        checkSendButton()
    }
    
    func sendAudioMessage() {
        self.delegate?.send("", attachmentsPresenter.attachmentsForSend())
            attachmentsPresenter?.cleanAll()
            checkCollectionHeight()
            UIView.animate(withDuration: 0.1, animations: {
//                self.audioInputView.alpha = 0
                self.deleteAudioButton.alpha = 0
                self.attachmentsAddButton.alpha = 1
                self.inputTextView.alpha = 1
                self.cancelButton.alpha = 0
            }, completion: {_ in
                self.audioLeadingConstraint?.constant = 0
//                self.audioInputView.state = .stopped
//                self.audioInputView.layoutIfNeeded()
//                self.audioInputView.setNeedsLayout()
//                self.audioInputView.state = .stopped
//                self.audioInputView.layoutIfNeeded()
//                self.audioInputView.setNeedsLayout()
                self.cancelButton.setImage(UIImage.PSDImage(name: "arrowsLeft")?.imageWith(color: CustomizationHelper.recordImagesColors), for: .normal)
            })
        
//        OpusPlayer.shared.stopAllPlay()
        checkSendButton()
    }
    ///Check is send button need to enabled or not
    private func checkSendButton() {
        defer {
            updateMicButtonVisibility(animated: true)
        }
        let hasContent = self.inputTextView.text.count != 0 || self.attachmentsPresenter.attachmentsNumber() > 0
        if !PyrusServiceDesk.voiceMessages && !PSDLiquidGlassStyle.isEnabled {
            self.sendButton.isUserInteractionEnabled = hasContent
            self.sendButton.alpha = !hasContent ? 0.4 : 1
//            self.recordButton?.alpha = 0
            return
        }
        if PSDLiquidGlassStyle.isEnabled {
            //Исчезнувшая кнопка не должна оставаться кликабельной.
            self.sendButton.isUserInteractionEnabled = hasContent
        }
        UIView.animate(withDuration: 0.1, animations: {
            self.sendButton.alpha = hasContent ? 1 : self.idleSendButtonAlpha
//            self.recordButton?.alpha = (self.inputTextView.text.count != 0 || self.attachmentsPresenter.attachmentsNumber() > 0) ? 0 : 1
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
    private var textRateHeightConstraint: NSLayoutConstraint?
    private var rateTopConstraint: NSLayoutConstraint?
    private var textRateTopConstraint: NSLayoutConstraint?

    
    func addConstraints() {
        guard let inputTextView = inputTextView, let sendButton = sendButton else { return }
        
        self.autoresizingMask = [.flexibleHeight, .flexibleWidth, .flexibleBottomMargin]
        addBackgroundViewConstraints()
        addTopGrayLineConstraints()
        
        // Устанавливаем constraints для rateView
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
//        recordButton?.translatesAutoresizingMaskIntoConstraints = false
//        lockRecordView?.translatesAutoresizingMaskIntoConstraints = false
        attachmentsAddButton.translatesAutoresizingMaskIntoConstraints = false
        
//        recordButton?.isUserInteractionEnabled = true
//        guard let recordButton, let lockRecordView else { return }
        
//        audioLeadingConstraint = audioInputView.widthAnchor.constraint(equalToConstant: 0)
//        audioLeadingConstraint?.isActive = true
        if PSDLiquidGlassStyle.isEnabled {
            addLiquidGlassControlConstraints()
        } else {
            addLegacyControlConstraints()
        }
        
        NSLayoutConstraint.activate([
            textRateLabel.topAnchor.constraint(equalTo: emojiRateView.topAnchor, constant: 12),
            textRateLabel.centerXAnchor.constraint(equalTo: emojiRateView.centerXAnchor),
            
            rateLabel.topAnchor.constraint(equalTo: textRateView.topAnchor, constant: 12),
            rateLabel.centerXAnchor.constraint(equalTo: textRateView.centerXAnchor),
            
            emojiRateView.bottomAnchor.constraint(equalTo: textRateView.topAnchor),
            
        ])
        superview?.topAnchor.constraint(equalTo: emojiRateView.topAnchor).isActive = true
    }
    
    ///Прежняя раскладка: кнопка отправки справа от поля ввода, без стеклянных подложек.
    private func addLegacyControlConstraints() {
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
            
//            recordButton.centerXAnchor.constraint(equalTo: sendButton.centerXAnchor),
//            recordButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor),
//            lockRecordView.centerXAnchor.constraint(equalTo: recordButton.centerXAnchor),
//            lockRecordView.bottomAnchor.constraint(equalTo: recordButton.topAnchor, constant: -10),
            
            cancelButton.centerXAnchor.constraint(equalTo: centerXAnchor),
            cancelButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor),
            
            sendButton.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor, constant: 5),
            sendButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor, constant: 1),
            sendButton.widthAnchor.constraint(equalToConstant: 60),
            sendButton.heightAnchor.constraint(equalToConstant: 44),
            
//            audioInputView.trailingAnchor.constraint(equalTo: sendButton.leadingAnchor, constant: 0),
//            audioInputView.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor, constant: 0),
        ])
        
//        centerXConstraint = recordButton.centerXAnchor.constraint(equalTo: sendButton.centerXAnchor)
//        centerYConstraint = recordButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor)
//        NSLayoutConstraint.activate([centerXConstraint, centerYConstraint])
//
        // Отдельно сохраняем heightConstraint для inputTextView
        heightConstraint = inputTextView.heightAnchor.constraint(lessThanOrEqualToConstant: inputTextView.maxVerticalHeight())
        heightConstraint?.isActive = true
    }
    
    ///Раскладка Liquid Glass: кнопка вложений и микрофон на стеклянных кругах,
    ///поле ввода в стеклянной капсуле, кнопка отправки — внутри капсулы.
    ///Правый край капсулы двойной (до микрофона / до края) — см. `updateMicButtonVisibility`.
    private func addLiquidGlassControlConstraints() {
        [attachGlassBackground, inputGlassCapsule, micGlassBackground].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
        }
        let capsuleContent = inputGlassCapsule.glassContentView
        
        NSLayoutConstraint.activate([
            attachGlassBackground.leadingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.leadingAnchor),
            attachGlassBackground.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            attachGlassBackground.widthAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            attachGlassBackground.heightAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            
            attachmentsAddButton.centerXAnchor.constraint(equalTo: attachGlassBackground.glassContentView.centerXAnchor),
            attachmentsAddButton.centerYAnchor.constraint(equalTo: attachGlassBackground.glassContentView.centerYAnchor),
            attachmentsAddButton.widthAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            attachmentsAddButton.heightAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            
            inputGlassCapsule.leadingAnchor.constraint(equalTo: attachGlassBackground.trailingAnchor, constant: GlassLayout.itemSpacing),
            inputGlassCapsule.topAnchor.constraint(equalTo: attachmentsCollection.bottomAnchor, constant: DEFAULT_LAYOUT_MARGINS),
            inputGlassCapsule.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            inputGlassCapsule.heightAnchor.constraint(greaterThanOrEqualToConstant: GlassLayout.capsuleMinHeight),
            
            inputTextView.leadingAnchor.constraint(equalTo: capsuleContent.leadingAnchor, constant: GlassLayout.textHorizontalInset),
            inputTextView.topAnchor.constraint(equalTo: capsuleContent.topAnchor, constant: GlassLayout.textVerticalInset),
            inputTextView.bottomAnchor.constraint(equalTo: capsuleContent.bottomAnchor, constant: -GlassLayout.textVerticalInset),
            inputTextView.trailingAnchor.constraint(equalTo: sendButton.leadingAnchor, constant: -GlassLayout.textToSendSpacing),
            inputTextView.heightAnchor.constraint(greaterThanOrEqualToConstant: defaultTextHeight),
            
            //Кнопка отправки — внутри капсулы, у правого нижнего угла:
            //при многострочном тексте капсула растёт вверх, кнопка остаётся у нижней кромки.
            sendButton.trailingAnchor.constraint(equalTo: capsuleContent.trailingAnchor, constant: -GlassLayout.sendButtonInset),
            sendButton.bottomAnchor.constraint(equalTo: capsuleContent.bottomAnchor, constant: -GlassLayout.sendButtonInset),
            sendButton.widthAnchor.constraint(equalToConstant: GlassLayout.sendButtonSize),
            sendButton.heightAnchor.constraint(equalToConstant: GlassLayout.sendButtonSize),
            
            micGlassBackground.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor),
            micGlassBackground.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            micGlassBackground.widthAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            micGlassBackground.heightAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            
            micButton.centerXAnchor.constraint(equalTo: micGlassBackground.glassContentView.centerXAnchor),
            micButton.centerYAnchor.constraint(equalTo: micGlassBackground.glassContentView.centerYAnchor),
            micButton.widthAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            micButton.heightAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            
            deleteAudioButton.leadingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.leadingAnchor),
            deleteAudioButton.bottomAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.bottomAnchor),
            deleteAudioButton.widthAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            deleteAudioButton.heightAnchor.constraint(equalToConstant: GlassLayout.sideButtonSize),
            
            cancelButton.centerXAnchor.constraint(equalTo: centerXAnchor),
            cancelButton.centerYAnchor.constraint(equalTo: sendButton.centerYAnchor)
        ])
        
        capsuleTrailingToMic = inputGlassCapsule.trailingAnchor.constraint(equalTo: micGlassBackground.leadingAnchor,
                                                                           constant: -GlassLayout.itemSpacing)
        capsuleTrailingToEdge = inputGlassCapsule.trailingAnchor.constraint(equalTo: backgroundView.layoutMarginsGuide.trailingAnchor)
        
        heightConstraint = inputTextView.heightAnchor.constraint(lessThanOrEqualToConstant: inputTextView.maxVerticalHeight())
        heightConstraint?.isActive = true
        
        updateMicButtonVisibility(animated: false)
    }
    
    ///Перекладывает элементы на стеклянные подложки. Иерархия меняется до раскладки:
    ///кнопки получают стеклянные круги, поле ввода и кнопка отправки переезжают
    ///внутрь стеклянной капсулы. Разделитель в стекле не нужен — его роль играет блюр.
    private func embedControlsIntoGlass() {
        backgroundView.addSubview(attachGlassBackground)
        backgroundView.insertSubview(inputGlassCapsule, belowSubview: cancelButton)
        backgroundView.addSubview(micGlassBackground)
        
        //Кнопки живут внутри своих подложек, а не поверх них: интерактивное стекло
        //должно реагировать на касания того элемента, который оно подсвечивает.
        attachGlassBackground.glassContentView.addSubview(attachmentsAddButton)
        inputGlassCapsule.glassContentView.addSubview(inputTextView)
        inputGlassCapsule.glassContentView.addSubview(sendButton)
        micGlassBackground.glassContentView.addSubview(micButton)
        
        //Внутри капсулы кнопка отправки квадратная — отступы широкого варианта ей не подходят.
        sendButton.applyLiquidGlassStyle()
        
        topGrayLine.isHidden = true
    }
    
    @objc private func deleteAudioButtonTapped() {
        clearAll(clearInput: false)
    }
    
    @objc private func cancel() {
//        recordButton?.cancelRecording()
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
            topGrayLine.topAnchor.constraint(equalTo: textRateView.bottomAnchor),
            topGrayLine.heightAnchor.constraint(equalToConstant: 0.5)
        ])
    }
}

extension PSDMessageInputView: AttachmentCollectionViewDelegateProtocol {
    func attachmentRemoved(){
        checkCollectionHeight()
        checkSendButton()
        if attachmentsPresenter.attachmentsNumber() == 0 {
            delegate?.deletedAllAttachments()
        }
    }
}
extension PSDMessageInputView: AttachmentsAddButtonDelegate{
    func addButtonPressed() {
        inputTextView.keyboardAppearance = CustomizationHelper.keyboardStyle
        delegate?.addButtonTapped()
        //        self.becomeFirstResponder()
        //        inputTextView.becomeFirstResponder()
    }
    
    func attachmentMenuPresenter() -> UIViewController? {
        return delegate?.presenterForInputMenus()
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
        delegate?.sendRate(rateValue)
        showRate = false
    }
}

//extension PSDMessageInputView: RecordableViewProtocol, AudioRecordingObjectDelegate {
//    func lockRecord() {
//        UIView.animate(withDuration: 0.2, animations: {
//            self.cancelButton.setImage(nil, for: .normal)
//        })
//    }
//
//
//    func cancelRecording() {
//        isRecording = false
//        delegate?.recordStop()
//        clearAll(clearInput: false)
//    }
//
//    func didStartRecord() {
//        isRecording = true
//        OpusPlayer.shared.stopAllPlay()
//        delegate?.recordStart()
//        UIView.animate(withDuration: 0.2, animations: {
//            self.attachmentsAddButton.alpha = 0
//            self.inputTextView.alpha = 0
//            self.cancelButton.alpha = 1
//        })
//    }
//
//    func didEndRecord() {
//        isRecording = false
//        delegate?.recordStop()
//        UIView.animate(withDuration: 0.2, animations: {
//            self.attachmentsAddButton.alpha = 1
//            self.inputTextView.alpha = 1
//            self.cancelButton.alpha = 0
//        })
//    }
//
//    func sendAudio(attachment: PSDAttachment) {
//        attachmentsPresenter.addAttachment(attachment)
//        sendMessage()
//        delegate?.recordStop()
//    }
//
//    func didCreateFile(attachment: PSDAttachment, url: URL) {
//        inputTextView.resignFirstResponder()
//        attachmentsPresenter.addAttachment(attachment)
//        checkSendButton()
//        audioInputView.removeFromSuperview()
//        audioInputView = AudioInputView()
//        audioInputView.alpha = 0
//        setupAudioInputView()
//
//        let audioPlayerPresenter = AudioPlayerPresenter(view: audioInputView, fileUrl: URL(fileURLWithPath: attachment.localPath ?? ""), attachmentId: attachment.localId, attachment: attachment)
//        audioInputView.presenter = audioPlayerPresenter
//
//        UIView.animate(withDuration: 0.2, animations: {
//            self.audioInputView.alpha = 1
//            self.deleteAudioButton.alpha = 1
//            self.attachmentsAddButton.alpha = 0
//            self.inputTextView.alpha = 0
//            self.cancelButton.alpha = 0
//            self.audioLeadingConstraint?.constant = self.inputTextView.frame.width + 4//self.frame.width - 100
//            self.setNeedsLayout()
//            self.layoutIfNeeded()
//        })
//    }
//
//    func lastCellRect() -> CGRect {
//        return CGRect.zero
//    }
//
//    func setSourseAndShowMicrophoneAlert(_ alert: UIAlertController) { }
//
//    func getAccountId() -> NSInteger {
//        return 0
//    }
//}
