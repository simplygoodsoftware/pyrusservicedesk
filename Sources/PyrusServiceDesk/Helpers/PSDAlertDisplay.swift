
import Foundation
    /**
     Showing alert Access_Error with actions: cancel and open application settings
     - Parameter alertMessage: String to show in Alert.
     - Parameter viewController: UIViewController where alert must be presented
     */
    func showAccessAlert(with alertMessage: String, on viewController:UIViewController){
        let accessAlertController = UIAlertController (title: "Access_Error".localizedPSD() , message: alertMessage, preferredStyle: .alert)
        let settingsAction = UIAlertAction(title: "Settings".localizedPSD(), style: .destructive) { (_) -> Void in
            let settingsUrl = NSURL(string:UIApplication.openSettingsURLString)
            if let url = settingsUrl {
                UIApplication.shared.openURL(url as URL)
            }
        }
        let cancelAction = UIAlertAction(title: "Cancel".localizedPSD(), style: .default, handler: nil)
        accessAlertController .addAction(cancelAction)
        accessAlertController .addAction(settingsAction)
        present(accessAlertController, on: viewController, sourseView: nil)
    }
    /**
     Showing alert menu with actions and cancel button (without any title or message)
     - Parameter actions: Actions to show in alert menu
     - Parameter viewController: UIViewController where alert must be presented
     */
func showMenuAlert(_ actions:[UIAlertAction], on viewController: UIViewController, sourseView: UIView?){
        let actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        for action in actions{
            actionSheet.addAction(action)
        }
        actionSheet.addAction(UIAlertAction(title: "Cancel".localizedPSD(), style: .cancel, handler: nil))
        present(actionSheet, on: viewController, sourseView: sourseView)
    }
    /**
     Showing alert with error title and message and cancel button
     - Parameter alertMessage: Message to show in alert menu
     - Parameter viewController: UIViewController where alert must be presented
     */
    func showErrorAlert(_ alertMessage:String, on viewController:UIViewController?){
        if(viewController != nil){
            let accessAlertController = UIAlertController (title: "Error".localizedPSD() , message: alertMessage, preferredStyle: .alert)
            let cancelAction = UIAlertAction(title: "Cancel".localizedPSD(), style: .default, handler: nil)
            accessAlertController .addAction(cancelAction)
            present(accessAlertController, on: viewController!, sourseView: nil)
        }
        
    }
    private func present(_ alert: UIAlertController , on viewController:UIViewController, sourseView: UIView?) {
        var viewControllerForPresent = viewController
        if(viewController.navigationController?.parent != nil){
            viewControllerForPresent = (viewController.navigationController?.parent!)! 
        }
        if let popoverController = alert.popoverPresentationController {
            if sourseView != nil{
                popoverController.sourceView = sourseView
                popoverController.sourceRect = sourseView!.bounds
            }else{
                popoverController.sourceView = viewControllerForPresent.view
                popoverController.sourceRect=viewControllerForPresent.view.bounds
            }
            popoverController.permittedArrowDirections = []
        }
        CustomizationHelper.prepareWithCustomizationAlert(alert)
        viewControllerForPresent.present(alert , animated: true, completion: {() -> Void in
                
        })
        
        
    }

