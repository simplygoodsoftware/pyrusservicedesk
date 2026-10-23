import UIKit

/// Градиентная блюр-подложка под навигационным баром.
///
/// Обычно такую полосу рисует система — краевым эффектом скролла. На экране чата
/// это не работает: таблица развёрнута на 180° и живёт с
/// `contentInsetAdjustmentBehavior = .never`, поэтому системный эффект либо
/// промахивается краем и гасит всю переписку, либо не появляется вовсе.
///
/// Раз подложку приходится рисовать самим, она обязана подстраиваться под цвета
/// кастомизации: системный материал знает только про светлую и тёмную тему, а фон
/// и цвет бара интегратор задаёт любыми. Плотность набирается двумя слоями —
/// материалом нужной светлоты, который честно размывает уезжающую под бар переписку,
/// и лёгкой подмешкой `CustomizationHelper.navigationBarColor` поверх.
///
/// ВАЖНО про маску: у `UIVisualEffectView` нельзя маскировать слой (`layer.mask`) —
/// эффект при этом деградирует, размытие пропадает и остаётся цветная вуаль.
/// Маска задаётся только через свойство `mask` самого вью.
final class PSDNavigationBlurView: UIView {
    
    private enum Constants {
        /// Доля высоты, на которой подложка держится в полную силу.
        static let solidFraction: CGFloat = 0.45
        /// Число ступеней градиента. Чем больше, тем ближе переход к непрерывному.
        static let gradientStepCount = 16
        /// Максимальная плотность цветовой подмешки. Больше — точнее попадание в цвет бара,
        /// меньше — заметнее размытая переписка под ним.
        static let maxTintAlpha: CGFloat = 0.55
    }
    
    private lazy var effectView: UIVisualEffectView = {
        let view = UIVisualEffectView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    ///Вью-маска для effectView. Держим сильную ссылку: свойство `mask` вью не ретейнит.
    private lazy var blurMaskView: UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        view.layer.addSublayer(blurMaskLayer)
        return view
    }()
    
    private lazy var tintView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .clear
        view.layer.addSublayer(tintLayer)
        return view
    }()
    
    private let blurMaskLayer = CAGradientLayer()
    private let tintLayer = CAGradientLayer()
    
    init() {
        super.init(frame: .zero)
        setupLayout()
        recolor()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /// Пересобирает материал и подмешку под текущие цвета кастомизации.
    /// Вызывать при смене темы — цвет бара может зависеть от `userInterfaceStyle`.
    func recolor() {
        let barColor = CustomizationHelper.navigationBarColor
        
        effectView.effect = UIBlurEffect(style: blurStyle(for: barColor))
        
        let mask = Self.makeGradientStops(color: .black, maxAlpha: 1)
        blurMaskLayer.colors = mask.colors
        blurMaskLayer.locations = mask.locations
        
        let tint = Self.makeGradientStops(color: barColor, maxAlpha: Constants.maxTintAlpha)
        tintLayer.colors = tint.colors
        tintLayer.locations = tint.locations
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        //Маска и слои живут вне Auto Layout, размер им задаётся здесь.
        //CATransaction — чтобы полоса не отставала от вью при повороте экрана.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        blurMaskView.frame = effectView.bounds
        blurMaskLayer.frame = blurMaskView.bounds
        tintLayer.frame = tintView.bounds
        CATransaction.commit()
    }
    
    private func setupLayout() {
        //Подложка декоративная: касания должны доходить до переписки под ней.
        isUserInteractionEnabled = false
        backgroundColor = .clear
        
        addSubview(effectView)
        addSubview(tintView)
        
        //Именно mask, а не layer.mask — см. комментарий к классу.
        effectView.mask = blurMaskView
        
        [blurMaskLayer, tintLayer].forEach {
            $0.startPoint = CGPoint(x: 0.5, y: 0)
            $0.endPoint = CGPoint(x: 0.5, y: 1)
        }
        
        NSLayoutConstraint.activate([
            effectView.topAnchor.constraint(equalTo: topAnchor),
            effectView.bottomAnchor.constraint(equalTo: bottomAnchor),
            effectView.leadingAnchor.constraint(equalTo: leadingAnchor),
            effectView.trailingAnchor.constraint(equalTo: trailingAnchor),
            
            tintView.topAnchor.constraint(equalTo: topAnchor),
            tintView.bottomAnchor.constraint(equalTo: bottomAnchor),
            tintView.leadingAnchor.constraint(equalTo: leadingAnchor),
            tintView.trailingAnchor.constraint(equalTo: trailingAnchor)
        ])
    }
    
    /// Материал выбирается по светлоте цвета бара, а не по теме системы:
    /// интегратор может задать тёмный фон в светлой теме и наоборот.
    /// Thin, а не chrome: поверх ещё ложится цветная подмешка, и с плотным
    /// материалом размытая переписка переставала просвечивать.
    private func blurStyle(for barColor: UIColor) -> UIBlurEffect.Style {
        barColor.isDarkColor ? .systemThinMaterialDark : .systemThinMaterialLight
    }
    
    /// Ступени градиента: полная плотность до `solidFraction`, дальше спад по кривой сглаживания.
    private static func makeGradientStops(color: UIColor,
                                          maxAlpha: CGFloat) -> (colors: [CGColor], locations: [NSNumber]) {
        var colors: [CGColor] = [color.withAlphaComponent(maxAlpha).cgColor]
        var locations: [NSNumber] = [0]
        
        let fadeLength = 1 - Constants.solidFraction
        for step in 0...Constants.gradientStepCount {
            let progress = CGFloat(step) / CGFloat(Constants.gradientStepCount)
            colors.append(color.withAlphaComponent(maxAlpha * fadeCurve(progress)).cgColor)
            locations.append(NSNumber(value: Double(Constants.solidFraction + fadeLength * progress)))
        }
        return (colors, locations)
    }
    
    /// Линейный спад даёт видимую границу на обоих концах. Smoothstep убирает её.
    private static func fadeCurve(_ progress: CGFloat) -> CGFloat {
        let t = max(0, min(1, progress))
        return 1 - (t * t * (3 - 2 * t))
    }
}
