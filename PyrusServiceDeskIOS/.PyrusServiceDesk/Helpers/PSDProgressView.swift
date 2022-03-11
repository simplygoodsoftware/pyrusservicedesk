
import UIKit

///Timeout for load from server
let LOAD_TIMEOUT = 120.0// the default time for request timeout

class PSDProgressView: UIView {

    private let animationKey = "strokeEndCustomAnimation"
    private let height : CGFloat = 5.0
    ///Progress of PSDProgressView in range [0...1]
    var progress : CGFloat = 0.0{
        willSet(newValue)
        {
            if(!fake)
            {
                progressLayer.strokeEnd = CGFloat(newValue)
                if(newValue == 1.0){
                    self.resetProgress()
                }
            }
            else if(newValue == 1.0){
                self.forseAnimation()
            }
        }
    }
    ///Color of progress line. Default is yellow.
    var progressColor : UIColor = .goldColor{
        didSet(newColor){
            progressLayer.strokeColor = newColor.cgColor
        }
    }
    
    private let progressLayer = CAShapeLayer()
    
    
    ///Draing PSDProgressView at top of viewController.view
    ///If need custom design and position set it after calling that function
    func draw(at view:UIView){
        
        let rect = CGRect(x: 0, y: view.bounds.origin.y, width: view.bounds.size.width, height: height)
        self.backgroundColor = nil
        
        self.frame = rect
        
        view.addSubview(self)
        
        self.addConstraints(to:view)
        
        
        progressLayer.lineWidth = height*2
        progressLayer.fillColor = nil
        progressLayer.strokeStart = 0.0
        progressLayer.strokeEnd = 0.0
        self.layer.addSublayer(progressLayer)
        
        progressColor = .goldColor
        
        
    }
    override func layoutSubviews() {
        super.layoutSubviews()
        let progressPath =  UIBezierPath()
        
        progressPath.lineWidth = height*2
        progressPath.move(to: CGPoint(x: 0, y: 0))
        progressPath.addLine(to: CGPoint(x: self.bounds.size.width, y: 0))
        progressPath.close()
        progressLayer.path = progressPath.cgPath
    }
    var fake : Bool = false
    
    ///Start progress animation. With LOAD_TIMEOUT duration.
    func startAnimate(){
        fake = true
        progressLayer.strokeEnd = 0.0
        progressLayer.speed = Float(1)
        
        CATransaction.begin()
        CATransaction.setCompletionBlock{ [weak self] in
            if self == nil { return }
            self?.resetProgress()
        }
        let animation : CABasicAnimation = CABasicAnimation(keyPath: "strokeEnd")
        animation.fromValue = 0.0
        animation.toValue = 1.0
        animation.duration = LOAD_TIMEOUT
        animation.autoreverses = false
        animation.timingFunction = CAMediaTimingFunction(name: CAMediaTimingFunctionName.easeOut)
        progressLayer.strokeEnd = 1.0
        progressLayer.add(animation, forKey: animationKey)
        CATransaction.commit()
    }
    ///Return progress to 0 without animation
    private func resetProgress(){
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3, execute:{
            self.isHidden=true
             CATransaction.begin()
            CATransaction.setDisableActions(true)
            self.progressLayer.strokeEnd = 0.0//back to 0 after some delay
            CATransaction.commit()
            self.isHidden=false
        })
    }
    
    ///Quickly ends the animation.
    private func forseAnimation()
    {
        progressLayer.speed = Float(3)
    }
    private func addConstraints(to  view:UIView){
        self.translatesAutoresizingMaskIntoConstraints = false
        if #available(iOS 11.0, *) {
            let guide = view.safeAreaLayoutGuide
            self.topAnchor.constraint(equalTo: guide.topAnchor).isActive = true
        } else {
            NSLayoutConstraint(item: self,
                               attribute: .top,
                               relatedBy: .equal,
                               toItem: view,
                               attribute: .top,
                               multiplier: 1.0,
                               constant: 0).isActive = true
            
        }
        self.addZeroConstraint([.trailing,.leading])
        self.addSizeConstraint([.height], constant: height)
    }
    
}
