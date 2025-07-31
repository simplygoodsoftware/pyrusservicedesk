import UIKit
@_implementationOnly import LibVorbis

@objc extension VorbisDecoder {
    @objc static func getFile() -> OggVorbis_File {
        return OggVorbis_File()
    }
    
    static func canOpenFile(url: URL) -> Bool{
        
//        var vorbisFile = VorbisDecoder.getFile()
//        let error = ov_fopen(url.path.cString(using: .utf8), &vorbisFile)
//        if error != 0  {
//            return false
//        }
//        let errorOpen = ov_test_open(&vorbisFile)
        
        return true
    }
    
    @objc func getPcmTotal() -> Int64 {
        let pcmTotal = ov_pcm_total(&vorbisFile, -1)
        return pcmTotal
    }
}
