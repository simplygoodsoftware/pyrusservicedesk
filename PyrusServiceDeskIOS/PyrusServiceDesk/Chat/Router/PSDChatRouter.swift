import Foundation

final class PSDChatRouter: NSObject {
    weak var controller: PSDChatViewController?
}

extension PSDChatRouter: PSDChatRouterProtocol {
    func route(to destination: PSDChatRouterDestination) {
        switch destination {
        case .showLinkOpenAlert(linkString: let linkString):
            showLinkOpenAlert(linkString: linkString)
        case .close:
            close()
        }
    }
}

private extension PSDChatRouter {
    func showLinkOpenAlert(linkString: String) {
        guard let url = URL(string: linkString) else { return }
        
        let message = String(format: "ExternalSourceWarning".localizedPSD(), linkString)
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "ShortNo".localizedPSD(), style: .cancel))
        alert.addAction(
            UIAlertAction(title: "ShortYes".localizedPSD(), style: .default, handler: { _ in
                if #available(iOS 10.0, *) {
                    UIApplication.shared.open(url)
                } else {
                    UIApplication.shared.openURL(url)
                }
            })
        )
        controller?.present(alert, animated: true, completion: nil)
    }
    
    func close() {
        guard !PyrusServiceDesk.multichats else {
            controller?.navigationController?.popViewController(animated: true)
            return
        }
        
        if let mainController = PyrusServiceDesk.mainController {
            mainController.remove()//with quick opening - closing can be nil
        } else if let navigationController = controller?.navigationController as? PyrusServiceDeskController {
            navigationController.remove()
        } else {
            CATransaction.begin()
            CATransaction.setCompletionBlock({
                PyrusServiceDesk.stopCallback?.onStop()
                PyrusServiceDeskController.clean()
            })
            controller?.navigationController?.popViewController(animated: true)
            CATransaction.commit()
        }
    }
}
