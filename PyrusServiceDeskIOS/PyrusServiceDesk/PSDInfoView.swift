import Foundation
///The showed
public class PSDInfoView: UIView {
    public override func removeFromSuperview() {
        super.removeFromSuperview()
        PSDMessagesStorage.pyrusUserDefaults()?.set(true, forKey: PSD_WAS_CLOSE_INFO_KEY)
        if let vc  =  UIApplication.topViewController() as? PSDChatViewController{
            vc.resizeTable()
        }
    }
}

