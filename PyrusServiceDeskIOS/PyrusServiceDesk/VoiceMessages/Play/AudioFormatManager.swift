import Foundation

@objc protocol AudioDecoderProtocol {
    func chanelsCount() -> Int
    func psmOffset() -> Int64
    func read(_ buffer: AudioQueueBufferRef) -> Bool
    func getPcmTotal() -> Int64
    
    func getTotalTime() -> CGFloat
    func getSampleRate() -> Int64
}

class AudioFormatManager {
    static func getDecoder(url: URL, offSet: Int64) -> AudioDecoderProtocol? {
        if VorbisDecoder.canOpenFile(url: url) {
            return VorbisDecoder(url: url, offset: offSet)
        } else if OggOpusDecoder.canOpenFile(url: url) {
            return OggOpusDecoder(url: url, offset: offSet)
        }
        return nil
    }
    
    static func getPcmTotal(path: String) -> Int64 {
        guard let url = URL(string: path) else {
            return 0
        }
        let decoder = getDecoder(url: url, offSet: 0)
        return decoder?.getPcmTotal() ?? 0
    }
    
    static func getTotalTime(path: String) -> CGFloat {
        guard let url = URL(string: path) else {
            return 0
        }
        let decoder = getDecoder(url: url, offSet: 0)
        return decoder?.getTotalTime() ?? 0
    }
}
