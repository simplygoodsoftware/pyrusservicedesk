import Foundation

/**
 Show alert error on viewController if it is exist and now is top most.
 - parameter statusCode : HTTPURLResponse statusCode. Pass nil if error - fundamental networking error. And Alert will show message "no internet connection". Else will show server error.
 - parameter viewController: UIViewController where alert must be presented. Pas nil if dont need to alert error.
*/
func showError(_ statusCode:Int?, on viewController:UIViewController? ){
    guard let viewController = viewController,
          let newTopController = UIApplication.topViewController(),
          newTopController == viewController,
          viewController is PSDChatViewController else {
        return
    }
    showErrorAlert(errorMessage(from: statusCode), on: viewController)
}
private func errorMessage(from statusCode:Int?)->String{
    if(statusCode == nil){
        return "No_Connection".localizedPSD()
    }
    else{
        return "ErrorServerError".localizedPSD()
    }
}

