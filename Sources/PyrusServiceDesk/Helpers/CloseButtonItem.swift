
import UIKit
@objc protocol CloseButtonItemDelegate {
    func closeButtonAction()
}
///Close UIBarButtonItem with "Done" and darkAppColor tintColor. When pressed - pass to its delegate action closeButtonAction().
class CloseButtonItem: UIBarButtonItem {
    convenience init(_ delegate:CloseButtonItemDelegate) {
        self.init(image: nil,
                  style: .plain,
                  target: delegate,
                  action: #selector(delegate.closeButtonAction))
        self.title = "Done".localizedPSD()
        self.tintColor = PyrusServiceDesk.mainController?.customization?.barButtonTintColor ?? UIColor.darkAppColor
        
    }
}
