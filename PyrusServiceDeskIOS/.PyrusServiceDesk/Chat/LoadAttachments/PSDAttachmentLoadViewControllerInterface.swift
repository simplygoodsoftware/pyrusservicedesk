
import Foundation
//MARK: CloseButtonItemDelegate
extension PSDAttachmentLoadViewController : CloseButtonItemDelegate{
    @objc func closeButtonAction(){
        self.dismiss(animated: true, completion: nil)
    }
}
//MARK: PSDProgressDownloadViewDelegate
extension PSDAttachmentLoadViewController : PSDProgressDownloadViewDelegate{
    func cancelPressed(){
        downloader.cancel()
    }
    
}
//MARK: PSDNoConnectionViewDelegate && PSDAttachmentLoadErrorViewDelegate
extension PSDAttachmentLoadViewController : PSDNoConnectionViewDelegate,PSDAttachmentLoadErrorViewDelegate{
    func retryPressed(){
        downloadFile()
    }
    func openPressed(){
        if let url = self.shareBarItem.shareFileUrl as URL?{
            let doc : UIDocumentInteractionController =  UIDocumentInteractionController.init(url: url)
            doc.presentOpenInMenu(from:  self.errorView.button.frame, in: self.errorView, animated: true)
        }
    }
}
//MARK: PSDDownloaderDelegate
extension PSDAttachmentLoadViewController : PSDDownloaderDelegate{
    func changeProgress(to newValue:CGFloat){
        downloadView.progress = newValue
    }
    func showNoConnection(){
        addNoConnectionView()
    }
    func ended(with data: Data?){
        if(data != nil && (data?.count ?? 0) > 0){
            self.shareBarItem.prepairToShare(data!, fileName: self.attachmentName)
            
            if(attachment?.canOpen ?? false){
                if let url = self.shareBarItem.shareFileUrl as URL?{
                    addPreviewView(with:  url)
                }
            }
            else{
                errorView.state = .noPreview
                addErrorView()
            }
            
        }
        else{
            errorView.state = .cantLoad
            addErrorView()
        }
    }
}
