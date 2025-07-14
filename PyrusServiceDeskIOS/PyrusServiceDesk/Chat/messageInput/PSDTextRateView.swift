import Foundation
var rateArray = [RatingTextValue]()//[1 : "😩", 2 : "🙁", 3 : "😐", 4 : "🙂", 5 :"😄"]
private let STACK_SPACING: CGFloat = 8

enum EmojiRateType {
    case smile
    case like
    
    func rateArray(size: Int) -> [Int: String] {
        switch self {
        case .smile:
            switch size {
            case 2:
                return [1: "😩", 2: "😄"]
            case 3:
                return [1: "😩", 2: "😐", 3: "😄"]
            default:
                return [1: "Ужасно", 2: "Плохо", 3: "Удовлетворительно", 4: "Хорошо", 5: "Отлично"]//[1: "😩", 2: "🙁", 3: "😐", 4: "🙂", 5: "😄"]
            }
        case .like:
            return [1: "👎", 2: "👍"]
        }
    }
}

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
        
        addSubview(stackView)
        stackView.translatesAutoresizingMaskIntoConstraints = false
        stackView.leftAnchor.constraint(equalTo: layoutMarginsGuide.leftAnchor, constant: STACK_LEFT_SPACING).isActive = true
       // stackView.topAnchor.constraint(equalTo: self.topAnchor, constant: STACK_SPACING).isActive = true
        let bottom = stackView.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant:  -12)
        bottom.priority = UILayoutPriority.defaultHigh
        bottom.isActive = true
        stackView.rightAnchor.constraint(equalTo: layoutMarginsGuide.rightAnchor, constant: -STACK_LEFT_SPACING).isActive = true
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
        for rate  in rateArray.sorted(by: {$0.rating > $1.rating}){
            let button = UIButton(type: .system)
            button.setTitle(rate.text, for: .normal)
            button.setTitleColor(CustomizationHelper.textColorForInput, for: .normal)
            button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
            button.layer.cornerRadius = 8
            button.contentEdgeInsets = UIEdgeInsets(top: 9.5, left: 12, bottom: 9.5, right: 12)
            button.setBackgroundColor(color: CustomizationHelper.supportMassageBackgroundColor, forState: .normal)
            button.setBackgroundColor(color: CustomizationHelper.grayViewColor, forState: .highlighted)
            button.tag = rate.rating
            
            button.addTarget(self, action: #selector(didTapButton), for: .touchUpInside)
            
            stackView.addArrangedSubview(button)
            button.translatesAutoresizingMaskIntoConstraints = false
            //  button.widthAnchor.constraint(equalToConstant: 60).isActive = true
            // button.heightAnchor.constraint(equalTo: stackView.heightAnchor).isActive = true
        }
    }
    @objc private func didTapButton(_ button: UIButton) {
        tapDelegate?.didTapRate(button.tag)
    }
    
    override func recolor() {
        super.recolor()
        for view in stackView.arrangedSubviews {
            guard let button = view as? UIButton else {
                continue
            }
            button.setBackgroundColor(color: CustomizationHelper.lightGrayViewColor, forState: .normal)
            button.setBackgroundColor(color: CustomizationHelper.grayViewColor, forState: .highlighted)
        }
    }
}
