import Foundation
let rateArray = [1 : "😩", 2 : "🙁", 3 : "😐", 4 : "🙂", 5 :"😄"]
private let STACK_SPACING: CGFloat = 10

protocol PSDRateViewDelegate: NSObjectProtocol{
    func didTapRate(_ rateValue: Int)
}
/**
 The view to show rating buttons
 */
class PSDRateView: PSDView {
    weak var delegate: PSDRateViewDelegate?
    private let BUTTON_CORNER_RADIUS: CGFloat = 10
    private let STACK_LEFT_SPACING: CGFloat = 5

    private var stackView: UIStackView = {
       let stack = UIStackView()
        stack.axis = .horizontal
        stack.distribution = .fillEqually
        stack.spacing = STACK_SPACING
        return stack
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        addSubview(stackView)
        stackView.translatesAutoresizingMaskIntoConstraints = false
        stackView.leftAnchor.constraint(equalTo: layoutMarginsGuide.leftAnchor, constant:STACK_LEFT_SPACING).isActive = true
        stackView.topAnchor.constraint(equalTo: self.topAnchor, constant: STACK_SPACING).isActive = true
        let bottom = stackView.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant:  -STACK_SPACING)
        bottom.priority = UILayoutPriority.defaultHigh
        bottom.isActive = true
        stackView.rightAnchor.constraint(equalTo: layoutMarginsGuide.rightAnchor, constant: -STACK_LEFT_SPACING).isActive = true
        
        createRate()
    }
    private func createRate() {
        for rate  in rateArray.sorted(by: {$0.0 < $1.0}){
            let button  = UIButton()
            button.layer.cornerRadius = BUTTON_CORNER_RADIUS
            button.setTitle(rate.value, for: .normal)
            button.tag = rate.key
            button.setBackgroundColor(color: CustomizationHelper.lightGrayViewColor, forState: .normal)
            button.setBackgroundColor(color: CustomizationHelper.grayViewColor, forState: .highlighted)
            
            button.addTarget(self, action: #selector(didTapButton), for: .touchUpInside)
            
            stackView.addArrangedSubview(button)
            button.translatesAutoresizingMaskIntoConstraints = false
            button.heightAnchor.constraint(equalTo: stackView.heightAnchor).isActive = true
        }
    }
    @objc private func didTapButton(_ button: UIButton) {
        delegate?.didTapRate(button.tag)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
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
