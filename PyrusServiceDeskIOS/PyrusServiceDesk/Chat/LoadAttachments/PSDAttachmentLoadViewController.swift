
import UIKit

/**
 A view controller with WKWebView to show attachments.
 Needs attachmentId and attachmentName.  attachmentName used to show in navigation title, and attachmentId used for create url for URLRequest.
 */
class PSDAttachmentLoadViewController: UIViewController{
    ///The id of attachment from server. Using To create url for URLRequest.
    private var attachmentId : String =  " "
    ///The name of attachment. Using to create title in navigation bar. And to create file name
    var attachmentName : String =  ""
    
    ///The attachment.
    var attachment : PSDAttachment?
    
    private var oldScrollContentInset : Int? = nil
    override func viewDidLoad() {
        super.viewDidLoad()
        self.design()
        if(attachment != nil && attachment!.serverIdentifer != nil){
            self.attachmentId = attachment!.serverIdentifer!
            self.attachmentName = attachment!.name
            
            self.drawDownload()

        }
        
        let defaultName = PSD_ChatTitle()
        self.title = attachmentName.count>0 ? attachmentName : defaultName
       
        if #available(iOS 11.0, *) {
            self.oldScrollContentInset =  UIScrollView.appearance().contentInsetAdjustmentBehavior.rawValue
            UIScrollView.appearance().contentInsetAdjustmentBehavior = .automatic
        }
    }
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        if #available(iOS 11.0, *) {
            if(oldScrollContentInset != nil && UIScrollView.ContentInsetAdjustmentBehavior.init(rawValue: oldScrollContentInset!) != nil){
                UIScrollView.appearance().contentInsetAdjustmentBehavior = UIScrollView.ContentInsetAdjustmentBehavior.init(rawValue: oldScrollContentInset!)!
            }
        }
    }
    private func drawDownload(){
        errorView.attachmentExtension =  (attachmentName as NSString).pathExtension
        downloadFile()
    }
    deinit {
        downloader.remove()
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }
  
    private func design() {
        self.view.backgroundColor = UIColor.psdBackground
        self.setCloseButton()
        self.setDownloadButton()
    }
    private func setCloseButton()
    {
        self.navigationItem.leftBarButtonItem = CloseButtonItem.init(self)
    }
    ///UIBarButtonItem that open share menu
    lazy var shareBarItem : PSDShareBarItemView = {
        let item = PSDShareBarItemView.init()
        item.tintColor = UIColor.darkAppColor
        return item
    }()
    private func setDownloadButton(){
        self.navigationItem.rightBarButtonItem = shareBarItem
    }
    private lazy var previewView : PSDAttachmentPreviewView  = {
        let view = PSDAttachmentPreviewView.init(frame: self.view.bounds)
        return view
    }()
    private lazy var noConnectionView : PSDNoConnectionView  = {
        let view = PSDNoConnectionView.init(frame: self.view.bounds)
        view.delegate = self
        return view
    }()
    ///The view with error - can't-Load-attachment or attachment-has-no-preview
    lazy var errorView : PSDAttachmentLoadErrorView  = {
        let view = PSDAttachmentLoadErrorView.init(frame: self.view.bounds)
        view.delegate = self
        return view
    }()
    ///The PSDProgressDownloadView view to show when attachment data is loading.
    lazy var downloadView : PSDProgressDownloadView  = {
        let view = PSDProgressDownloadView.init(frame: self.view.bounds)
        view.delegate = self
        return view
    }()
    
    
    
    private func removePreviewView(){
        if self.view.subviews.contains(self.previewView){
            self.previewView.removeFromSuperview()
        }
    }
    private func removeNoConnectionView(){
        if self.view.subviews.contains(self.noConnectionView){
            self.noConnectionView.removeFromSuperview()
        }
    }
    private func removeDownloadView(){
        if self.view.subviews.contains(self.downloadView){
            self.downloadView.removeFromSuperview()
        }
    }
    private func removeErrorView(){
        if self.view.subviews.contains(self.errorView){
            self.errorView.removeFromSuperview()
        }
    }
    ///Add PSDAttachmentPreviewView and remove NoConnectionView, ErrorView and DownloadView
    func addPreviewView(with url:URL){
        removeDownloadView()
        removeNoConnectionView()
        removeErrorView()
        if !self.view.subviews.contains(self.previewView){
            self.view.addSubview(previewView)
        }
        previewView.drawAttachment(url)
    }
    ///Add PSDNoConnectionView and remove PreviewView, ErrorView and DownloadView
    func addNoConnectionView(){
        removePreviewView()
        removeErrorView()
        removeDownloadView()
        if !self.view.subviews.contains(noConnectionView){
            self.view.addSubview(noConnectionView)
        }
    }
    ///Add PSDProgressDownloadView and remove NoConnectionView, ErrorView and PreviewView
    private func addDownloadView(){
        removePreviewView()
        removeNoConnectionView()
        removeErrorView()
        if !self.view.subviews.contains(downloadView){
            self.view.addSubview(downloadView)
        }
    }
    ///Add PSDAttachmentLoadErrorView and remove PreviewView, NoConnectionView  and DownloadView
    func addErrorView(){
        removePreviewView()
        removeNoConnectionView()
        removeDownloadView()
        if !self.view.subviews.contains(errorView){
            self.view.addSubview(errorView)
        }
    }
    ///PSDDownloader to download attachment from server
    lazy var downloader : PSDDownloader  = {
        let loader = PSDDownloader.init()
        loader.delegate = self
        return loader
    }()
    func downloadFile(){
        addDownloadView()
        DispatchQueue.global().async {
            [weak self] in
            let url = PyrusServiceDeskAPI.PSDURL(type: .download, ticketId: self!.attachmentId)
            self?.downloader.expectedSize = self?.attachment?.size ?? 0
            self?.downloader.load(from: url)
        }
    }
}

