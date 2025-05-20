import Foundation
import AudioToolbox
import CoreAudioKit
import libopus

let bufCount: Int = 3
let OPUS_PLAYER_NOTIFICATION_KEY = "opusPlayerNotification"
@objc class OpusPlayer: NSObject {
    ///The key to store attachment id that is stopping
    static let keyStop = "attachmentIdToStop"
    ///The key to store attachment id that is playing
    static let keyPlay = "attachmentIdToPlay"
    ///The key to store the progress of attachment in OpusPlayer.keyPlay
    static let keyProgress = "playProgress"
    ///The global OpusPlayer
    @objc static let shared: OpusPlayer = OpusPlayer.init()
    private var progressesMap: [URL: Int64] = [:]
    private var decoder: AudioDecoderProtocol?
//    private var decoder2: VorbisDecoder?
    private var pQueue: AudioQueueRef? = nil
    private var timeLine: AudioQueueTimelineRef!
    private var stopping: Bool = false
    private var paused: Bool = false
    private var fullTime: CGFloat = 0///current audio file time in milliseconds
    private var mBuffers = [AudioQueueBufferRef?](repeating: nil, count: bufCount)
    private var pUrl: URL?
    private var pId: String = ""//The id of attachment, using to detect local attachments
    static let OpusAudioPlayerSampleRate = Int(AVAudioSession.sharedInstance().sampleRate)
    
    private var isPausedTapped: Bool = false
    
    private var progressTimer: Timer?
    private static let startPlayLogName: String = "Audio : start play"
    private static let errorPlayLogName: String = "Audio : Error: Can't open OggOpusFile"
    
    ///Start new AudioQueue and kill old.
    func play(url: URL, attachmentId: String, progress: Int64?) {
      //  if pUrl != url{
            //the new song is start play
            //so stop previous
            if self.pQueue != nil{
                stopCurrentPlaying(needPassStop: pUrl != url)
            }
            //save new attachmentId
            pUrl = url
     //   }
        prepareToPlay(attachmentId: attachmentId)
        //start playing
        if pQueue == nil{
            if let audioDecoder = AudioFormatManager.getDecoder(url: url, offSet: progress ??  getSavedProgress(url: url))  {
                decoder = audioDecoder
                fullTime = timeFile(path: url.path)
//                decoder = OggOpusDecoder.init(url: url, offset: getSavedProgress(url: url))
                prepareToPlay()
                if pQueue != nil{
                    AudioQueuePrime(pQueue!, UInt32(bufCount), nil)
                    startPlay()
                    passProgress()
                }
                else{
                    print("Error create pQueue")
                }
            }
            else{
                decoder = nil
                passStopped(to: url)
            }
        }else{
            startPlay()
            passProgress()
        }
    }
    ///Kill old play, and remember the new one.
    func prepareToPlay(attachmentId: String){
        //save new attachmentId
        pId = attachmentId
    }
    ///Return true is OpusPlayer now is ready to play attachment with passeed attachmentId
    @objc func readyToPlay(_ attachmentId: String) -> Bool{
        return pId == attachmentId && pId != ""
    }
    private func startPlay(){
        if pQueue != nil {
            paused = false
            checkSpeaker()
            createTimer()
            if progressTimer != nil {
                RunLoop.current.add(self.progressTimer!, forMode: .default)
            }
            AudioQueueStart(pQueue!, nil)
            UIApplication.shared.isIdleTimerDisabled = true
            //UIDevice.current.isProximityMonitoringEnabled = true//automatically turn screen off when holding against ear

        }
        
    }
    ///Return true if current OpusPlayer is ready to play same attachmentId
    func hasPlayer(_ url: URL?) -> Bool{
        return url == pUrl
    }
    ///Return true if OpusPlayer now is playing the same attachmentId
    func isPlaying(_ url: URL?) -> Bool{
        if pQueue != nil && url == pUrl {
            var isRunning: UInt32 = 0
            var formatSize = UInt32(MemoryLayout<UInt32>.stride)
            AudioQueueGetProperty(pQueue!, kAudioQueueProperty_IsRunning, &isRunning, &formatSize)
            
            return isRunning > 0 && !paused
        }
        return false
    }
    ///Return progress of play (the time that its playing in seconds) of attachment
    func getProgress(of url: URL?) -> CGFloat{
        if pQueue != nil && url == pUrl && self.decoder != nil{
            let psmOffset = self.decoder?.psmOffset() ?? 0
            let psmTotal = self.decoder?.getPcmTotal() ?? 0
            print("psmOffset: \(CGFloat(psmOffset)/CGFloat(psmTotal)), psmTotal: \(psmTotal)")
            let curSecProgress = CGFloat(psmOffset) / CGFloat(OpusPlayer.OpusAudioPlayerSampleRate)
           
            return CGFloat(psmOffset) / CGFloat(psmTotal)//curSecProgress
        }
        else{
            let savedProgresss = getSavedProgress(url: url)
            let psmTotal = self.decoder?.getPcmTotal() ?? 0
            let startedProgress = CGFloat(savedProgresss) / CGFloat(psmTotal)// CGFloat(OpusPlayer.OpusAudioPlayerSampleRate)
             return startedProgress
        }
       
    }
    ///Return total seconds in audio file
    func getFileTime(_  url: URL?) -> CGFloat?{
        if url == pUrl{
            return self.fullTime
        } else if url != nil{
            return timeFile(path: url!.path)
        }
        return nil
    }
    ///Stop current play(if it play) and clean progresses
    @objc func stopAllPlay(){
        if pQueue != nil {
            stopCurrentPlaying()
            progressesMap.removeAll()
        }
    }
    ///Pause all current play(if it play)
    @objc func pauseAllPlay(){
        if pQueue != nil{
            saveProgress()
            
            UIApplication.shared.isIdleTimerDisabled = false
            //UIDevice.current.isProximityMonitoringEnabled = false
            AudioQueuePause(pQueue!)
            paused = true
            
            self.progressTimer?.invalidate()
            self.progressTimer = nil
            passProgress()
        }
    }
    ///Pause play if now playing the same attachment url as in parameter, and clean info about progress if cleanAfter is true.
    @objc func pausePlay(_ url: URL?, cleanAfter: Bool = false){
        if pQueue != nil && url == pUrl{
            if cleanAfter{
                stopCurrentPlaying()
                self.progressesMap[pUrl!] = 0
            }else{
                //pauseAllPlay()
                isPausedTapped = true
            }
            passStopped(to: pUrl!)
        }
    }
    
    func getTotalProgress() -> Int64 {
        return decoder?.getPcmTotal() ?? 0
    }
    
    ///Save current file progress to progressesMap if file doesnt reach the end, if reched - removes it
    private func saveProgress(){
        if pUrl != nil {
            var psmOffset: Int64 = self.decoder?.psmOffset() ?? 0
            let totalPcm = decoder?.getPcmTotal() ?? 0
            print("opa \(CGFloat(CGFloat(psmOffset)/CGFloat(totalPcm)))")
            if pUrl != nil && psmOffset >= totalPcm {
                psmOffset = 0
            }
            self.progressesMap[pUrl!] = psmOffset
        }
    }
    ///Return saved file progress from progressesMap or 0 if it was not find
    private func getSavedProgress(url: URL?) -> Int64{
        if url != nil {
             return self.progressesMap[url!] ?? 0
        }
        return 0
    }
    private func createTimer() {
        if progressTimer == nil {
            let timer = Timer(timeInterval: 0.01,
                              target: self,
                              selector: #selector(passProgress),
                              userInfo: nil,
                              repeats: true)
            
            RunLoop.current.add(timer, forMode: .common)
            
            progressTimer = timer
            passProgress()
        }
    }
    ///Post notification with info about changed progress
    @objc private func passProgress(){
        if pUrl == nil {
            return
        }
        let progress = self.getProgress(of: pUrl)
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: OPUS_PLAYER_NOTIFICATION_KEY), object: nil, userInfo: [OpusPlayer.keyPlay : pUrl!, OpusPlayer.keyProgress : progress])
    }
    
    private func timeFile(path: String) -> CGFloat{
        let pcmTotal: Int = Int(AudioFormatManager.getPcmTotal(path: path))
        return CGFloat(pcmTotal / OpusPlayer.OpusAudioPlayerSampleRate)
    }
    
    private static let bufferSize: UInt32 = 4096
    private func prepareToPlay(){
        let numberOfChanels: UInt32 = UInt32(decoder?.chanelsCount() ?? 1)
        let kBitsPerChannel: UInt32 = 16
        var dataFormat = AudioStreamBasicDescription(
            mSampleRate: Float64(OpusPlayer.OpusAudioPlayerSampleRate),
            mFormatID: kAudioFormatLinearPCM,
            mFormatFlags: kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked,
            mBytesPerPacket: 2 * numberOfChanels,
            mFramesPerPacket: 1,
            mBytesPerFrame: 2 * numberOfChanels,
            mChannelsPerFrame: numberOfChanels,
            mBitsPerChannel: kBitsPerChannel,
            mReserved: 0)
        stopping = false
        paused = false
        let selfPointer = UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque())//passRetained
        
        var status = AudioQueueNewOutput(&dataFormat, self.opusPlayerOutputCallback, selfPointer, CFRunLoopGetCurrent(), CFRunLoopMode.commonModes.rawValue, 0, &pQueue)
        assert(status == 0, "Audio queue creation was successful.")
        AudioQueueSetParameter(pQueue!, kAudioQueueParam_Volume, 1.0)
        
        var bf: UInt32 = OpusPlayer.bufferSize
        let curBufSize = bf * numberOfChanels
        for i in 0..<bufCount {
            if pQueue == nil {
                print("Error NewOutput")
                return
            }
            status = AudioQueueAllocateBuffer(pQueue!, curBufSize, &self.mBuffers[i])
            if status != 0 {
                print("Error allocate buffer")
                AudioQueueDispose(pQueue!, true)
            }
            else if self.mBuffers[i] != nil{
                self.opusPlayerOutputCallback(selfPointer, pQueue!, self.mBuffers[i]!)
            }
        }
        if pQueue == nil {
            print("Error create pQueue 2")
            return
        }
//        _ = AudioQueueCreateTimeline(self.pQueue!, &self.timeLine)
        
    }
    private func readBuffer(_ buffer: AudioQueueBufferRef){
        if (self.decoder?.read(buffer) ?? false) && self.pQueue != nil{
            if isPausedTapped {
                isPausedTapped = false
                pauseAllPlay()
                return
            }
            AudioQueueEnqueueBuffer(self.pQueue!, buffer, 0, nil)
        }else if pUrl != nil{
            stopCurrentPlaying()
            self.progressesMap[pUrl!] = 0
        }
    }
    private func stopCurrentPlaying(needPassStop: Bool = true){
        if self.pQueue != nil && pUrl != nil{
            self.stopping = true
            paused = false
            AudioQueueStop(self.pQueue!, false)
            saveProgress()
            UIApplication.shared.isIdleTimerDisabled = false
            //UIDevice.current.isProximityMonitoringEnabled = false
            pQueue = nil
            timeLine = nil
            decoder = nil
            if needPassStop {
                passStopped(to: pUrl!)
            }
        }
        if self.progressTimer != nil {
            self.progressTimer?.invalidate()
            self.progressTimer = nil
        }
    }
    
    private func passStopped(to url: URL?) {
        guard let url = url else { return }
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: OPUS_PLAYER_NOTIFICATION_KEY),
                                        object: nil,
                                        userInfo: [OpusPlayer.keyStop : url])
    }
    
    ///Checking AVAudioSession and change output to the earpiece or loud speaker
    func checkSpeaker(){
        if pQueue != nil {
            let session: AVAudioSession = AVAudioSession.sharedInstance()
        
            if session.category != .playback{
                do {
                    try session.setCategory(.playback, mode: .default)
                    try session.setActive(true)
                }catch {
                    print("Error: cant set playback category")
                }
            }
        }
    }
    
    private let opusPlayerOutputCallback: AudioQueueOutputCallback = {
        userData, inAQ, inCompleteAQBuffer in
        
        guard let userData = userData else { return }
        let pl = Unmanaged<OpusPlayer>.fromOpaque(userData).takeUnretainedValue()
        

        
        let buf: AudioQueueBufferRef = inCompleteAQBuffer
        pl.readBuffer(buf)
        
    }
}
