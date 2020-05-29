import UIKit
protocol PSDNoConnectionViewDelegate
: class {
    func retryPressed()
}
///A view with "no connection" message and retry button. This view has same size as it's superview. When retry button pressed - pass to its delegate retryPressed()
class PSDNoConnectionView: UIView {
    weak var delegate: PSDNoConnectionViewDelegate?
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .psdLightGray
        self.addSubview(noConnectionLabel)
        self.addSubview(retryButton)
        setConstraints()
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private lazy var noConnectionLabel : UILabel = {
        let label = UILabel()
        label.textColor = .psdGray
        label.font = UIFont.systemFont(ofSize: 17.0)
        label.text = "No_Connection".localizedPSD()
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.textAlignment = .center
        label.preferredMaxLayoutWidth = self.frame.size.width - (PSDNoConnectionView.dist*2)
        label.sizeToFit()
        return label
    }()
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        self.translatesAutoresizingMaskIntoConstraints = false
        self.addZeroConstraint([.top,.bottom,.leading,.trailing])
    }
    private let retryButtonFontSize: CGFloat = 17.0
    private lazy var retryButton : UIButton = {
        let button = UIButton()
        button.titleLabel?.font = UIFont.systemFont(ofSize: retryButtonFontSize)
        button.setTitle("Retry".localizedPSD(), for: .normal)
        button.setTitleColor(.darkAppColor, for: .normal)
        button.addTarget(self, action: #selector(retryButtonPressed), for: .touchUpInside)
        return button
    }()
    @objc private func retryButtonPressed(){
        self.delegate?.retryPressed()
    }
    
    private static let dist : CGFloat = 20.0
    private static let distBetwinElement : CGFloat  = 15
    private func setConstraints(){
        noConnectionLabel.translatesAutoresizingMaskIntoConstraints = false
        retryButton.translatesAutoresizingMaskIntoConstraints = false
        
        self.setLateralConstraints(noConnectionLabel)
        self.setLateralConstraints(retryButton)
        
        self.addConstraint(NSLayoutConstraint(
            item: noConnectionLabel,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: retryButton,
            attribute: .top,
            multiplier: 1,
            constant: -PSDNoConnectionView.distBetwinElement))
        
        self.addConstraint(NSLayoutConstraint(
            item: retryButton,
            attribute: .bottom,
            relatedBy: .equal,
            toItem: self,
            attribute: .centerY,
            multiplier: 1,
            constant:0))
        
        
        
    }
    private func setLateralConstraints(_ view:UIView){
        view.leftAnchor.constraint(
            equalTo: self.layoutMarginsGuide.leftAnchor,
            constant: PSDNoConnectionView.dist
            ).isActive = true
        view.rightAnchor.constraint(
            equalTo: self.layoutMarginsGuide.rightAnchor,
            constant: -PSDNoConnectionView.dist
            ).isActive = true
    }

}
