import Foundation
class CircularProgress: CAShapeLayer{
    ///Used to show circular progress. It consists of two circular lines: the first one shows the progress itself, and the second is the backs for it.
    ///- parameter lineColor: The color of main line to show the progress,
    ///- parameter lineWidth: width of both of lines
    ///- parameter fillLineColor: The color for back line of progress line
    ///- parameter size: The diameter
    init(lineColor: UIColor, lineWidth: CGFloat, fillLineColor: UIColor?, size: CGFloat) {
        self.dSize = size
        self.lineColor = lineColor
        self.lineW = lineWidth
        self.fillLineColor = fillLineColor
        super.init()
        
        backLayer.lineWidth = lineWidth
        backLayer.strokeColor = fillLineColor?.cgColor
        backLayer.fillColor = nil
        backLayer.lineCap = CAShapeLayerLineCap.round
        backLayer.strokeEnd = 1.0
        
        self.lineWidth = lineWidth
        self.strokeColor = lineColor.cgColor
        self.fillColor = nil
        self.lineCap = CAShapeLayerLineCap.round
        self.strokeEnd = 0.0
    }
    ///Add layer with its back to superlayer
    func addToLayer(_ sLayer: CALayer){
        sLayer.addSublayer(backLayer)
        sLayer.addSublayer(self)
    }
    override init(layer: Any) {
        self.dSize = CircularProgress.default_dSize
        self.lineColor = CircularProgress.default_lineColor
        self.lineW = CircularProgress.default_lineW
        self.fillLineColor = nil
        super.init(layer: layer)
    }
    private static let default_dSize: CGFloat = 10.0
    private static let default_lineColor = UIColor.black
    private static let default_lineW: CGFloat = 1.0
    required init?(coder aDecoder: NSCoder) {
        self.dSize = CircularProgress.default_dSize
        self.lineColor = CircularProgress.default_lineColor
        self.lineW = CircularProgress.default_lineW
        self.fillLineColor = nil
        super.init(coder: aDecoder)
    }
    var dSize: CGFloat {
        didSet{
            changePath()
        }
    }
    var lineColor: UIColor {
        didSet{
            self.strokeColor = lineColor.cgColor
        }
    }
    var lineW: CGFloat {
        didSet{
            self.lineWidth = lineW
            backLayer.lineWidth = lineW
        }
    }
    var fillLineColor: UIColor? {
        didSet{
            backLayer.strokeColor = fillLineColor?.cgColor
        }
    }
    private static let startCircleAngle: CGFloat = -.pi / 2
    private static let endCircleAngle: CGFloat = .pi * (3 / 2)
    private static let circleClockwise: Bool = true//The direction in which to draw the arc.
    private func changePath(){
        let bgPath = UIBezierPath()
        let x: CGFloat = dSize / 2
        let y: CGFloat = dSize / 2
        let center = CGPoint(x: x, y: y)
        bgPath.addArc(withCenter: center, radius: x - (lineWidth / 2), startAngle: CircularProgress.startCircleAngle, endAngle: CircularProgress.endCircleAngle, clockwise: CircularProgress.circleClockwise)
        bgPath.close()
        
        backLayer.path = bgPath.cgPath
        self.path = bgPath.cgPath
    }
    private var backLayer: CAShapeLayer = CAShapeLayer.init()
    override var isHidden: Bool{
        didSet{
            backLayer.isHidden = isHidden
        }
    }
    override func removeFromSuperlayer() {
        super.removeFromSuperlayer()
        backLayer.removeFromSuperlayer()
    }
}
