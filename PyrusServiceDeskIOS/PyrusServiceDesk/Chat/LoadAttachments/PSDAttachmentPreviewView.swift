
import UIKit
import  WebKit
///The view to show preview of Attachment.
///Using WKWebView for this.
///Call drawAttachment(_ url : URL).
class PSDAttachmentPreviewView: UIView {

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .psdLightGray
        self.addSubview(webView)
    }
    ///Show preview of Attachment
    ///- parameter url: The file's url that need to show
    func drawAttachment(_ url : URL){
        webView.loadFileURL(url, allowingReadAccessTo: url)
    }
    var needBackground: Bool = false{
        didSet{
            webView.isOpaque = !needBackground
        }
    }
    deinit {
        webView.stopLoading()
    }
    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if self.superview != nil{
            self.translatesAutoresizingMaskIntoConstraints = false
            self.addZeroConstraint([.bottom,.leading,.trailing])
            if #available(iOS 11.0, *) {
                let guide = self.superview!.safeAreaLayoutGuide
                self.topAnchor.constraint(equalTo: guide.topAnchor).isActive = true
            } else {
                self.topAnchor.constraint(equalTo: self.superview!.topAnchor).isActive = true
            }
        }
        
    }
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    private lazy var webView : WKWebView = {
        let preference = WKPreferences()
        preference.javaScriptEnabled = true
        let configuration = WKWebViewConfiguration()
        configuration.preferences = preference
        let webV = WKWebView(frame: self.bounds, configuration: configuration)
        webV.backgroundColor = UIColor.psdBackground
        webV.autoresizingMask = [.flexibleWidth,.flexibleHeight]
        return webV
    }()
}
