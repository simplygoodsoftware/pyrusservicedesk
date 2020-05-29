
import UIKit

class PSDAvatarImageView: UIImageView {

    var owner : PSDUser?
    override init(frame: CGRect) {
        super.init(frame:frame)
        self.backgroundColor = UIColor.psdLightGray
        self.clipsToBounds = true
        self.contentMode = .scaleAspectFill
        self.layer.cornerRadius = AVATAR_SIZE/2
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
}
