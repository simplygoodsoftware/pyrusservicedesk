
import UIKit
protocol PSDUploadViewDelegate: class {
    func stopUpload()
}
/**
 View to show Attachment state.
 Drawing in 3 state that is indicated by its progress value and downloadState:
 1) if progress < 1 is drawn as loading:
    it has a progress line. when clicked - sends to delegate that it is need to stop loading.
 2) if progress == 1 is drawn as default:
    Has no progress line, when clicked - sends to delegate that it is need to open attachment.
 3) if downloadState == .cantSend is drawn as error:
    Has no progress line, when clicked - sends to delegate that need to send attachment one more time.
 */
class PSDUploadView: UIButton {
    weak var delegate : PSDUploadViewDelegate?
    /// progress of load, 0...1, where 1 - is full load.
    var progress : CGFloat = 1.0{
        didSet{
            redraw()
        }
        
    }
    var downloadState: messageState = .sent{
        didSet{
            redraw()
        }
    }
    func redraw(){
        if(downloadState == .cantSend){
            drawError()
        } else{
            if(progress == 0.0){
                UIView.performWithoutAnimation {
                    progressLayer.strokeEnd = progress
                }
                drawWithLoad()
            }
            else if(progress == 1.0){
                progressLayer.strokeEnd = progress
                drawDefault()
            }
            else{
                drawWithLoad()
                progressLayer.strokeEnd = progress
            }
        }
    }
    var color: UIColor = .white {
        didSet {
            if color.isDarkColor {
                self.setImage(self.image(for: .selected)?.imageWith(color: .black), for: .selected)
                self.setImage(UIImage.PSDImage(name: "DownloadBlack"), for: .normal)
                progressLayer.strokeColor = UIColor.black.cgColor
                shapeLayer.fillColor = UIColor.black.withAlphaComponent(0.1).cgColor
            }
            else {
                self.setImage(self.image(for: .selected)?.imageWith(color: color), for: .selected)
                self.setImage(UIImage.PSDImage(name: "DownloadWhite"), for: .normal)
                progressLayer.strokeColor = color.cgColor
                shapeLayer.fillColor = UIColor.white.withAlphaComponent(0.1).cgColor
            }
        }
    }
    
    private var bgPath: UIBezierPath!
    private var shapeLayer: CAShapeLayer!
    private var progressLayer: CAShapeLayer!
    override init(frame: CGRect) {
        super.init(frame: frame)
        bgPath = UIBezierPath()
        simpleShape()
        self.addTarget(self, action: #selector(uploadViewPressed(_:)), for: .touchUpInside)
        drawDefault()
        
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    ///Draw upload view with progress, and cancel action
    private func drawWithLoad(){
        let image = UIImage.PSDImage(name: "Close")?.imageWith(color: color)
        self.setImage(image, for: .selected)
        self.isSelected=true
        progressLayer.isHidden = false
    }
    ///Draw upload view default with open action
    private func drawDefault(){
        self.isSelected=false
        progressLayer.isHidden = true
        progressLayer.strokeEnd = CGFloat(0)
    }
    ///Draw upload view as error with retry action
    private func drawError(){
        self.isSelected=true
        progressLayer.isHidden = true
        
        let image = UIImage.PSDImage(name: "Refresh")?.imageWith(color: color)
        self.setImage(image, for: .selected)
    }
    
    @objc private func uploadViewPressed(_ selector: Selector) {
        if downloadState == .cantSend{
            self.progress = 0.0
        }
        else if progress != 1{
            self.delegate?.stopUpload()
        }
        
        
    }
    private func simpleShape() {
        createCirclePath()
        shapeLayer = CAShapeLayer()
        shapeLayer.path = bgPath.cgPath
        shapeLayer.lineWidth = PROGRESS_LINE_WIDTH
        shapeLayer.strokeColor = UIColor.clear.cgColor
        progressLayer = CAShapeLayer()
        progressLayer.path = bgPath.cgPath
        progressLayer.lineWidth = shapeLayer.lineWidth
        progressLayer.lineCap = CAShapeLayerLineCap.round
        progressLayer.fillColor = nil
        progressLayer.strokeEnd = 0.0
        self.layer.addSublayer(shapeLayer)
        self.layer.addSublayer(progressLayer)
    }
    private func createCirclePath() {
        let x = self.frame.width/2
        let y = self.frame.height/2
        let center = CGPoint(x: x, y: y)
        bgPath.addArc(withCenter: center, radius: x, startAngle: -.pi/2, endAngle: .pi*(3/2), clockwise: true)
        bgPath.close()
        
    }
    

}
