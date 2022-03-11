
import UIKit

///Delegate for PSDProgressDownloadView
protocol PSDProgressDownloadViewDelegate: class {
    ///Pass message that cancel button was pressed
    func cancelPressed()
}
///A view with progress and cancel button. This view has same size as it's superview. When cancel button pressed - pass to its delegate cancelPressed()
class PSDProgressDownloadView: UIView {
    weak var delegate: PSDProgressDownloadViewDelegate?
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdLightGray
        self.addSubview(progressLoader)
        self.addSubview(cancelButton)
        setConstraints()
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    private let progressLayer: CAShapeLayer = CAShapeLayer()
    ///The progress of loading. Using to show progress in progress' view
    var progress: CGFloat = 0.0{
        didSet{
            CATransaction.begin()
            CATransaction.setCompletionBlock{ [weak self] in
                if (self?.progress ?? 0) == 1.0{
                    self?.progress = 0.0
                }
                
            }
            progressLayer.strokeEnd = progress
            CATransaction.commit()
        
        }
    }
    private static let progressLoaderSize = PSDDownloadView.downloadSize
    private lazy var progressLoader : UIView = {
        let view = UIView.init(frame: CGRect(x: 0, y: 0, width: PSDProgressDownloadView.progressLoaderSize, height: PSDProgressDownloadView.progressLoaderSize))
        view.layer.borderWidth = 1.0
        view.layer.borderColor = UIColor.darkAppColor.cgColor
        view.layer.cornerRadius = PSDProgressDownloadView.progressLoaderSize/2
        
        let bgPath: UIBezierPath = UIBezierPath()
        let x = view.frame.width/2
        let y = view.frame.height/2
        let center = CGPoint(x: x, y: y)
        bgPath.addArc(withCenter: center, radius: x-2, startAngle: -.pi/2, endAngle: .pi*(3/2), clockwise: true)
        bgPath.close()
        
        let shapeLayer: CAShapeLayer = CAShapeLayer()
        
        shapeLayer.path = bgPath.cgPath
        shapeLayer.lineWidth = PROGRESS_LINE_WIDTH
        shapeLayer.strokeColor = UIColor.clear.cgColor
        progressLayer.path = bgPath.cgPath
        progressLayer.lineWidth = shapeLayer.lineWidth
        progressLayer.lineCap = CAShapeLayerLineCap.round
        progressLayer.fillColor = nil
        progressLayer.strokeEnd = 0.0
        
        progressLayer.strokeColor = UIColor.darkAppColor.cgColor
        shapeLayer.fillColor = nil
        
        view.layer.addSublayer(shapeLayer)
        view.layer.addSublayer(progressLayer)
        
        view.backgroundColor = .clear
        return view
    }()
    private lazy var cancelButton : UIButton = {
        let button = UIButton()
        button.titleLabel?.font = .cancelButton
        button.setTitle("CancelDownload".localizedPSD(), for: .normal)
        button.setTitleColor(.darkAppColor, for: .normal)
        button.addTarget(self, action: #selector(cancelButtonPressed), for: .touchUpInside)
        return button
    }()
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        self.translatesAutoresizingMaskIntoConstraints = false
        self.addZeroConstraint([.top,.bottom,.leading,.trailing])
    }
    @objc private func cancelButtonPressed(){
        self.delegate?.cancelPressed()
    }
    private static let dist : CGFloat = 20.0
    private static let distBetwinElement : CGFloat  = 15
    private func setConstraints(){
        progressLoader.translatesAutoresizingMaskIntoConstraints = false
        cancelButton.translatesAutoresizingMaskIntoConstraints = false
        
        self.setLateralConstraints(cancelButton)
        
        progressLoader.addZeroConstraint([.centerY,.centerX])
        progressLoader.addSizeConstraint([.width,.height], constant: PSDProgressDownloadView.progressLoaderSize)
        self.addConstraint(NSLayoutConstraint(
            item: cancelButton,
            attribute: .top,
            relatedBy: .equal,
            toItem: progressLoader,
            attribute: .bottom,
            multiplier: 1,
            constant:PSDProgressDownloadView.distBetwinElement))
    }
    private func setLateralConstraints(_ view:UIView){
        view.leftAnchor.constraint(
            equalTo: self.layoutMarginsGuide.leftAnchor,
            constant: PSDProgressDownloadView.dist
            ).isActive = true
        view.rightAnchor.constraint(
            equalTo: self.layoutMarginsGuide.rightAnchor,
            constant: -PSDProgressDownloadView.dist
            ).isActive = true
    }
}
private extension UIFont {
    static let cancelButton = CustomizationHelper.systemFont(ofSize: 17.0)
}
