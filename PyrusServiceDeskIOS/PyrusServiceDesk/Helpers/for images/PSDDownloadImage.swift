
import Foundation
let PROGRESS_LINE_WIDTH : CGFloat = 2.0
///Black download image (not full circle)
class PSDDownloadImage{
    static func image(color: UIColor) -> UIImage {
        guard color.isDarkColor else {
                return imageWhite
        }
        return imageBlack
    }
    private static let imageWhite = createImage(.white)
    private static let imageBlack = createImage(.black)
    
    ///Create image (open black circle line)
    private static func createImage(_ color: UIColor)-> UIImage{
        let view = UIView.init(frame: CGRect(x: 0, y: 0, width: PSDDownloadView.downloadSize, height: PSDDownloadView.downloadSize))
        let bgPath: UIBezierPath = UIBezierPath()
        let x = view.frame.width/2
        let y = view.frame.height/2
        let center = CGPoint(x: x, y: y)
        bgPath.addArc(withCenter: center, radius: x-2, startAngle: -.pi/2, endAngle: .pi*(3/2), clockwise: true)
        bgPath.close()
        
        let shapeLayer: CAShapeLayer = CAShapeLayer()
        let progressLayer: CAShapeLayer = CAShapeLayer()
        shapeLayer.path = bgPath.cgPath
        shapeLayer.lineWidth = PROGRESS_LINE_WIDTH
        shapeLayer.strokeColor = UIColor.clear.cgColor
        progressLayer.path = bgPath.cgPath
        progressLayer.lineWidth = shapeLayer.lineWidth
        progressLayer.lineCap = CAShapeLayerLineCap.round
        progressLayer.fillColor = nil
        progressLayer.strokeEnd = 0.0
        
        progressLayer.strokeColor = color.cgColor
        shapeLayer.fillColor = nil
        
        view.layer.addSublayer(shapeLayer)
        view.layer.addSublayer(progressLayer)
        
        progressLayer.strokeEnd = CGFloat(0.8)
        view.backgroundColor = .clear
        return view.asImage()
    }
}

