import Foundation
import libopus
import PyrusServiceDeskPrivate
// import ogg

extension OggOpusDecoder {
    static func canOpenFile(url: URL) -> Bool{
        
        var error = OPUS_OK
        let sd = op_open_file(url.path, &error)
        let file = op_test_file(url.path, &error)
        if file != nil{
            error = op_test_open(file!)
            op_free(file)
            return error == OPUS_OK
        }
        return false
    }
    
//    @objc func getPcmTotal() -> Int64 {
//        let pcmTotal = op_pcm_total(oggFile, -1)
//        return pcmTotal
//    }
}
