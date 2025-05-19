import Foundation

protocol AudioRecordingObjectDelegate: NSObjectProtocol {
    func showBuble(title: String)
    func didStartRecord()
    func didEndRecord()
    func cancelRecording()
    func lockRecord()
    func didCreateFile(attachment: PSDAttachment, url: URL)
    func sendAudio(attachment: PSDAttachment)
    func changeRecordBottom()
    func showVoiceRecordView()
    func lastCellRect() -> CGRect
    func setSourseAndShowMicrophoneAlert(_ alert: UIAlertController)
    func getAccountId() -> NSInteger
    var recordButton: VoiceRecordButton? { get }
    var voiceRecordView: VoiceRecordView? { get }
    var recordBuble: BubleTextView? { get }
    var lockRecordView: LockView? { get }
}
///The object to coordinate working of VoiceRecordButton, VoiceRecordView and AudioRecordPresenter
@objc class AudioRecordingObject: NSObject{
    private(set) weak var delegate: AudioRecordingObjectDelegate?
    ///The collection view where is new attachment would be added, used for animation.
    private var recordCreator: AudioRecordPresenter!
    private var bubleTimer: Timer?                                   // timer to hide recordBuble
    private static let bubleTimerSec: CGFloat = 2.0
    private var needShowAudioView: Bool = false
    override init() {
        super.init()
        recordCreator = AudioRecordPresenter.init(view: self)
    }
    ///Create AudioRecordingObject with new AudioRecordingObjectDelegate and UICollectionView.
    func createWith(_ delegate: AudioRecordingObjectDelegate){
        self.delegate = delegate
        self.delegate?.recordButton?.delegate = self
    }
    deinit {
        stopTimer()
    }
    private func stopTimer(){
        bubleTimer?.invalidate()
        bubleTimer = nil
    }
}
extension AudioRecordingObject: VoiceRecordButtonDelegate{
    
    func lockRecord() {
        delegate?.lockRecord()
    }
    
    func cancelRecord() {
        delegate?.cancelRecording()
        let rect = self.delegate?.lastCellRect() ?? CGRect.zero
        self.recordCreator?.stop()
        self.delegate?.voiceRecordView?.animateDismiss(to: rect.origin, completion: {
            self.delegate?.voiceRecordView?.removeFromSuperview()
            if let view = self.delegate as? UIView{
                view.setNeedsLayout()
            } else if let vc = self.delegate as? UIViewController{
                vc.view.setNeedsLayout()
            }
        })
    }
    
    ///Return true if need perform action. Using to block all interaction while recording is perform
    @objc func allowInteraction() -> Bool{
        hideRecordBuble(true)
        return !(self.delegate?.recordButton?.isRecording ?? false)
    }
    
    func showBuble(title: String) {
        stopTimer()
        bubleTimer = Timer(
            timeInterval: TimeInterval(AudioRecordingObject.bubleTimerSec),
            target: self,
            selector: #selector(hideRecordBuble(_:)),
            userInfo: nil,
            repeats: true
        )
        RunLoop.current.add(bubleTimer!, forMode: RunLoop.Mode.common)
        
        self.delegate?.showBuble(title: title)
    }
    ///Hides alert over record button, if stopRecord == true, Stops recording and hide everything about it if finger is not on button
    @objc func hideRecordBuble(_ stopRecord: Bool = false) {
        stopTimer()
        self.delegate?.recordBuble?.removeFromSuperview()
        if stopRecord && !(self.delegate?.recordButton?.fingerIsDown() ?? false) && (self.delegate?.recordButton?.isRecording ?? false) {
            self.forseStopRecord()
        }
    }
    
    func voiceRecordStart() {
        self.delegate?.didStartRecord()
        self.recordCreator?.startRecording()
    }
    private static let voiceRecordViewDistance: CGFloat = 15.0
    func changeRecordBottom(){
        self.delegate?.changeRecordBottom()
    }
    
    func voiceRecordStop(needShowAudioView: Bool) {
        self.needShowAudioView = needShowAudioView
        self.delegate?.didEndRecord()
        let rect = self.delegate?.lastCellRect() ?? CGRect.zero
        self.recordCreator?.stopRecording()
        self.delegate?.voiceRecordView?.animateDismiss(to: rect.origin, completion: {
            self.delegate?.voiceRecordView?.removeFromSuperview()
            if let view = self.delegate as? UIView{
                view.setNeedsLayout()
            } else if let vc = self.delegate as? UIViewController{
                vc.view.setNeedsLayout()
            }
        })
    }
}
extension AudioRecordingObject: AudioRecordViewProtocol {
    func needRecording() -> Bool{
        return self.delegate?.recordButton?.fingerIsDown() ?? false
    }
    func getAccountId() -> NSInteger{
        return self.delegate?.getAccountId() ?? 0
    }
    func didCreateFile(attachment: PSDAttachment, url: URL){
        if needShowAudioView {
            self.delegate?.didCreateFile(attachment: attachment,url: url)
        } else {
            delegate?.sendAudio(attachment: attachment)
        }
    }
    func didStartRecord(){
        if self.delegate == nil || self.delegate?.recordButton == nil{
            return
        }
//        delegate?.lockRecordView?.showLock()
        self.delegate?.showVoiceRecordView()
        
    }
    func noRecordPermission(){
        let alert: UIAlertController = UIAlertController(title: "\("PyrusCannotAccessYourMicrophone".localizedPSD())", message: "\("PleaseEnableAccessMicrophone".localizedPSD())", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Cancel".localizedPSD(), style: .cancel, handler: nil))
        alert.addAction(UIAlertAction(title: "Settings".localizedPSD(), style: .default, handler: { _ in
            UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
        }))

        self.delegate?.setSourseAndShowMicrophoneAlert(alert)
    }
    func forseStopRecord(){
        self.delegate?.recordButton?.sendActions(for: .touchUpInside)
    }
    func changeLevelMeter(_ array: [CGFloat]){
        self.delegate?.voiceRecordView?.didChangeVolume(array)
    }
}
