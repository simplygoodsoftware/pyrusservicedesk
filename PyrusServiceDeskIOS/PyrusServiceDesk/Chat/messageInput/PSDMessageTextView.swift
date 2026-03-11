
import UIKit

let PLACEHOLDER_ALPHA: CGFloat = 0.2
/// Новый префикс для ключей черновиков по ticketId
private let PSD_MESSAGE_DRAFT_KEY_PREFIX: String = "PSDMessageDraft_ticket_"
/// Старый общий ключ (для миграции)
private let PSD_MESSAGE_DRAFT_LEGACY_KEY: String = "PSDMessageDraft"

protocol PSDMessageTextViewDelegate: AnyObject {
    func textViewChanged()
}

class PSDMessageTextView: UITextView, UITextViewDelegate {
    
    weak var messageDelegate: PSDMessageTextViewDelegate?
    
    /// Текущий идентификатор тикета, по которому сохраняется/получается черновик.
    var ticketId: Int? {
        didSet {
            if let oldValue, ticketId ?? 0 != 0 {
                clearDraft(for: oldValue)
            }
        }
    }
    
    // MARK: - Init
    
    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        commonInit()
    }
    
    /// Удобный инициализатор с установкой ticketId.
    convenience init(frame: CGRect, textContainer: NSTextContainer?, ticketId: Int?) {
        self.init(frame: frame, textContainer: textContainer)
        self.ticketId = ticketId
        // Подгружаем черновик для переданного ticketId
        self.text = self.getDraft(for: ticketId)
        self.textViewDidChange(self)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit()
    }
    
    private func commonInit() {
        self.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        backgroundColor = .clear
        font = .textFont
        tintColor = CustomizationHelper.recordImagesColors
        textColor = .psdLabel
        self.delegate = self
        addPlaceholder()
        self.textContainer.lineFragmentPadding = 0.0
        
        // Если ticketId не задан, подгрузим общий/последний черновик (миграционный сценарий).
        if self.ticketId == nil {
            self.text = self.getDraft(for: nil)
        }
        self.textViewDidChange(self)
        
        self.keyboardAppearance = CustomizationHelper.keyboardStyle
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(saveDraft),
                                               name: UIApplication.willResignActiveNotification,
                                               object: nil)
    }
    
    // MARK: - Overrides
    
    override func setContentOffset(_ contentOffset: CGPoint, animated: Bool) {
        super.setContentOffset(contentOffset, animated: false)
    }
    
    override var textColor: UIColor? {
        didSet {
            placeholder.textColor = textColor
        }
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        saveDraft()
    }
    
    override var intrinsicContentSize: CGSize {
        return CGSize(width: 0, height: self.contentSize.height)
    }
    
    // MARK: - Placeholder
    
    private let placeholder: UILabel = {
        let label = UILabel()
        label.text = "Comment".localizedPSD()
        label.alpha = PLACEHOLDER_ALPHA
        label.textAlignment = .left
        return label
    }()
    
    private func addPlaceholder() {
        placeholder.isHidden = self.text.count > 0
        placeholder.font = self.font
        
        var rect = self.bounds
        rect.origin.x = 0
        rect.size.width = rect.size.width - (rect.origin.x * 2)
        placeholder.frame = rect
        
        placeholder.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        
        self.addSubview(placeholder)
    }
    
    private func defineNeedPlaceholder() {
        placeholder.isHidden = self.text.count > 0
    }
    
    // MARK: - Sizing helpers
    
    func maxVerticalHeight() -> CGFloat {
        return (self.font?.lineHeight ?? 0) * 3.5
    }
    
    func maxHorizontalHeight() -> CGFloat {
        return (self.font?.lineHeight ?? 0) * 2.5
    }
    
    // MARK: - Draft persistence
    
    /// Сформировать ключ для сохранения черновика по ticketId.
    private func draftKey(for ticketId: Int?) -> String {
        guard let id = ticketId, id != 0 else {
            return PSD_MESSAGE_DRAFT_LEGACY_KEY
        }
        return PSD_MESSAGE_DRAFT_KEY_PREFIX + "\(id)"
    }
    
    /// Сохранить текущий текст как черновик для текущего ticketId.
    @objc func saveDraft() {
        guard let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults() else { return }
        let key = draftKey(for: self.ticketId)
        pyrusUserDefaults.set(self.text, forKey: key)
        pyrusUserDefaults.synchronize()
    }
    
    func updateDraft() {
        self.text = self.getDraft(for: ticketId)
        self.textViewDidChange(self)
    }
    
    /// Получить сохранённый черновик для указанного ticketId.
    /// Если ticketId = nil или черновик для ключа отсутствует — пытаемся прочитать по старому общему ключу.
    private func getDraft(for ticketId: Int?) -> String {
        guard let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults() else { return "" }
        let key = draftKey(for: ticketId)
        if let value = pyrusUserDefaults.value(forKey: key) as? String {
            return value
        }
        // Fallback: если ключ с ticketId не найден, пробуем достать из общего ключа для миграции
        if key != PSD_MESSAGE_DRAFT_LEGACY_KEY,
           let legacyValue = pyrusUserDefaults.value(forKey: PSD_MESSAGE_DRAFT_LEGACY_KEY) as? String {
            return legacyValue
        }
        return ""
    }
    
    /// Очистить черновик для указанного ticketId (опционально, если нужно).
    func clearDraft(for ticketId: Int?) {
        guard let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults() else { return }
        let key = draftKey(for: ticketId)
        pyrusUserDefaults.removeObject(forKey: key)
        pyrusUserDefaults.synchronize()
    }
    
    // MARK: - UITextViewDelegate
    
    func textViewDidChange(_ textView: UITextView) {
        defineNeedPlaceholder()
        self.messageDelegate?.textViewChanged()
        self.invalidateIntrinsicContentSize()
    }
    
    func textViewDidEndEditing(_ textView: UITextView) {
        defineNeedPlaceholder()
    }
}

// MARK: - UIFont helper

private extension UIFont {
    static let textFont = CustomizationHelper.systemFont(ofSize: 17.0)
}
