import Foundation

/**
 Show alert error on viewController if it is exist and now is top most.
 - parameter statusCode : HTTPURLResponse statusCode. Pass nil if error - fundamental networking error. And Alert will show message "no internet connection". Else will show server error.
 - parameter viewController: UIViewController where alert must be presented. Pas nil if dont need to alert error.
*/
func showError(_ statusCode:Int?, on viewController:UIViewController? ){
    if viewController != nil{//is viewcontroller exist?
        if let newTopController = UIApplication.topViewController() {
            if(newTopController == viewController){
                if(isPSD(viewController: viewController!))
                {
                    showErrorAlert(errorMessage(from: statusCode), on: viewController!)
                }
            }
        }
    }
}
///Cheking if current view controller is part of framework
private func isPSD(viewController:UIViewController)->Bool{
    let PSDControllersList : [UIViewController.Type] = [PSDChatsViewController.self, PSDChatViewController.self]
    for vc in  PSDControllersList{
        if viewController.isKind(of: vc) {
            return true
        }
    }
    return false
}
private func errorMessage(from statusCode:Int?)->String{
    if(statusCode == nil){
        return "No_Connection".localizedPSD()
    }
    else{
        return "ErrorServerError".localizedPSD()
    }
}

