//import MobileVLCKit
//
//protocol MultiAudioPlayerDelegate: AnyObject {
//    func audioDidPause()
//    func audioDidPlay()
//    func audioDidFinish()
//    func timeChanged(_ string: String)
//    func updateProgress(_ progress: Float)
//}
//
//class MultiAudioPlayer: NSObject {
//    private var subscribers: [Int: MultiAudioPlayerDelegate] = [:]
//    
//    static let shared = MultiAudioPlayer()
//    
//    private var audioPlayers: [NSInteger: VLCMediaPlayer] = [:]
//    private var playersToId: [VLCMediaPlayer: NSInteger] = [:]
//    
//    private var currentID: NSInteger?
//    
//    override init() {
//        super.init()
//      //  NotificationCenter.default.addObserver(self, selector: #selector(handleDisappearedNotification), name: NSNotification.Name(TASK_CLOSED_CONTROLLER_NOTIFICATION), object: nil)
//    }
//    
//    func subscribe(id: Int, url: URL, with subscriber: MultiAudioPlayerDelegate) {
//        subscribers[id] = subscriber
//        addAudioIfNeeded(id: id, url: url)
//    }
//    
//    func updateTime(id: Int) {
//        guard let player = audioPlayers[id] else {
//            return
//        }
//        setTime(player: player, id: id)
//    }
//    
//    func updateProgress(id: Int) {
//        guard let player = audioPlayers[id] else {
//            return
//        }
//        subscribers[id]?.updateProgress(player.position)
//    }
//    
//    func playAudio(id: NSInteger, url: URL, fromPosition position: Float? = nil) {
//        if let position = position {
//            playAudioFromPosition(id: id, position: position)
//            return
//        }
//        
//        if let currentID = currentID, let currentPlayer = audioPlayers[currentID] {
//            currentPlayer.pause()
//        }
//        
//        if let player = audioPlayers[id] {
//            player.play()
//            currentID = id
//        }
//    }
//    
//    func pauseAudio(id: NSInteger) {
//        if currentID == id, let currentPlayer = audioPlayers[id] {
//            currentPlayer.pause()
//            currentID = nil
//        }
//    }
//    
//    func isPlaying(id: NSInteger) -> Bool {
//        if currentID != id {
//            return false
//        }
//        let mediaPlayer = audioPlayers[id]
//        return mediaPlayer?.isPlaying ?? false
//    }
//    
//    func audioFullTime(_ url: URL) -> Int32 {
//        let media = VLCMedia(url: url)
//        let length = media.length.intValue
//        if length > 0 {
//            return length
//        }
//        guard let nowPlusFive = Calendar.current.date(byAdding: .second,
//                                                      value: 5,
//                                                      to: Date()) else {
//            return length
//        }
//        let lengthWait = media.lengthWait(until: nowPlusFive).intValue
//        if
//            lengthWait == 0,
//            let url = media.url
//        {
//            return Int32(oggOpusTimeCalclate(path: url.path) * 1000)
//        }
//        return lengthWait
//    }
//    
//    func audioProgress(from attachmentId: Int) -> Float {
//        return audioPlayers[attachmentId]?.position ?? 0
//    }
//    
//    func audioWasDeleted(attachmentId: Int) {
//        subscribers.removeValue(forKey: attachmentId)
//        if let player = audioPlayers[attachmentId] {
//            player.stop()
//            playersToId.removeValue(forKey: player)
//        }
//        audioPlayers.removeValue(forKey: attachmentId)
//    }
//    
//    func clean() {
//        playersToId.removeAll()
//        audioPlayers.removeAll()
//        subscribers.removeAll()
//        currentID = nil
//    }
//    
//    private func addAudioIfNeeded(id: NSInteger, url: URL) {
//        let player: VLCMediaPlayer
//        let media: VLCMedia
//        // Кастомное кэширование для локальных файлов (мс). Без этого свойства аудио стартует/продолжается с задержкой в ±1 сек.
//        let options = [
//            "--file-caching=50"
//        ]
//        if !audioPlayers.keys.contains(id) {
//            media = VLCMedia(url: url)
//            player = VLCMediaPlayer(options: options)
//            player.media = media
//            player.delegate = self
//            
//            audioPlayers[id] = player
//            playersToId[player] = id
//        } else {
//            player = audioPlayers[id] ?? VLCMediaPlayer(options: options)
//            media = player.media ?? VLCMedia(url: url)
//        }
//        setTime(player: player, id: id)
//        
//    }
//    
//    private func playAudioFromPosition(id: NSInteger, position: Float) {
//        let safePosition = max(0.0, min(1.0, position))
//        
//        if let player = audioPlayers[id] {
//            if let currentID = currentID, currentID != id, let currentPlayer = audioPlayers[currentID] {
//                currentPlayer.pause()
//            }
//            
//            player.play()
//            player.position = safePosition
//
//            currentID = id
//        }
//    }
//    
//    private func setTime(player: VLCMediaPlayer, id: Int, endOfAudio: Bool = false) {
//        guard let url = player.media?.url else { return }
//        
//        let fullTime = Float(audioFullTime(url))
//        var p = endOfAudio ? 0 : roundf(player.position * fullTime) / 1000
//        
//        let f = roundf(fullTime) / 1000
//        if p > f {
//            p = f
//        }
//        let progStr = createTimeString(from: Int(p))
//        let fullStr = f > 0 ? createTimeString(from: Int(f)) : "--:--"
//        let timeString = "\(progStr)/\(fullStr)"
//        
//        subscribers[id]?.timeChanged(timeString)
//    }
//    
//    private func createTimeString(from seconds: Int) -> String {
//        let hour = seconds / 3600
//        let min = (seconds % 3600) / 60
//        let sec = (seconds % 3600) % 60
//        
//        let hourString = hour > 0 ? String(format: "%02d:", hour) : ""
//        return String(format: "%@%02d:%02d", hourString, min, sec)
//    }
//    
//    private func setEndedState(player: VLCMediaPlayer, id: Int) {
//        subscribers[id]?.audioDidFinish()
//        subscribers[id]?.updateProgress(0)
//        setTime(player: player, id: id, endOfAudio: true)
//    }
//    
//    @objc private func handleDisappearedNotification(_ notification: Notification) {
//        clean()
//    }
//    
//    deinit {
//        NotificationCenter.default.removeObserver(self)
//    }
//}
//
//extension MultiAudioPlayer: VLCMediaPlayerDelegate {
//    func mediaPlayerStateChanged(_ aNotification: Notification) {
//        guard
//            let player = aNotification.object as? VLCMediaPlayer,
//            let id = playersToId[player]
//        else { return }
//        if player.state == .ended {
//            setEndedState(player: player, id: id)
//            audioPlayers[id] = nil
//            subscribers[id] = nil
//            playersToId[player] = nil
//        } else if player.state == .paused {
//            subscribers[id]?.audioDidPause()
//        } else if player.state == .playing {
//            subscribers[id]?.audioDidPlay()
//        }
//    }
//    
//    func mediaPlayerTimeChanged(_ aNotification: Notification) {
//        guard
//            let player = aNotification.object as? VLCMediaPlayer,
//            let id = playersToId[player]
//        else { return }
//        setTime(player: player, id: id)
//        subscribers[id]?.updateProgress(player.position)
//    }
//}
//
////MARK: Calculation time
//private extension MultiAudioPlayer {
//    private static let OpusAudioPlayerSampleRate = Int(AVAudioSession.sharedInstance().sampleRate)
//    private func oggOpusTimeCalclate(path: String) -> CGFloat {
//        let pcmTotal: Int = Int(self.getPcmTotal(path: path))
//        return CGFloat(pcmTotal / MultiAudioPlayer.OpusAudioPlayerSampleRate)
//    }
//    
//    private func getPcmTotal(path: String) -> Int64{
//        var error = OPUS_OK
//        if let file = op_test_file(strdup(path), &error){
//            var pcmTotal: Int64 = 0
//            error = op_test_open(file)
//            pcmTotal = op_pcm_total(file, -1)
//            op_free(file)
//            return pcmTotal
//        }
//        return 0
//    }
//}
