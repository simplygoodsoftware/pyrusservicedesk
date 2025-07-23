import Foundation
import AVFAudio

@objc protocol AudioRecordViewProtocol: class{
    func didCreateFile(attachment: PSDAttachment, url: URL)
    func didStartRecord()
    func noRecordPermission()
    func forseStopRecord()
    func changeLevelMeter(_ array: [CGFloat])
    func getAccountId() -> NSInteger
    ///Recording is start after animation complete - ask to protocol: "is recording is still needed?"
    func needRecording() -> Bool
}
///The object that is create audio recorder
@objc class AudioRecordPresenter: NSObject{
    weak var view: AudioRecordViewProtocol?
    var recorder: OpusRecorder?
    private var limitTimer: Timer?                                   // timer to break long record
    private var startRecordTime: DispatchTime?
    private static let recordingLimit: Int = 3600//seconds
    private static let recordingMinLimit: Int = 1 * 1000000000//nanoseconds
    @objc init(view: AudioRecordViewProtocol?) {
        self.view = view
    }
    private static let startRecordLogName: String = "Audio : start record"
    func startRecording(){
        let thisIsFirstTime = AVAudioSession.sharedInstance().recordPermission == .undetermined
        let session = AVAudioSession.sharedInstance()
        session.requestRecordPermission{
            granted in
            guard granted else {
                DispatchQueue.main.async {
                    if  !thisIsFirstTime{
                        self.view?.noRecordPermission()
                    }
                    self.view?.forseStopRecord()
                }
                return
            }
            if !thisIsFirstTime {
                // Получаем путь к директории Documents/audio
                if let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
                    let audioDirectory = documentsDirectory.appendingPathComponent("audio")

                    // Создаём директорию, если её ещё нет
                    if !FileManager.default.fileExists(atPath: audioDirectory.path) {
                        do {
                            try FileManager.default.createDirectory(at: audioDirectory, withIntermediateDirectories: true, attributes: nil)
                        } catch {
                            print("Failed to create audio directory: \(error)")
                            return
                        }
                    }

                    // Генерируем путь к файлу
                    self.audioUrl = audioDirectory.appendingPathComponent("\(self.createFilename()).opus")

                    // Обновляем UI на главном потоке
                    DispatchQueue.main.async {
                        self.view?.didStartRecord()
                    }

                    // Стартуем запись с задержкой
                    DispatchQueue.main.asyncAfter(deadline: .now() + TimeInterval(VoiceRecordView.appearDuration)) {
                        if self.view?.needRecording() ?? false {
                            self.startRecordTime = DispatchTime.now()
                            self.recorder = OpusRecorder()
                            self.recorder?.listener = self
                            self.recorder?.start(self.audioUrl!)
                        } else {
                            self.cleanUpTimers()
                        }
                    }

                    // Таймер на ограничение длины записи
                    self.limitTimer = Timer(
                        timeInterval: TimeInterval(AudioRecordPresenter.recordingLimit),
                        target: self,
                        selector: #selector(self.timeIsUp),
                        userInfo: nil,
                        repeats: false
                    )
                    RunLoop.current.add(self.limitTimer!, forMode: .common)
                }
            }
        }
    }
    private static let animationDev: CGFloat = 1.7
    @objc private func timeIsUp(){
        self.view?.forseStopRecord()
    }
    private func cleanUpTimers(){
        limitTimer?.invalidate()
        limitTimer = nil
    }
    
    func stopRecording() {
        recorder?.stop()
        let nowTime = DispatchTime.now().uptimeNanoseconds
        let tooShortFile = nowTime - (self.startRecordTime?.uptimeNanoseconds ?? nowTime) < AudioRecordPresenter.recordingMinLimit
        if let audioUrl = audioUrl, recorder != nil && !tooShortFile {
            let attachment =
            PSDAttachment(localPath: audioUrl.path, data: PSDFilesManager.dataFrom(url: audioUrl), serverIdentifer: nil)
            attachment.name = audioUrl.lastPathComponent
            attachment.localPath = audioUrl.path
            self.view?.didCreateFile(attachment: attachment, url: audioUrl)
        } else if tooShortFile {
            let notificationFeedbackGenerator = UINotificationFeedbackGenerator()
            notificationFeedbackGenerator.prepare()
            notificationFeedbackGenerator.notificationOccurred(.error)
        }
        self.startRecordTime = nil
        recorder = nil
        cleanUpTimers()
    }
    
    func stop() {
        recorder?.stop()
        self.startRecordTime = nil
        recorder = nil
        cleanUpTimers()
    }
    
    deinit {
        cleanUpTimers()
    }
    
    private let fileNamePrefix = "Recording_"
    private(set) var audioUrl: URL?
    private func createFilename() -> String {
        let dateFormat = DateFormatter()
        dateFormat.dateFormat = "ddMMyyyy_HHmmss"//"DTF_Full".localizedPSD()
        return fileNamePrefix + dateFormat.string(from: Date())
    }
}

extension AudioRecordPresenter: OpusRecorderDelegate {
    
    func parseRecordLevelMeters(_ array: [CGFloat]?) {
        view?.changeLevelMeter(array ?? [CGFloat]())
    }
}
