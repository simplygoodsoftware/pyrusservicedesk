
import Foundation
///Is rotating  PSDDownloadImage
class PSDDownloadView : UIImageView{
     private var isAnimateRotation : Bool = false{
        didSet{
            if(isAnimateRotation){
                startRotation()
            }
        }
    }
    private func startRotation() {
        UIView.animate(withDuration: 1.0, delay: 0.0, options: .curveLinear, animations: {
            [weak self] in
            if(self != nil){
                self!.transform = self!.transform.rotated(by: CGFloat(Double.pi))
            }
        }) { [weak self] finished in
            if(self?.isAnimateRotation ?? false){
                self?.startRotation()
            }
        }
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.image = PSDDownloadImage.image()
    }
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        self.image = PSDDownloadImage.image()
    }
    static let downloadSize : CGFloat = 50.0
    
    override func didMoveToSuperview() {
        if(self.superview == nil){
            isAnimateRotation = false
        }
        else{
            isAnimateRotation = true
        }
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
