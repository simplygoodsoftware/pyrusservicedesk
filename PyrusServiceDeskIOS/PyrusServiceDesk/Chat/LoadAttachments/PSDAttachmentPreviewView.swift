
import UIKit
import  WebKit
import AVKit
///The view to show preview of Attachment.
///Using WKWebView for this.
///Call drawAttachment(_ url : URL).
class PSDAttachmentPreviewView: UIView {

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdLightGray
    }
    ///Show preview of Attachment
    ///- parameter url: The file's url that need to show
    func drawAttachment(_ url : URL){
        if url.absoluteString.isVideoFormat(){
            showVideo(url: url)
        }else{
            showWebView(url: url)
        }
    }
    var needBackground: Bool = false{
        didSet{
            webView?.isOpaque = !needBackground
        }
    }
    deinit {
        webView?.stopLoading()
        videoPlayer?.player?.pause()
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
    private var webView: WKWebView?
    private var videoPlayer: AVPlayerViewController?
    private var player: AVPlayer?
    private func showVideo(url: URL){
        player = AVPlayer.init(url: url)
        videoPlayer = AVPlayerViewController()
            
        if let videoPlayer = videoPlayer{
            videoPlayer.view.frame = frame
            videoPlayer.videoGravity = .resizeAspect
            addSubview(videoPlayer.view)
        }
        DispatchQueue.main.async() {
            self.videoPlayer?.player = self.player
        }
    }
    private func showWebView(url: URL){
        if webView == nil{
            let preference = WKPreferences()
            preference.javaScriptEnabled = true
            let configuration = WKWebViewConfiguration()
            configuration.preferences = preference
            let webV = WKWebView(frame: self.bounds, configuration: configuration)
            webV.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? UIColor.psdBackground
            webV.autoresizingMask = [.flexibleWidth,.flexibleHeight]
            webView = webV
            webView?.isOpaque = !needBackground
            self.addSubview(webV)
        }
        webView?.loadFileURL(url, allowingReadAccessTo: url)
    }
}
