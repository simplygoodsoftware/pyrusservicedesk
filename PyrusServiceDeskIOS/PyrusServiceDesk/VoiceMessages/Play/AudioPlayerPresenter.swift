//
//  AudioPlayerPresenter.swift
//  Papirus
//
//  Created by  Галина Муравьева on 26/06/2019.
//

import Foundation

@objc enum AudioState: Int{
    case stopped = 1
    case playing = 2
    case loading = 3
    case paused = 4
    case needLoad = 5
}

@objc protocol AudioPlayerViewProtocol: class {
    func changeState(_ state: AudioState)
    func playingProgress(_ progress: CGFloat)
    func setTime(string: String)
    func changeLoadingProggress(_ progress: Float)
    @objc var state: AudioState { get }
    var slider: UISlider { get }
}

@objc class AudioPlayerPresenter: NSObject{
    weak var view: AudioPlayerViewProtocol?
    private var fileUrl: URL?
    private var attachmentId: String = ""
    private var attachmentName: String = ""
    
    private let audioRepository = AudioRepository()
    
    ///The function to pass action. If state is stopped - it's will change to playing and player is starting, else it will be stopped.
    func buttonWasPressed() {
        if view?.state == .needLoad {
            view?.changeState(.loading)
            loadAudio()
        } else if view?.state == .loading {
            view?.changeState(.needLoad)
        } else if view?.state == .playing {
            view?.changeState(.paused)
            self.pausePlay()
        } else {
            view?.changeState(.playing)
            self.startPlay(progress: view?.slider.value)
        }
    }
    
    private func loadAudio() {
        view?.changeLoadingProggress(0.5)
        let url = PyrusServiceDeskAPI.PSDURL(type: .download, ticketId: attachmentId)
        PyrusServiceDesk.mainSession.dataTask(with: url) { [weak self] data, response, error in
            guard let self else { return }
            if let data, data.count != 0 {
                do {
                    try audioRepository?.saveAudio(data, name: attachmentName, id: attachmentId)
                    view?.changeLoadingProggress(1)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
                        self?.view?.changeState(.stopped)
                    }
                    
                } catch {
                    view?.changeState(.needLoad)
                }
                
            }
        }.resume()
    }
    ///Change fileUrl
    func update(fileUrl: URL?, attachmentId: String){
        self.fileUrl = fileUrl
        self.attachmentId = attachmentId
        self.drawCurrentState()
    }
    init(view: AudioPlayerViewProtocol?, fileUrl: URL?, attachmentId: String, attachment: PSDAttachment) {
        self.view = view
        self.fileUrl = fileUrl
        self.attachmentId = attachmentId
        self.attachmentName = attachment.name
        super.init()
       // self.drawCurrentState()
        self.changeTime(0)
        
        NotificationCenter.default.addObserver(self, selector: #selector(opusPlayerNotification(notification:)), name: NSNotification.Name(rawValue: OPUS_PLAYER_NOTIFICATION_KEY), object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appClosed), name: UIApplication.willResignActiveNotification, object: nil)
        //this lines may be needed to detect changes in port
        /*NotificationCenter.default.addObserver(self, selector: #selector(changeProximityState), name: UIDevice.proximityStateDidChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(changeProximityState), name: AVAudioSession.routeChangeNotification, object: nil)*/
    }
    ///Pass to OpusPlayer to start play attachment from url
    func startPlay(progress: Float? = nil) {
        var playingProgress: Int64?
        if self.fileUrl != nil  {
            if let progress, let dur = getFileTime() {
                playingProgress = Int64(CGFloat(progress) * CGFloat(AudioFormatManager.getPcmTotal(path: fileUrl!.path)))
            }
            OpusPlayer.shared.play(url: self.fileUrl!, attachmentId: attachmentId, progress: playingProgress)
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
    private func getCurrentPlayProgress() -> CGFloat?{
        return OpusPlayer.shared.getProgress(of: fileUrl)
    }
    ///Return full time of audio file if it is playing now
    private func getFileTime() -> CGFloat?{
        return OpusPlayer.shared.getFileTime(fileUrl)
    }
    ///Pass to OpusPlayer to pause play current attachment
    func pausePlay(){
        OpusPlayer.shared.pausePlay(fileUrl)
    }
    /*
    @objc private func changeProximityState(){///this is need to lock screen when phone is next to head
        OpusPlayer.shared.checkSpeaker()
    }*/
    @objc private func opusPlayerNotification(notification: Notification){
        if let info = notification.userInfo{
            if let urlToStop = info[OpusPlayer.keyStop] as? URL{
                if urlToStop == fileUrl{
                    drawCurrentState()
                }
            }
            if let urlToPlay = info[OpusPlayer.keyPlay] as? URL{
                if urlToPlay == fileUrl{
                    if let progress = info[OpusPlayer.keyProgress] as? CGFloat{
                        if needChangeState(){
                            drawCurrentState()
                        }
                        self.view?.playingProgress(progress)// / (getFileTime() ?? 0.0))
                        self.changeTime(progress)
                    }
                }
            }
        }
    }
    
    private func needChangeState() -> Bool{
        return (self.view?.state != .playing && self.isPlaying()) ||
        ((self.view?.state != .stopped || self.view?.state != .paused) && !self.isPlaying())
    }
    ///Took Info from OpusPlayer to draw audioAttachmentView state and progress
    func drawCurrentState() {
        let p = getCurrentPlayProgress()
        let isPlaying = self.isPlaying()
        if isPlaying{
            self.view?.changeState(.playing)
            if p != nil && p! > 0 {
                let dur = self.getFileTime() ?? 0
                let progress = p!/dur
                self.view?.playingProgress(p ?? 0)
                changeTime(p!)
            }
        }
        else{
            if p != nil && p! > 0 {
                let dur = getFileTime() ?? 0
                let progress = p!/dur
                self.view?.changeState(.paused)
                //self.view?.playingProgress(p!)
//                changeTime(p!)
            }
            else{
                if self.view?.state != .loading && self.view?.state != .needLoad {
                    self.view?.changeState(.stopped)
                }
               // changeTime(0)
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
    ///Change time according to progress(seconds that was played)
    func changeTime(_ progress: CGFloat){
        var p = roundf(Float(progress) * Float(getFileTime() ?? 0))
        let f = roundf(Float(getFileTime() ?? 0))
        if p > f{
            p = f
        }
        let progStr = createTimeString(from: Int(f - p))
        let fullStr = createTimeString(from: Int(f))
        let timeString = "\(progStr)"//\(fullStr)"
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
