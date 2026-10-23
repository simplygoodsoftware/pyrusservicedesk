import Foundation
var rateArray = [RatingTextValue]()
private let STACK_SPACING: CGFloat = 8

protocol PSDRateViewDelegate: NSObjectProtocol{
    func didTapRate(_ rateValue: Int)
}
/**
 The view to show rating buttons
 */
class PSDTextRateView: PSDView, RateViewProtocol {
    weak var tapDelegate: PSDRateViewDelegate?
    private let BUTTON_CORNER_RADIUS: CGFloat = 10
    private let STACK_LEFT_SPACING: CGFloat = 5
    
    ///Геометрия Liquid Glass оформления. Расположение плашек не меняется —
    ///только их вид и общий стеклянный контейнер.
    private enum GlassLayout {
        static let chipHeight: CGFloat = 40
        static let containerPadding: CGFloat = 12
        ///Отступ стеклянного контейнера от краёв экрана.
        static let containerSideInset: CGFloat = 16
        static let containerCornerRadius: CGFloat = 24
    }
    
    ///Стеклянный контейнер под всеми плашками. Используется только в Liquid Glass оформлении.
    private lazy var glassBackground = PSDGlassBackgroundView(shape: .rounded(radius: GlassLayout.containerCornerRadius))

    private var stackView: UIStackView = {
       let stack = UIStackView()
        stack.axis = .vertical
        stack.alignment = .center
        stack.distribution = .fill
        stack.spacing = STACK_SPACING
        return stack
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        if PSDLiquidGlassStyle.isEnabled {
            addSubview(glassBackground)
            glassBackground.translatesAutoresizingMaskIntoConstraints = false
        }
        addSubview(stackView)
        stackView.translatesAutoresizingMaskIntoConstraints = false
        stackView.leftAnchor.constraint(equalTo: layoutMarginsGuide.leftAnchor, constant: STACK_LEFT_SPACING).isActive = true
       // stackView.topAnchor.constraint(equalTo: self.topAnchor, constant: STACK_SPACING).isActive = true
        let bottom = stackView.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant:  -12)
        bottom.priority = UILayoutPriority.defaultHigh
        bottom.isActive = true
        stackView.rightAnchor.constraint(equalTo: layoutMarginsGuide.rightAnchor, constant: -STACK_LEFT_SPACING).isActive = true
        
        if PSDLiquidGlassStyle.isEnabled {
            //По вертикали контейнер повторяет стек, по горизонтали держит отступ
            //от краёв экрана — сама вью растянута на всю ширину панели.
            NSLayoutConstraint.activate([
                glassBackground.topAnchor.constraint(equalTo: stackView.topAnchor, constant: -GlassLayout.containerPadding),
                glassBackground.bottomAnchor.constraint(equalTo: stackView.bottomAnchor, constant: GlassLayout.containerPadding),
                glassBackground.leadingAnchor.constraint(equalTo: leadingAnchor, constant: GlassLayout.containerSideInset),
                glassBackground.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -GlassLayout.containerSideInset)
            ])
        }
    }
    
    @MainActor required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func configure(with rateValues: [RatingTextValue]) {
        rateArray = rateValues
        createRate()
    }
    
    private func createRate() {
        stackView.arrangedSubviews.forEach { subview in
            stackView.removeArrangedSubview(subview)
            subview.removeFromSuperview()
        }
        for rate in rateArray.sorted(by: {$0.rating > $1.rating}){
            let button = UIButton(type: .system)
            button.setTitle(rate.text, for: .normal)
            button.setTitleColor(CustomizationHelper.textColorForInput, for: .normal)
            button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
            button.layer.cornerRadius = PSDLiquidGlassStyle.isEnabled ? GlassLayout.chipHeight / 2 : 8
            button.clipsToBounds = true
            button.contentEdgeInsets = UIEdgeInsets(top: 9.5, left: 12, bottom: 9.5, right: 12)
            button.setBackgroundColor(color: CustomizationHelper.supportMassageBackgroundColor, forState: .normal)
            button.setBackgroundColor(color: CustomizationHelper.grayViewColor, forState: .highlighted)
            button.tag = rate.rating
            
            button.addTarget(self, action: #selector(didTapButton), for: .touchUpInside)
            
            stackView.addArrangedSubview(button)
            button.translatesAutoresizingMaskIntoConstraints = false
            if PSDLiquidGlassStyle.isEnabled {
                //Пилюля фиксированной высоты — радиус в её половину.
                button.heightAnchor.constraint(equalToConstant: GlassLayout.chipHeight).isActive = true
            }
            //  button.widthAnchor.constraint(equalToConstant: 60).isActive = true
            // button.heightAnchor.constraint(equalTo: stackView.heightAnchor).isActive = true
        }
    }
    //ВРЕМЕННО: диагностика задержки, удалить вместе с PSDRateDebug.
    override func layoutSubviews() {
        super.layoutSubviews()
        PSDRateDebug.log("textRate layout self=\(frame.height) stack=\(stackView.frame.height) glass=\(glassBackground.frame.height)")
    }
    
    @objc private func didTapButton(_ button: UIButton) {
        tapDelegate?.didTapRate(button.tag)
    }
    
    override func recolor() {
        super.recolor()
        //В Liquid Glass цвета плашек не зависят от смены темы напрямую:
        //setBackgroundColor уже получил динамические цвета кастомизации.
        guard !PSDLiquidGlassStyle.isEnabled else { return }
        for view in stackView.arrangedSubviews {
            guard let button = view as? UIButton else {
                continue
            }
            button.setBackgroundColor(color: CustomizationHelper.lightGrayViewColor, forState: .normal)
            button.setBackgroundColor(color: CustomizationHelper.grayViewColor, forState: .highlighted)
        }
    }
}
