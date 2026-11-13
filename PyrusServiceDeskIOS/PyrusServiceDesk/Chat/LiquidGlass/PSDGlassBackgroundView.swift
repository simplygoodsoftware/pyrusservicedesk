import UIKit

/// Подложка в стиле Liquid Glass.
///
/// На iOS 26+ использует системный `UIGlassEffect` (он сам подстраивается под
/// светлую/тёмную тему и под содержимое за собой). На более ранних версиях системы
/// деградирует до системного материала, чтобы вью оставалось пригодным
/// для переиспользования вне Liquid Glass-сценариев.
final class PSDGlassBackgroundView: UIView {
    
    /// Форма стекла.
    enum Shape {
        /// Капсула: радиус — половина меньшей стороны. Для кнопок и однострочных пилюль.
        case capsule
        /// Фиксированный радиус. Для контейнеров, которые растут в высоту:
        /// при минимальной высоте выглядит капсулой, при росте не раздувает углы.
        case rounded(radius: CGFloat)
    }
    
    private enum Constants {
        /// Материал-заглушка для систем без Liquid Glass.
        static let fallbackBlurStyle: UIBlurEffect.Style = .systemThinMaterial
    }
    
    /// Контейнер, в который нужно складывать содержимое, лежащее поверх стекла.
    var glassContentView: UIView {
        effectView.contentView
    }
    
    private let isInteractive: Bool
    private let shape: Shape
    
    private lazy var effectView: UIVisualEffectView = {
        let view = UIVisualEffectView(effect: Self.makeEffect(isInteractive: isInteractive))
        view.translatesAutoresizingMaskIntoConstraints = false
        // Форму стекла задаёт только cornerConfiguration и только после установки эффекта:
        // layer.cornerRadius на UIGlassEffect система игнорирует.
        if #available(iOS 26.0, *) {
            switch shape {
            case .capsule:
                view.cornerConfiguration = .capsule()
            case .rounded(let radius):
                view.cornerConfiguration = .corners(radius: .fixed(radius))
            }
        }
        return view
    }()
    
    /// - Parameter isInteractive: включает отклик стекла на касания.
    ///   Имеет смысл только для подложек под интерактивными элементами.
    /// - Parameter shape: форма стекла, по умолчанию капсула.
    init(isInteractive: Bool = false, shape: Shape = .capsule) {
        self.isInteractive = isInteractive
        self.shape = shape
        super.init(frame: .zero)
        setupLayout()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        updateFallbackShapeIfNeeded()
    }
    
    private func setupLayout() {
        backgroundColor = .clear
        addSubview(effectView)
        
        NSLayoutConstraint.activate([
            effectView.topAnchor.constraint(equalTo: topAnchor),
            effectView.bottomAnchor.constraint(equalTo: bottomAnchor),
            effectView.leadingAnchor.constraint(equalTo: leadingAnchor),
            effectView.trailingAnchor.constraint(equalTo: trailingAnchor)
        ])
    }
    
    private static func makeEffect(isInteractive: Bool) -> UIVisualEffect {
        if #available(iOS 26.0, *) {
            let effect = UIGlassEffect()
            effect.isInteractive = isInteractive
            return effect
        }
        return UIBlurEffect(style: Constants.fallbackBlurStyle)
    }
    
    /// На iOS 26+ форму держит `cornerConfiguration`, выставленный при создании эффекта.
    /// Для материала-заглушки формы нет — скругляем слой вручную.
    private func updateFallbackShapeIfNeeded() {
        if #available(iOS 26.0, *) {
            return
        }
        switch shape {
        case .capsule:
            effectView.layer.cornerRadius = min(bounds.width, bounds.height) / 2
        case .rounded(let radius):
            effectView.layer.cornerRadius = radius
        }
        effectView.layer.masksToBounds = true
    }
}
