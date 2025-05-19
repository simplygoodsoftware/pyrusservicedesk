import Foundation
private let recordBubleDist: CGFloat = 5
private let voiceRecordViewDistance: CGFloat = 15.0
protocol RecordableViewProtocol: AudioRecordingObjectDelegate{
    var recordButton: VoiceRecordButton? { get }
    var recordBuble: BubleTextView? { get set }
    var voiceRecordView: VoiceRecordView? { get set }
}
extension RecordableViewProtocol where Self: UIView{
    private func recordButtonRect() -> CGRect{
        if let recordButton = recordButton{
            return self.convert(recordButton.frame, from: recordButton.superview)
        }else{
            return CGRect.zero
        }
    }
    func showBuble(title: String) {
        if let recordBuble = recordBuble, self.subviews.contains(recordBuble){
            return
        }
        if recordBuble == nil{
            recordBuble = BubleTextView()
        }
        if let recordBuble = self.recordBuble{
            var frameBubble = recordBuble.frame
            let buttonRect = recordButtonRect()
            frameBubble.origin.y = buttonRect.origin.y - recordBubleDist
            frameBubble.size.width = self.bounds.size.width
            recordBuble.frame = frameBubble
            self.addSubview(recordBuble)
            recordBuble.alpha = 0.0
            let center = (buttonRect.origin.x + buttonRect.size.width / 2) - (frameBubble.size.width / 2)
            recordBuble.load(withTitle: title, maxWidth: self.frame.size.width, centerOfView: center, showOnTop: true)
            recordBuble.animateShow()
        }
    }
    func showVoiceRecordView(){
        if self.voiceRecordView == nil {
            self.voiceRecordView = VoiceRecordView.init(frame: CGRect.zero)
        }
        if let voiceRecordView = self.voiceRecordView{
            self.addSubview(voiceRecordView)
            self.sendSubviewToBack(voiceRecordView)
            self.changeRecordBottom()
            self.layoutIfNeeded()
            let buttonRect = recordButtonRect()
            let point = CGPoint(x: buttonRect.origin.x + (buttonRect.size.width / 2),y: buttonRect.origin.y + (buttonRect.size.height / 2))
            voiceRecordView.animateAppearance(from: point, completion: nil)
        }
    }
    func changeRecordBottom(){
        let expextedY = self.frame.size.height + voiceRecordViewDistance
        var bottomY = expextedY
        if self.convert(CGPoint(x: 0, y: -voiceRecordViewDistance - VoiceRecordView.size.height), to: nil).y < 0 {
            bottomY = bottomY - VoiceRecordView.size.height
        }
        self.voiceRecordView?.bottomLayoutConstraintConstant = bottomY
    }
}
