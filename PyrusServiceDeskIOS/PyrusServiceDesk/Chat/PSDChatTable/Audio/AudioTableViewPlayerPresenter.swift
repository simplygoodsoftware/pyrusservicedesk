//import Foundation
//
//enum AudioState {
//    case stopped, playing, loading, paused
//}
//
//protocol AudioTableViewPlayerProtocol: AnyObject {
//    func changeState(_ state: AudioState)
//    func updateProgress(_ progress: Float)
//    func setTime(string: String)
//    func startDownloadFileForAttachment()
//    var state: AudioState { get }
//}
//
//class AudioTableViewPlayerPresenter: NSObject {
//    
//    weak var view: AudioTableViewPlayerProtocol?
//    private var fileUrl: URL?
//    private var attachmentId: Int = 0
//    
//    init(view: AudioTableViewPlayerProtocol?, fileUrl: URL?, attachmentId: Int) {
//        self.view = view
//        self.fileUrl = fileUrl
//        self.attachmentId = attachmentId
//        super.init()
//        drawCurrentState()
//        if let url = fileUrl {
//            MultiAudioPlayer.shared.subscribe(id: attachmentId, url: url, with: self)
//        }
//        MultiAudioPlayer.shared.updateTime(id: attachmentId)
//        MultiAudioPlayer.shared.updateProgress(id: attachmentId)
//    }
//
//    func startPlay() {
//        if let url = fileUrl  {
//            MultiAudioPlayer.shared.subscribe(id: attachmentId, url: url, with: self)
//            MultiAudioPlayer.shared.playAudio(id: attachmentId, url: url)
//        }
//    }
//    
//    func buttonWasPressed(){
//        if view?.state == .playing {
//            pausePlay()
//        } else if view?.state == .stopped {
//            view?.startDownloadFileForAttachment()
//        } else {
//            startPlay()
//        }
//    }
//
//    func update(fileUrl: URL?, attachmentId: NSInteger, view: AudioTableViewPlayerProtocol) {
//        self.fileUrl = fileUrl
//        self.attachmentId = attachmentId
//        self.view = view
//        if let url = fileUrl {
//            MultiAudioPlayer.shared.subscribe(id: attachmentId, url: url, with: self)
//        }
//        self.drawCurrentState()
//        MultiAudioPlayer.shared.updateTime(id: attachmentId)
//        MultiAudioPlayer.shared.updateProgress(id: attachmentId)
//    }
//
//    func pausePlay() {
//        MultiAudioPlayer.shared.pauseAudio(id: attachmentId)
//    }
//    
//    func drawCurrentState() {
//        let isPlaying = self.isPlaying()
//        if isPlaying {
//            view?.changeState(.playing)
//        } else if fileUrl != nil {
//            view?.changeState(.paused)
//        } else {
//            view?.changeState(.stopped)
//        }
//    }
//
//    func cleanUp() {
//        attachmentId = 0
//        fileUrl = nil
//        view = nil
//    }
//    
//    func endDragging(_ draggedProgess: Float) {
//        guard let url = fileUrl else { return }
//        MultiAudioPlayer.shared.subscribe(id: attachmentId, url: url, with: self)
//        MultiAudioPlayer.shared.playAudio(id: attachmentId, url: url, fromPosition: draggedProgess)
//    }
//    
//    private func isPlaying() -> Bool {
//        return MultiAudioPlayer.shared.isPlaying(id: attachmentId)
//    }
//}
//
//extension AudioTableViewPlayerPresenter: MultiAudioPlayerDelegate {
//    func audioDidPause() {
//        drawCurrentState()
//    }
//    
//    func audioDidPlay() {
//        drawCurrentState()
//    }
//    
//    func audioDidFinish() {
//        drawCurrentState()
//    }
//    
//    func timeChanged(_ string: String) {
//        view?.setTime(string: string)
//    }
//    
//    func updateProgress(_ progress: Float) {
//        view?.updateProgress(progress)
//    }
//}
