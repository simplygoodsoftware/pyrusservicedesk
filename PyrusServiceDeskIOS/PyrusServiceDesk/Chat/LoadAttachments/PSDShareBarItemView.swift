
import UIKit
///UIBarButtonItem to share attachment.
//Call prepairToShare(_ data : Data, fileName: String) to prepair file for sharing. In denit this file will be removed
class PSDShareBarItemView: UIBarButtonItem{
    ///The name of attachment.
    private(set) var shareFileUrl : NSURL? = nil{
        didSet{
            redraw()
        }
    }
    deinit{
         if let url = self.shareFileUrl as URL?{
            PSDFilesManager.removeFile(url:url)
        }
        
    }
    override init() {
        super.init()
        self.image = UIImage.PSDImage(name: "Share")
        self.target = self
        self.action = #selector(leftButtonAction)
    }
    @objc func leftButtonAction(){
        self.shareFile()
    }
    private func redraw(){
        if(shareFileUrl != nil){
            self.isEnabled = true
        }
        else{
            self.isEnabled = false
        }
    }
   
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    ///Create file for sharing. (with url - shareFileUrl)
    ///- parameter data: Data to write to file
    ///- parameter fileName: Using to create file name
    func prepairToShare(_ data : Data, fileName: String){
        if(data.count > 0 && fileName.count > 0){
            self.shareFileUrl = data.dataToFile(fileName: fileName, messageLocalId: "0")
        }
    }
    private func shareFile(){
        if(self.shareFileUrl != nil ){
            var filesToShare = [Any]()
            
            filesToShare.append(self.shareFileUrl!)
            
            let activityViewController = UIActivityViewController(activityItems: filesToShare, applicationActivities: nil)
            
            let viewControllerForPresent = UIApplication.topViewController()
            
           
            if let popoverController = activityViewController.popoverPresentationController {
                popoverController.barButtonItem = self
            }
            viewControllerForPresent?.present(activityViewController , animated: true, completion: {() -> Void in
                
            })
        }
        
    }
}
