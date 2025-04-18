import Foundation
import AudioToolbox
import CoreAudioKit
import libopus
import MobileVLCKit

let bufCount: Int = 3
let OPUS_PLAYER_NOTIFICATION_KEY = "opusPlayerNotification"
@objc class OpusPlayer: NSObject {
    ///The key to store attachment id that is stopping
    static let keyStop = "attachmentIdToStop"
    ///The key to store attachment id that is playing
    static let keyPlay = "attachmentIdToPlay"
    ///The key to store the progress of attachment in OpusPlayer.keyPlay
    static let keyProgress = "playProgress"
    ///The key to store the full time of attachment in OpusPlayer.keyPlay
    static let keyTime = "playTime"
    ///The global OpusPlayer
    @objc static let shared: OpusPlayer = OpusPlayer.init()
    private var progressesMap: [URL: Int32] = [:]
    private let mediaPlayer = VLCMediaPlayer()
    ///current audio file time in milliseconds
    private var fullTime: Int32 = 0 {
        didSet {
            guard fullTime != oldValue else {
                return
            }
            passTimeUpdate()
        }
    }
    private var pUrl: URL?
    private var pId: NSInteger = 0//The id of attachment, using to detect local attachments
    private var progressTimer: Timer?
    private static let startPlayLogName: String = "Audio : start play"
    private static let errorPlayLogName: String = "Audio : Error: Can't open OggOpusFile"
    private var cancellable = Set<AnyHashable>()
    
    override init() {
        super.init()
        mediaPlayer.delegate = self
    }
    
    ///Start new AudioQueue and kill old.
    func play(url: URL, attachmentId: NSInteger){
        if pUrl != url {
            //the new song is start play
            //so stop previous
            stopCurrentPlaying(pause: false)
            if let pUrl = pUrl {
                progressesMap[pUrl] = 0
            }
            //save new attachmentId
            pUrl = url
        }
        prepareToPlay(attachmentId: attachmentId)
        //start playing
        fullTime = timeFile(url: url)
        startPlay(with: getProgress(of: url))
    }
    ///Kill old play, and remember the new one.
    func prepareToPlay(attachmentId: NSInteger){
        //save new attachmentId
        pId = attachmentId
    }
    ///Return true is OpusPlayer now is ready to play attachment with passeed attachmentId
    @objc func readyToPlay(_ attachmentId: NSInteger) -> Bool {
        return pId == attachmentId && pId != 0
    }
    private func startPlay(with offset: Int32) {
        guard let pUrl = pUrl else {
            return
        }
        checkSpeaker()
        if mediaPlayer.media?.url != pUrl {
            let media = VLCMedia(url: pUrl)
            mediaPlayer.media = media
        }
        if
            offset > 0,
            offset < fullTime,
            mediaPlayer.isSeekable
        {
            let time = VLCTime(int: offset)
            mediaPlayer.time = time
        }
        UIApplication.shared.isIdleTimerDisabled = true
        //UIDevice.current.isProximityMonitoringEnabled = true//automatically turn screen off when holding against ear
        createTimer()
        if progressTimer != nil {
            RunLoop.current.add(self.progressTimer!, forMode: .default)
        }
        mediaPlayer.play()
    }
    ///Return true if current OpusPlayer is ready to play same attachmentId
    func hasPlayer(_ url: URL?) -> Bool{
        return url == pUrl
    }
    ///Return true if OpusPlayer now is playing the same attachmentId
    func isPlaying(_ url: URL?) -> Bool {
        guard
            let url = url,
            url == pUrl,
            mediaPlayer.media?.url == url
        else {
          return false
        }
        return mediaPlayer.isPlaying
    }
    
    ///Return progress of play (the time that its playing in seconds) of attachment
    func getProgress(of url: URL?) -> Int32 {
        if url == pUrl {
            if mediaPlayer.time.intValue == 0 && mediaPlayer.position == 1 && isPlaying(url) {
                return fullTime
            }
            if isPlaying(url) {
                if mediaPlayer.time.intValue == 0 && mediaPlayer.position > 0 {
                    return Int32(Float(fullTime) * mediaPlayer.position)
                }
                return mediaPlayer.time.intValue
            }
        }
        let savedProgresss = getSavedProgress(url: url)
        return savedProgresss
    }
    
    ///Return total seconds in audio file
    func getFileTime(_  url: URL?) -> Int32? {
        if url == pUrl {
            return fullTime
        } else if let url = url {
            return timeFile(url: url)
        }
        return nil
    }
    ///Stop current play(if it play) and clean progresses
    @objc func stopAllPlay(){
        stopCurrentPlaying(pause: false)
        progressesMap.removeAll()
    }
    ///Pause all current play(if it play)
    @objc func pauseAllPlay(){
        UIApplication.shared.isIdleTimerDisabled = false
        //UIDevice.current.isProximityMonitoringEnabled = false
        mediaPlayer.pause()
        saveProgress()
        self.progressTimer?.invalidate()
        self.progressTimer = nil
    }
    ///Pause play if now playing the same attachment url as in parameter, and clean info about progress if cleanAfter is true.
    @objc func pausePlay(_ url: URL?, cleanAfter: Bool = false) {
        guard
            let url = url,
            url == pUrl
        else {
            return
        }
        if cleanAfter {
            stopCurrentPlaying(pause: true)
            self.progressesMap[url] = 0
        }else {
            pauseAllPlay()
        }
        passUpdate(to: url, on: cleanAfter)
    }
    
    ///Save current file progress to progressesMap if file doesnt reach the end, if reched - removes it
    private func saveProgress(){
        guard let pUrl = pUrl else {
            return
        }
        var offset = mediaPlayer.time.intValue
        let total = getFileTime(pUrl) ?? 0
        if offset >= total {
            offset = 0
        }
        progressesMap[pUrl] = offset
    }
    ///Return saved file progress from progressesMap or 0 if it was not find
    private func getSavedProgress(url: URL?) -> Int32 {
        guard let url = url else {
            return 0
        }
        return progressesMap[url] ?? 0
    }
    private func createTimer() {
        guard progressTimer == nil else {
            return
        }
        progressTimer = Timer.scheduledTimer(timeInterval: 0.1,
                                             target: self,
                                             selector: #selector(timerFire),
                                             userInfo: nil,
                                             repeats: true)
    }
    
    ///Post notification with info about changed progress
    @objc private func timerFire() {
        passTimeUpdate()
    }
    
    ///Post notification with info about changed progress
    @objc private func passTimeUpdate(){
        guard let pUrl = pUrl else {
            return
        }
        passUpdate(to: pUrl, on: false)
    }
    
    private func timeFile(url: URL) -> Int32 {
        let media = VLCMedia(url: url)
        let length = media.length.intValue
        if length > 0 {
            return length
        }
        guard let nowPlusFive = Calendar.current.date(byAdding: .second,
                                                      value: 5,
                                                      to: Date()) else {
            return length
        }
        let lengthWait = media.lengthWait(until: nowPlusFive).intValue
        if
            lengthWait == 0,
            let url = media.url
        {
            return Int32(oggOpusTimeCalclate(path: url.path) * 1000)
        }
        return lengthWait
    }
    
    private func stopCurrentPlaying(pause: Bool){
        if let url = pUrl {
            if pause {
                mediaPlayer.pause()
                saveProgress()
            } else {
                mediaPlayer.stop()
            }
            UIApplication.shared.isIdleTimerDisabled = false
            //UIDevice.current.isProximityMonitoringEnabled = false
            passUpdate(to: url, on: !pause)
            if
                !pause,
                let pUrl = pUrl
            {
                progressesMap[pUrl] = 0
                mediaPlayer.media = nil
            }
        }
        progressTimer?.invalidate()
        progressTimer = nil
    }
    
    private func passUpdate(to url: URL?, on stop: Bool) {
        guard let url = url else { return }
        let fullTime = url == pUrl ? self.fullTime : timeFile(url: url)
        let progress = stop ? 0 : getProgress(of: url)
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: OPUS_PLAYER_NOTIFICATION_KEY),
                                        object: nil,
                                        userInfo: [OpusPlayer.keyPlay: url, OpusPlayer.keyProgress: progress, OpusPlayer.keyTime: fullTime])
    }
    
    ///Checking AVAudioSession and change output to the earpiece or loud speaker
    func checkSpeaker(){
        let session: AVAudioSession = AVAudioSession.sharedInstance()
    
        if session.category != .playback{
            do {
                try session.setCategory(.playback, mode: .default)
                try session.setActive(true)
            } catch {
                print("Error: cant set playback category")
            }
        }
    }
}

extension OpusPlayer: VLCMediaPlayerDelegate {
    func mediaPlayerStateChanged(_ aNotification: Notification) {
        switch mediaPlayer.state {
        case .ended:
            print("Get notification that player ended")
            stopCurrentPlaying(pause: false)
        default:
            break
        }
    }
    
    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard
            let media = mediaPlayer.media,
            let url = media.url
        else {
            return
        }
        fullTime = timeFile(url: url)
        passTimeUpdate()
    }
}

//MARK: Calculation time
private extension OpusPlayer {
    private static let OpusAudioPlayerSampleRate = Int(AVAudioSession.sharedInstance().sampleRate)
    private func oggOpusTimeCalclate(path: String) -> CGFloat {
        let pcmTotal: Int = Int(self.getPcmTotal(path: path))
        return CGFloat(pcmTotal / OpusPlayer.OpusAudioPlayerSampleRate)
    }
    
    private func getPcmTotal(path: String) -> Int64{
        var error = OPUS_OK
        if let file = op_test_file(strdup(path), &error){
            var pcmTotal: Int64 = 0
            error = op_test_open(file)
            pcmTotal = op_pcm_total(file, -1)
            op_free(file)
            return pcmTotal
        }
        return 0
    }
}
