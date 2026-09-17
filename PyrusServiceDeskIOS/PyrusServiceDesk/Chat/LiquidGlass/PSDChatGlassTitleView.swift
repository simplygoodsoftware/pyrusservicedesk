import UIKit

/// Заголовок чата в стиле Liquid Glass — текст на стеклянной капсуле.
///
/// Рассчитан на использование в качестве `navigationItem.titleView`, поэтому
/// сам сообщает навигационному бару свой размер через `intrinsicContentSize`
/// и `sizeThatFits(_:)`, а при нехватке ширины обрезает текст, а не выдавливает кнопки.
final class PSDChatGlassTitleView: UIView {
    
    private enum Layout {
        static let height: CGFloat = 40
        static let horizontalInset: CGFloat = 14
        static let fontSize: CGFloat = 17
    }
    
    /// Текст заголовка.
    var title: String? {
        get { titleLabel.text }
        set {
            guard titleLabel.text != newValue else { return }
            titleLabel.text = newValue
            invalidateIntrinsicContentSize()
        }
    }
    
    /// Цвет текста заголовка.
    var titleColor: UIColor {
        get { titleLabel.textColor }
        set { titleLabel.textColor = newValue }
    }
    
    private let glassBackgroundView = PSDGlassBackgroundView()
    
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.font = CustomizationHelper.systemBoldFont(ofSize: Layout.fontSize)
        label.textAlignment = .center
        label.lineBreakMode = .byTruncatingTail
        label.numberOfLines = 1
        // Заголовок должен сжиматься первым: кнопки бара важнее полного текста.
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return label
    }()
    
    init() {
        super.init(frame: .zero)
        setupLayout()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override var intrinsicContentSize: CGSize {
        CGSize(width: titleLabel.intrinsicContentSize.width + Layout.horizontalInset * 2,
               height: Layout.height)
    }
    
    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let availableWidth = size.width > 0 ? size.width : .greatestFiniteMagnitude
        let maxLabelWidth = max(0, availableWidth - Layout.horizontalInset * 2)
        let labelWidth = titleLabel.sizeThatFits(CGSize(width: maxLabelWidth,
                                                       height: Layout.height)).width
        let width = min(availableWidth, labelWidth + Layout.horizontalInset * 2)
        return CGSize(width: width, height: Layout.height)
    }
    
    private func setupLayout() {
        isUserInteractionEnabled = false
        backgroundColor = .clear
        
        glassBackgroundView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(glassBackgroundView)
        glassBackgroundView.glassContentView.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            glassBackgroundView.topAnchor.constraint(equalTo: topAnchor),
            glassBackgroundView.bottomAnchor.constraint(equalTo: bottomAnchor),
            glassBackgroundView.leadingAnchor.constraint(equalTo: leadingAnchor),
            glassBackgroundView.trailingAnchor.constraint(equalTo: trailingAnchor),
            
            titleLabel.centerYAnchor.constraint(equalTo: glassBackgroundView.glassContentView.centerYAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: glassBackgroundView.glassContentView.leadingAnchor,
                                                constant: Layout.horizontalInset),
            titleLabel.trailingAnchor.constraint(equalTo: glassBackgroundView.glassContentView.trailingAnchor,
                                                 constant: -Layout.horizontalInset)
        ])
    }
}
