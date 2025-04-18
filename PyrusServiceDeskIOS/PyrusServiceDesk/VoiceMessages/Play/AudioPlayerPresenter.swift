import Foundation
@objc protocol AudioPlayerViewProtocol: AnyObject {
    func changeState(_ state: AudioAttachmentView.AudioState)
    func playingProgress(_ progress: CGFloat)
    func setTime(string: String)
    @objc var state: AudioAttachmentView.AudioState { get }
}

@objc class AudioPlayerPresenter: NSObject{
    weak var view: AudioPlayerViewProtocol?
    private var fileUrl: URL?
    private var attachmentId: NSInteger = 0
    ///The function to pass action. If state is stopped - it's will change to playing and player is starting, else it will be stopped.
    func buttonWasPressed(){
        if view?.state == .playing {
            view?.changeState(.paused)
            self.pausePlay()
        }else{
            view?.changeState(.playing)
            self.startPlay()
        }
    }
    ///Change fileUrl
    func update(fileUrl: URL?, attachmentId: NSInteger){
        self.fileUrl = fileUrl
        self.attachmentId = attachmentId
        self.drawCurrentState()
    }
    init(view: AudioPlayerViewProtocol?, fileUrl: URL?, attachmentId: NSInteger) {
        self.view = view
        self.fileUrl = fileUrl
        self.attachmentId = attachmentId
        super.init()
        self.drawCurrentState()
        
        NotificationCenter.default.addObserver(self, selector: #selector(opusPlayerNotification(notification:)), name: NSNotification.Name(rawValue: OPUS_PLAYER_NOTIFICATION_KEY), object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appClosed), name: UIApplication.willResignActiveNotification, object: nil)
        //this lines may be needed to detect changes in port
        /*NotificationCenter.default.addObserver(self, selector: #selector(changeProximityState), name: UIDevice.proximityStateDidChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(changeProximityState), name: AVAudioSession.routeChangeNotification, object: nil)*/
    }
    ///Pass to OpusPlayer to start play attachment from url
    func startPlay(){
        if self.fileUrl != nil  {
            OpusPlayer.shared.play(url: self.fileUrl!, attachmentId: attachmentId)
        }
    }
    ///Pass to OpusPlayer to start play attachment from url
    func preparePlay(){
        OpusPlayer.shared.prepareToPlay(attachmentId: attachmentId)
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    ///Return true if OpusPlayer now is play attchment from the same url
    private func isPlaying() -> Bool{
        return OpusPlayer.shared.isPlaying(fileUrl)
    }
    ///Return current play progress (in milleseconds) or nil if play was not started
    private func getCurrentPlayProgress() -> Int32? {
        return OpusPlayer.shared.getProgress(of: fileUrl) / TO_SECONDS
    }
    ///Return full time of audio file if it is playing now
    private func getFileTime() -> Int32? {
        return (OpusPlayer.shared.getFileTime(fileUrl) ?? 0) / TO_SECONDS
    }
    ///Pass to OpusPlayer to pause play current attachment
    func pausePlay(){
        OpusPlayer.shared.pausePlay(fileUrl)
    }
    
    @objc private func opusPlayerNotification(notification: Notification){
        if let info = notification.userInfo{
            if let urlToStop = info[OpusPlayer.keyStop] as? URL{
                if urlToStop == fileUrl{
                    drawCurrentState()
                }
            }
            if let urlToPlay = info[OpusPlayer.keyPlay] as? URL{
                if urlToPlay == fileUrl{
                    if
                        var p = info[OpusPlayer.keyProgress] as? Int32,
                        var time = info[OpusPlayer.keyTime] as? Int32
                    {
                        p = p / TO_SECONDS
                        time = time / TO_SECONDS
                        let progress = getPercentProgress(with: p, totalTime: time)
                        if needChangeState() {
                            drawCurrentState()
                        }
                        self.view?.playingProgress(progress)
                        self.changeTime(p)
                    }
                }
            }
        }
    }
    private func needChangeState() -> Bool{
        return (self.view?.state != .playing && self.isPlaying()) || ((self.view?.state != .stopped || self.view?.state != .paused) && !self.isPlaying())
    }
    ///Took Info from OpusPlayer to draw audioAttachmentView state and progress
    func drawCurrentState(){
        let p = getCurrentPlayProgress()
        let isPlaying = self.isPlaying()
        if isPlaying{
            self.view?.changeState(.playing)
            if
                let p = p,
                p > 0
            {
                var dur = getFileTime() ?? 0
                let progress = getPercentProgress(with: p, totalTime: dur)
                self.view?.playingProgress(progress)
                changeTime(p)
            }
        }
        else{
            if
                let p = p,
                p > 0
            {
                var dur = getFileTime() ?? 0
                let progress = getPercentProgress(with: p, totalTime: dur)
                self.view?.changeState(.paused)
                self.view?.playingProgress(progress)
                changeTime(p)
            }
            else{
                self.view?.changeState(.stopped)
            }
        }
    }
    @objc private func appClosed(){
        OpusPlayer.shared.pauseAllPlay()
        self.drawCurrentState()
    }
    ///Pausing all playing
    func cleanUp(){
        OpusPlayer.shared.pauseAllPlay()
    }
    
    private func getPercentProgress(with progress: Int32, totalTime: Int32) -> CGFloat {
        guard totalTime > 0 else {
            return 0
        }
        return CGFloat(progress) / CGFloat(totalTime)
    }
    ///Change time according to progress(seconds that was played)
    private func changeTime(_ progress: Int32){
        var p = roundf(Float(progress))
        let f = roundf(Float(getFileTime() ?? 0))
        if p > f{
            p = f
        }
        let progStr = createTimeString(from: Int(p))
        let fullStr = f > 0 ? createTimeString(from: Int(f)) : DEFAULT_TIME
        let timeString = "\(progStr)/\(fullStr)"
        self.view?.setTime(string: timeString)
    }
    
    func createTimeString(from seconds: Int) -> String {
        let secondsPerHour = 3600
        let secondsPerMinute = 60

        let hour = seconds / secondsPerHour
        let min = (seconds % secondsPerHour) / secondsPerMinute
        let sec = seconds % secondsPerMinute

        let hourString = hour > 0 ? String(format: "%02d:", hour) : ""
        return String(format: "%@%02d:%02d", hourString, min, sec)
    }
}

private let TO_SECONDS: Int32 = 1000
private let DEFAULT_TIME: String = "--:--"

