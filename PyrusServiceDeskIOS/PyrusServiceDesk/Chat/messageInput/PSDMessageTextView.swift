import UIKit

let PLACEHOLDER_ALPHA : CGFloat = 0.2
let PSD_MESSAGE_DRAFT_KEY : String = "PSDMessageDraft"

protocol PSDMessageTextViewDelegate: class {
    func textViewChanged()
}

class PSDMessageTextView: UITextView, UITextViewDelegate {
    
    weak var messageDelegate: PSDMessageTextViewDelegate?
    ///Was the view ever shown in a window. Prevents saving before the first appearance.
    private var wasShownInWindow = false
    
    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        self.autoresizingMask = [.flexibleWidth,.flexibleHeight]
        backgroundColor = .clear
        font = .textFont
        tintColor = CustomizationHelper.recordImagesColors
        textColor = .psdLabel
        self.delegate = self
        addPlaceholder()
        self.textContainer.lineFragmentPadding = 0.0
        
        self.text = self.getDraft()
        self.textViewDidChange(self)
        self.keyboardAppearance = CustomizationHelper.keyboardStyle
        NotificationCenter.default.addObserver(self, selector: #selector(saveDraft), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(saveDraft), name: UIApplication.didEnterBackgroundNotification, object: nil)
        
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    private var lastLayoutWidth: CGFloat = 0

    override func layoutSubviews() {
        super.layoutSubviews()
        if lastLayoutWidth != bounds.width {
            lastLayoutWidth = bounds.width
            invalidateIntrinsicContentSize()
        }
    }
    
    override func setContentOffset(_ contentOffset: CGPoint, animated: Bool) {
        super.setContentOffset(contentOffset, animated: false)
    }
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil {
            wasShownInWindow = true
        } else if wasShownInWindow {
            saveDraft()
        }
    }
    
    override var textColor: UIColor? {
        didSet {
            placeholder.textColor = textColor
        }
    }
    
    private let placeholder : UILabel = {
        let label = UILabel ()
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
        rect.size.width = rect.size.width - (rect.origin.x*2)
        placeholder.frame = rect
        
        placeholder.autoresizingMask = [.flexibleWidth,.flexibleHeight]

        self.addSubview(placeholder)
    }
    
    func maxVerticalHeight() -> CGFloat {
        return (self.font?.lineHeight)! * 3.5;
    }
    
    func maxHorizontalHeight() -> CGFloat {
        return (self.font?.lineHeight)! * 2.5;
    }
    
    ///Save current text in UserDefaults.
    @objc private func saveDraft() {
        guard let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults() else {
            return
        }
        
        let trimmed = self.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            pyrusUserDefaults.removeObject(forKey: PSD_MESSAGE_DRAFT_KEY)
        } else {
            pyrusUserDefaults.set(self.text, forKey: PSD_MESSAGE_DRAFT_KEY)
        }
    }
    
    ///Get saved text from UserDefaults.
    private func getDraft()->String? {
        if let pyrusUserDefaults = PSDMessagesStorage.pyrusUserDefaults(){
            return pyrusUserDefaults.string(forKey: PSD_MESSAGE_DRAFT_KEY)
        }
        return ""
    }
    
    ///Clear saved draft. Call it after message was sent.
    func clearDraft() {
        self.text = ""
        PSDMessagesStorage.pyrusUserDefaults()?.removeObject(forKey: PSD_MESSAGE_DRAFT_KEY)
        self.textViewDidChange(self)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    func textViewDidChange(_ textView: UITextView) {
        defineNeedPlaceholder()
        self.messageDelegate?.textViewChanged()
        self.invalidateIntrinsicContentSize()
    }
    
    func textViewDidEndEditing(_ textView: UITextView) {
        defineNeedPlaceholder()
        saveDraft()
    }
    
    private func defineNeedPlaceholder(){
        placeholder.isHidden = self.text.count > 0
    }
    
    override var intrinsicContentSize: CGSize {
        guard bounds.width > 0 else {
            return CGSize(width: 0, height: contentSize.height)
        }
        let fitting = sizeThatFits(CGSize(width: bounds.width,
                                          height: .greatestFiniteMagnitude))
        return CGSize(width: 0, height: ceil(fitting.height))
    }
}

private extension UIFont {
    static let textFont = CustomizationHelper.systemFont(ofSize: 16.0)
}
