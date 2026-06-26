//import Foundation
///**
// The view to show audio that can be played.
// Has states: AudioState.stopped, AudioState.playing, AudioState.loading, AudioState.paused
// The default value is stopped (draws with play button, all progresses is hidden)
// When state is changed to playing - show two progress: 1)for show loading progress, and 2)for show playing progress. Time label can be visible.
// When state is changed to loading - show loading progress.
// When state is changed to paused - All progresses is visible. Time is hided.
//AudioAttachmentView is not user interaction enabled to let its holder to control when need to pass action to it. Use action() to let AudioAttachmentView known that it was pressed and need to start/stop player.
// */
//@IBDesignable @objc class AudioAttachmentView: UIView{
//    ///The boolean flag to redraw Audio AttachmentView with bigger size
//    @IBInspectable @objc var bigMode: Bool = false
////    {
////        didSet(oldValue){
////            if bigMode != oldValue {
////                size = bigMode ? AudioAttachmentView.bigSize : AudioAttachmentView.defaultSize
////                redrawBySize()
////            }
////            
////        }
////    }
//    private static let bigSize: CGFloat = 135.0
//    static let defaultSize: CGFloat = 45.0
//    private var size: CGFloat = AudioAttachmentView.defaultSize
//    ///Is need to add timeLabel at bottom
//    @objc var needTime: Bool = false{
//        didSet{
////            self.addTimeLabel()
////            if !needTime {
////                self.timeLabel.removeFromSuperview()
////            }
//        }
//    }
//    ///Is preparing to play attachment and stop other playing
//    @objc func preparePlay(){
//        self.presenter?.preparePlay()
//    }
//    private let lightColor: UIColor = .label.withAlphaComponent(0.1)
//    private let loadProgressColor: UIColor = UIColor.systemGray4
//    private let playProgressColor: UIColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? .systemBlue
//    @objc enum AudioState: Int{
//        case stopped = 1
//        case playing = 2
//        case loading = 3
//        case paused = 4
//    }
//    ///The current display state of audioAttachmentView
//    @objc private(set) var state: AudioState = .stopped {
//        didSet(oldValue){
//            print("old: \(oldValue), new: \(state)")
//            if state == .stopped{
//                self.changePlayProgress(AudioAttachmentView.minProgress)
//                self.changeLoadProgress(AudioAttachmentView.maxProgress)
//                drawStopped()
//            }
//            else if state == .loading{
//                CATransaction.begin()
//                CATransaction.setDisableActions(true)
//                self.changePlayProgress(AudioAttachmentView.minProgress)
//                self.changeLoadProgress(AudioAttachmentView.minProgress)
//                CATransaction.commit()
//                drawLoading()
//            }
//            else if state == .paused {
//                self.changeLoadProgress(AudioAttachmentView.maxProgress)
//                drawPaused()
//            }
//            else{
//                if oldValue != .paused {
//                    self.changePlayProgress(AudioAttachmentView.minProgress)
//                    self.changeLoadProgress(AudioAttachmentView.maxProgress)
//                }
//                drawPlaying()
//            }
//        }
//    }
//    deinit{
//    }
//    override func removeFromSuperview() {
//        super.removeFromSuperview()
//        self.presenter?.cleanUp()
//    }
//    ///Change state of AudioAttachmentView.
//    @objc func changeState(_ state: AudioState){
//        self.state = state
//    }
//    ///Redraw audioAttachmentView with state taken from its attachment's url, if has no url - draw as stopped(default)
//    func drawCurrentState(){
//        self.presenter?.drawCurrentState()
//    }
//    private func drawStopped(){
//        self.playerProgressLayer.isHidden = true
//        self.loadingProgressLayer.isHidden = true
//        self.changeImage()
//        self.timeLabel.isHidden = true
//        self.playerButton.layer.borderWidth = 2.0
//    }
//    private func drawLoading(){
//        loadingProgressLayer.isHidden = false
//        playerProgressLayer.isHidden = true
//        self.playerButton.layer.borderWidth = 0.0
//        changeImage()
//        self.timeLabel.isHidden = true
//
//    }
//    private func drawPaused(){
//        loadingProgressLayer.isHidden = false
//        playerProgressLayer.isHidden = false
//        self.playerButton.layer.borderWidth = 0.0
//        changeImage()
//        self.timeLabel.isHidden = true
//    }
//    private func drawPlaying(){
//        loadingProgressLayer.isHidden = false
//        playerProgressLayer.isHidden = false
//        self.playerButton.layer.borderWidth = 0.0
//       changeImage()
//        self.timeLabel.isHidden = !needTime
//    }
//    private func changeImage(){
//        let imageName = self.state == .playing ? pauseImageName : playImageName
//        let image = UIImage.PSDImage(name: imageName)//init(named: imageName)?.withRenderingMode(.alwaysTemplate)
//        self.playerButton.setImage(image, for: .normal)
//    }
//    private lazy var playerButton: UIButton = {
//        let button = UIButton.init(frame: CGRect.zero)
//        button.backgroundColor = .clear
//        button.imageView?.contentMode = .scaleAspectFit
//        button.layer.borderColor = lightColor.cgColor
//        button.isUserInteractionEnabled = false
//        button.tintColor = .label
//        return button
//    }()
//    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
//        super.traitCollectionDidChange(previousTraitCollection)
//        layer.borderColor = loadProgressColor.cgColor
//        playerButton.layer.borderColor = lightColor.cgColor
//    }
//    private let timeFontSize: CGFloat = 9.0
//    ///The label to show time. Drawing in bottom part and visible only when state == .playing
//    lazy var timeLabel: UILabel = {
//        let label = UILabel.init(frame: CGRect.zero)
//        label.textColor = .label
//        label.text = ""
//        label.textAlignment = .center
//        label.font = UIFont.systemFont(ofSize: timeFontSize)
//        return label
//    }()
//    ///The function to pass action. If state is stopped - it's will change to playing and player is starting, else it will be stopped.
//    @objc func action(){
//        self.presenter?.buttonWasPressed()
//    }
//    override init(frame: CGRect) {
//        super.init(frame: frame)
//        customDraw()
//    }
//    required init?(coder aDecoder: NSCoder) {
//        super.init(coder: aDecoder)
//        customDraw()
//    }
//    ///redraw audioAttachmentView with new data
//    @objc func create(with fileUrl: URL?, attachmentId: String){
//        self.createPresenter(fileUrl, attachmentId: attachmentId)
//    }
//    ///redraw audioAttachmentView with new data
//    @objc func updateAttachment(_ fileUrl: URL?, attachmentId: String){
//        self.presenter?.update(fileUrl: fileUrl, attachmentId: attachmentId)
//    }
//    private func createPresenter(_ fileUrl: URL?, attachmentId: String){
//        if self.presenter == nil {
//            self.presenter = AudioPlayerPresenter.init(view: self,  fileUrl: fileUrl, attachmentId: attachmentId, attachment: PSDAttachment(localPath: "", data: nil, serverIdentifer: ""))
//        }else{
//            self.presenter?.update(fileUrl: fileUrl, attachmentId: attachmentId)
//        }
//        
//    }
//    private var presenter: AudioPlayerPresenter? = nil
//    private var widthConstraint: NSLayoutConstraint?
//    private var heightConstraint: NSLayoutConstraint?
//    private func customDraw(){
//        self.layer.borderColor = loadProgressColor.cgColor
//        self.layer.borderWidth = 1.0
//        
//        self.backgroundColor = .psdBackgroundColor
//        self.addSubview(self.playerButton)
//        self.addTimeLabel()
//        self.playerButton.translatesAutoresizingMaskIntoConstraints = false
//        self.timeLabel.translatesAutoresizingMaskIntoConstraints = false
//        widthConstraint = NSLayoutConstraint.init(item: self.playerButton, attribute: .width, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: size)
//        self.addConstraint(widthConstraint!)
//        
//        heightConstraint = NSLayoutConstraint.init(item: self.playerButton, attribute: .height, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: size)
//        self.addConstraint(heightConstraint!)
//        
//        self.addConstraint(NSLayoutConstraint.init(item: self.playerButton, attribute: .centerX, relatedBy: .equal, toItem: self, attribute: .centerX, multiplier: 1.0, constant: 0))
//        self.addConstraint(NSLayoutConstraint.init(item: self.playerButton, attribute: .centerY, relatedBy: .equal, toItem: self, attribute: .centerY, multiplier: 1.0, constant: 0))
//        self.initProgressLayers()
//        self.loadingProgress = AudioAttachmentView.minProgress
//        self.playingProgress = AudioAttachmentView.minProgress
//        self.state = .stopped
//       // playerButton.layer.cornerRadius = size / 2
//        redrawBySize()
//    }
//    private func redrawBySize(){
//        playerButton.layer.cornerRadius = size / 2
////        heightConstraint?.constant = size
////        widthConstraint?.constant = size
//        loadingProgressLayer.dSize = size
//        playerProgressLayer.dSize = size
//        loadingProgressLayer.lineW = bigMode ? AudioAttachmentView.progressFatLineWidth : AudioAttachmentView.progressLineWidth
//        playerProgressLayer.lineW = bigMode ? AudioAttachmentView.progressFatLineWidth : AudioAttachmentView.progressLineWidth
////        playImageName = bigMode ? bigPlayImageName : playImageName
////        pauseImageName = bigMode ? bigPauseImageName: pauseImageName
////        changeImage()
//    }
//    private var pauseImageName = "pauseIcon"
//    private var playImageName = "playIcon"
//    private var bigPauseImageName = "bigPauseIcon"
//    private var bigPlayImageName = "bigPlayIcon"
//    private func addTimeLabel(){
//        if self.timeLabel.superview == nil && needTime {
//            self.addSubview(self.timeLabel)
//
//            self.addConstraint(NSLayoutConstraint.init(item: self.timeLabel, attribute: .left, relatedBy: .equal, toItem: self, attribute: .left, multiplier: 1.0, constant: 0))
//            self.addConstraint(NSLayoutConstraint.init(item: self.timeLabel, attribute: .right, relatedBy: .equal, toItem: self, attribute: .right, multiplier: 1.0, constant: 0))
//            self.addConstraint(NSLayoutConstraint.init(item: self.timeLabel, attribute: .bottom, relatedBy: .equal, toItem: self, attribute: .bottom, multiplier: 1.0, constant: 0))
//            self.addConstraint(NSLayoutConstraint.init(item: self.timeLabel, attribute: .top, relatedBy: .equal, toItem: self.playerButton, attribute: .bottom, multiplier: 1.0, constant: 0))
//        }
//    }
//    
//    override func didMoveToSuperview() {
//        super.didMoveToSuperview()
//    }
//    ///Change load progress.
//    ///- parameter progress: CGFloat value betwin AudioAttachmentView.minProgress and AudioAttachmentView.maxProgress
//    @objc func changeLoadProgress(_ progress: CGFloat){
//        self.loadingProgress = min(AudioAttachmentView.maxProgress,max(progress, AudioAttachmentView.minProgress))
//        print("load: \(progress)")
//    }
//    private var loadingProgress: CGFloat = AudioAttachmentView.minProgress{
//        didSet{
//            loadingProgressLayer.strokeEnd = loadingProgress
//        }
//    }
//    ///Change playing progress.
//    ///- parameter progress: CGFloat value betwin 0.0 and 1.0
//    @objc func changePlayProgress(_ progress: CGFloat){
//        self.playingProgress = min(AudioAttachmentView.maxProgress,max(progress, AudioAttachmentView.minProgress))
//        print("play: \(progress)")
//    }
//    private var playingProgress: CGFloat = AudioAttachmentView.minProgress{
//        didSet{
//            playerProgressLayer.strokeEnd = playingProgress
//        }
//    }
//    private static let minProgress: CGFloat = 0.0
//    private static let maxProgress: CGFloat = 1.0
//    private static let progressLineWidth: CGFloat = 4.0
//    private static let progressFatLineWidth: CGFloat = 12.0
//    private var loadingProgressLayer: CircularProgress!
//    private var playerProgressLayer: CircularProgress!
//    private func initProgressLayers(){
//        
//        loadingProgressLayer = circleProgress(lineColor: loadProgressColor, backColor: lightColor)
//        loadingProgressLayer.addToLayer(playerButton.layer)
//        
//        playerProgressLayer = circleProgress(lineColor: playProgressColor, backColor: UIColor.clear)
//        playerProgressLayer.addToLayer(playerButton.layer)
//        
//    }
//    ///Add CAShapeLayer as circle
//    ///- parameter lineWidth: width of circular line
//    ///- parameter lineFillColor: the fillColor
//    ///- parameter lineColor: the strokeColor
//    private func circleProgress(lineColor: UIColor,  backColor: UIColor) -> CircularProgress{
//
//        let progressLayer = CircularProgress.init(lineColor: lineColor, lineWidth: AudioAttachmentView.progressLineWidth, fillLineColor: backColor, size: size)
//        
//        return progressLayer
//    }
//}
//
