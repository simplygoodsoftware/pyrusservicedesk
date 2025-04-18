import Foundation
extension AudioAttachmentView: AudioPlayerViewProtocol{
    ///Redraw play progress line with progress (value betwin 0 and 1)
    func playingProgress(_ progress: CGFloat){
        self.changePlayProgress(progress)
    }
    func setTime(string: String){
        self.timeLabel.text = string
    }
}
