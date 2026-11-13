import UIKit

/// Полупрозрачная подложка: лёгкий blur + едва заметное затемнение
/// контента за ней, плавно затухающие к одному из краёв.
///
/// Используется как под шапкой / прозрачным navigation bar (затухание вниз),
/// так и за полем ввода (затухание вверх). Компонент самодостаточен и не
/// привязан к конкретному экрану — переиспользуется чат-подобными экранами.
public final class BlurBackdropView: UIView {

    /// К какому краю подложка затухает в прозрачность.
    public enum FadeEdge {
        case top
        case bottom
    }

    /// Значение `blurIntensity` по умолчанию — мягкий blur.
    public static let defaultBlurIntensity: CGFloat = 0.08

    /// Цвет dim-слоя по умолчанию — системный светлый/тёмный по теме.
    public static let defaultDimColor = UIColor.themedColor(
        lightColor: UIColor.white.withAlphaComponent(Constants.dimAlpha),
        darkColor: UIColor.black.withAlphaComponent(Constants.dimAlpha)
    )

    /// Цвет затемняющей подложки (вместе с альфой).
    /// Экраны с кастомизацией цветов могут подставить свой — например, цвет
    /// навигационного бара интегратора, который не обязан совпадать с темой системы.
    public var dimColor: UIColor {
        didSet {
            dimView.backgroundColor = dimColor
        }
    }

    private let fadeEdge: FadeEdge

    /// Сила blur'а от 0 до 1:
    /// 0 — выключен, 1 — полный `blurStyle`. Чем выше, тем менее прозрачно.
    private let blurIntensity: CGFloat

    private let blurView: UIVisualEffectView = {
        // Стартуем без эффекта — нужную силу blur навешивает
        // `applyBlurIntensity` через paused `UIViewPropertyAnimator`.
        let view = UIVisualEffectView(effect: nil)
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private let dimView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    /// Маска blur'а живёт в отдельном вью: у `UIVisualEffectView` нельзя
    /// маскировать слой (`layer.mask`) — эффект деградирует, градиент
    /// превращается в бинарный обрез с чёткой границей. Маскировать можно
    /// только через свойство `mask` самого вью.
    /// Ссылка сильная: `mask` вью не ретейнит.
    private lazy var blurMaskView: UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        view.layer.addSublayer(blurMaskLayer)
        return view
    }()

    /// Маска blur'а: в сплошной зоне строго непрозрачна.
    private lazy var blurMaskLayer = Self.makeMaskLayer(solidAlpha: 1, fadeEdge: fadeEdge)

    /// Маска dim-слоя. Обычный вью слоем маскировать можно.
    private lazy var dimMaskLayer = Self.makeMaskLayer(
        solidAlpha: Constants.solidAlpha,
        fadeEdge: fadeEdge
    )

    /// Удерживает blur на нужной силе через `fractionComplete`.
    /// Никогда не запускается.
    private var blurAnimator: UIViewPropertyAnimator?

    public init(fadeEdge: FadeEdge,
                blurIntensity: CGFloat = BlurBackdropView.defaultBlurIntensity,
                dimColor: UIColor = BlurBackdropView.defaultDimColor) {
        self.fadeEdge = fadeEdge
        self.blurIntensity = blurIntensity
        self.dimColor = dimColor
        super.init(frame: .zero)
        isUserInteractionEnabled = false

        dimView.backgroundColor = dimColor
        addSubview(blurView)
        addSubview(dimView)

        NSLayoutConstraint.activate([
            blurView.topAnchor.constraint(equalTo: topAnchor),
            blurView.leadingAnchor.constraint(equalTo: leadingAnchor),
            blurView.trailingAnchor.constraint(equalTo: trailingAnchor),
            blurView.bottomAnchor.constraint(equalTo: bottomAnchor),

            dimView.topAnchor.constraint(equalTo: topAnchor),
            dimView.leadingAnchor.constraint(equalTo: leadingAnchor),
            dimView.trailingAnchor.constraint(equalTo: trailingAnchor),
            dimView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        blurView.mask = blurMaskView
        dimView.layer.mask = dimMaskLayer

        applyBlurIntensity()

        // Снапшоты при уходе приложения в фон сбрасывают paused-аниматор —
        // blur схлопывается в полную силу материала. После активации пересобираем.
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(applicationDidBecomeActive),
                                               name: UIApplication.didBecomeActiveNotification,
                                               object: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
        // Без явной остановки paused-аниматор продолжает удерживать ссылку
        // на блок анимации
        blurAnimator?.stopAnimation(true)
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil else { return }
        applyBlurIntensity()
    }

    public override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        // Частично применённый эффект не пересчитывается под новую тему сам.
        applyBlurIntensity()
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        // setDisableActions(true) гасит анимацию, иначе при
        // ресайзе градиент плывёт вслед за bounds.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        blurMaskView.frame = blurView.bounds
        blurMaskLayer.frame = blurMaskView.bounds
        dimMaskLayer.frame = dimView.bounds
        CATransaction.commit()
    }

    @objc private func applicationDidBecomeActive() {
        applyBlurIntensity()
    }

    /// Пересоздаёт paused-аниматор и выставляет силу blur'а заново.
    /// Идемпотентно: вызывается при попадании в окно, активации приложения
    /// и смене темы.
    private func applyBlurIntensity() {
        blurAnimator?.stopAnimation(true)
        blurView.effect = nil

        let animator = UIViewPropertyAnimator(duration: 1, curve: .linear)
        animator.addAnimations { [weak self] in
            self?.blurView.effect = UIBlurEffect(style: Constants.blurStyle)
        }
        animator.fractionComplete = blurIntensity
        animator.pausesOnCompletion = true
        blurAnimator = animator
    }

    private static func makeMaskLayer(
        solidAlpha: CGFloat,
        fadeEdge: FadeEdge
    ) -> CAGradientLayer {
        let layer = CAGradientLayer()
        layer.startPoint = CGPoint(x: 0.5, y: 0)
        layer.endPoint = CGPoint(x: 0.5, y: 1)

        let steps = Constants.fadeSteps
        let fade = Constants.fadeFraction

        func color(_ multiplier: CGFloat) -> CGColor {
            UIColor.white.withAlphaComponent(solidAlpha * multiplier).cgColor
        }

        var colors: [CGColor] = []
        var locations: [NSNumber] = []

        // Зону затухания считаем по smoothstep: у её начала альфа держится
        // почти на максимуме (не съедает заголовок), а конец мягко уходит в
        // ноль — вместо линейного затухания с резким обрывом на самом краю.
        switch fadeEdge {
        case .bottom:
            let fadeStart = 1 - fade
            colors.append(color(1))
            locations.append(0)
            for i in 0...steps {
                let p = CGFloat(i) / CGFloat(steps)
                colors.append(color(1 - smoothstep(p)))
                locations.append(NSNumber(value: Float(fadeStart + fade * p)))
            }
        case .top:
            for i in 0...steps {
                let p = CGFloat(i) / CGFloat(steps)
                colors.append(color(smoothstep(p)))
                locations.append(NSNumber(value: Float(fade * p)))
            }
            colors.append(color(1))
            locations.append(1)
        }

        layer.colors = colors
        layer.locations = locations
        return layer
    }

    private static func smoothstep(_ t: CGFloat) -> CGFloat {
        let x = min(max(t, 0), 1)
        return x * x * (3 - 2 * x)
    }
}

private extension BlurBackdropView {
    enum Constants {
        static let blurStyle: UIBlurEffect.Style = .systemUltraThinMaterial

        /// Доля высоты (от края затухания), на которой подложка плавно уходит
        /// в прозрачность. Раньше было 0.1 с линейным градиентом — обрыв на
        /// краю читался слишком резко.
        static let fadeFraction: CGFloat = 0.4

        /// Число промежуточных этапов в зоне затухания: чем больше, тем глаже
        /// кривая smoothstep (визуально достаточно ~8).
        static let fadeSteps: Int = 8

        /// Альфа dim-слоя в «статичной» зоне градиента. На blur НЕ влияет.
        static let solidAlpha: CGFloat = 0.95

        /// Базовая альфа затемняющей подложки.
        static let dimAlpha: CGFloat = 0.45
    }
}
