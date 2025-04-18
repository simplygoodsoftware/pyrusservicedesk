//
//  OpusRecorder.swift
//  OpusRecorder
//
//
//

import Foundation
import AVFoundation
protocol OpusRecorderDelegate: class{
    ///Passing array of current records meters
    func parseRecordLevelMeters(_ array: [CGFloat]?)
}
public class OpusRecorder {
    weak var listener: OpusRecorderDelegate?
    @objc private func getLevelMeters(){
        self.listener?.parseRecordLevelMeters(self.microphoneSession?.getLevelMeters(VoiceRecordView.viewsCount, in: VoiceRecordView.minLinesHeight...VoiceRecordView.linesHeight))
    }
    private var microphoneSession: Session?
    private var audioFile: FileHandle?
    private let audioSession = AVAudioSession.sharedInstance()
    private let domain = "org.opus-codec.org"
    private var powerTimer: Timer?                                   // timer to invoke metering callback
    public init() { }
    
    public func start(_ url: URL, failure: ((Error) -> Void)? = nil) {
        do {
            try self.audioFile = FileHandle(forWritingTo: url)
        } catch {
        }
        if self.audioFile == nil{
            //create file
            do {
                try "".write(to: url, atomically: true, encoding: .utf8)
                try self.audioFile = FileHandle(forWritingTo: url)
            }
            catch{
                print("error ceate new file")
            }
        }
        
        // make sure the AVAudioSession shared instance is properly configured
        do {
            try audioSession.setCategory(.playAndRecord, mode: .default)
            if #available(iOS 13.0, *) {
                try audioSession.setAllowHapticsAndSystemSoundsDuringRecording(true)
            }
            try audioSession.setActive(true)
        } catch {
            return
        }
        
        let session = Session()
        session.onError = failure
        session.onMicrophoneData = onMicrophoneData
        session.startMicrophone()
        
        microphoneSession = session
        
        // set metering timer to invoke callback
        powerTimer = Timer(
            timeInterval: TimeInterval(OpusRecorder.meterTimer),
            target: self,
            selector: #selector(getLevelMeters),
            userInfo: nil,
            repeats: true
        )
        RunLoop.current.add(powerTimer!, forMode: RunLoop.Mode.common)
    }
    private static let meterTimer: CGFloat = 0.025
    public func stop() {
        microphoneSession?.stopMicrophone()
        microphoneSession = nil
        
        powerTimer?.invalidate()
        powerTimer = nil
    }
    
    private func onMicrophoneData(data: Data) {
        audioFile?.write(data)
    }
}
