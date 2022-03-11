import Foundation
import Compression
import zlib
extension Data {
    static func gzipData(_ data: Data?) -> Data? {
        guard let data = data, data.count > 0 else {
            return nil
        }
        var stream = z_stream()
        stream.total_out = 0
        data.withUnsafeBytes({
            buffer in
            guard let bytes = buffer.baseAddress?.assumingMemoryBound(to: Bytef.self) else {
                    return
            }
            stream.next_in = UnsafeMutablePointer<Bytef>(mutating: bytes)
            
        })
        stream.avail_in = uint(data.count)
        
        let initError = deflateInit2_(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED, MAX_WBITS + 16, MAX_MEM_LEVEL, Z_DEFAULT_STRATEGY, ZLIB_VERSION, STREAM_SIZE)
        guard initError == Z_OK else {
            let errorMsg: String
            switch initError {
            case Z_STREAM_ERROR:
                errorMsg = "Invalid parameter passed in to function."
            case Z_MEM_ERROR:
                errorMsg = "Insufficient memory."
            case Z_VERSION_ERROR:
                errorMsg = "The version of zlib.h and the version of the library linked do not match."
            default:
                errorMsg = "Unknown error code."
            }
            PyrusLogger.shared.logEvent("\(#function): deflateInit2() Error: \(errorMsg) Message: \(String(describing: stream.msg))")
            return nil
        }
        var compressedData = Data(count: data.count * Int(1.001) + 12)
        var deflateStatus: Int32 = Z_OK
        
        while deflateStatus == Z_OK {
            compressedData.withUnsafeMutableBytes({
                buffer in
                guard let bytes = buffer.baseAddress?.assumingMemoryBound(to: Bytef.self) else {
                        return
                }
                stream.next_out = bytes.advanced(by: Int(stream.total_out))
                
            })
            stream.avail_out = uInt(data.count) - uInt(stream.total_out)
            
            deflateStatus = deflate(&stream, Z_FINISH)
        }
        
        if deflateStatus != Z_STREAM_END {
            let errorMsg: String
            switch deflateStatus {
            case Z_ERRNO:
                errorMsg = "Error occured while reading file."
            case Z_STREAM_ERROR:
                errorMsg = "The stream state was inconsistent (e.g., next_in or next_out was NULL)."
            case Z_DATA_ERROR:
                errorMsg = "The deflate data was invalid or incomplete."
            case Z_MEM_ERROR:
                errorMsg = "Memory could not be allocated for processing."
            case Z_BUF_ERROR:
                errorMsg = "Ran out of output buffer for writing compressed bytes."
            case Z_VERSION_ERROR:
                errorMsg = "The version of zlib.h and the version of the library linked do not match."
            default:
                errorMsg = "Unknown error code."
            }
            PyrusLogger.shared.logEvent("\(#function): zlib error while attempting compression \(errorMsg) Message: \(String(describing: stream.msg))")
            deflateEnd(&stream)
            return nil
        }
        
        deflateEnd(&stream)
        
        compressedData.count = Int(stream.total_out)
        
        PyrusLogger.shared.logEvent("\(#function): Compressed file from  \(data.count/1024) KB to \(compressedData.count/1024))")
        
        return compressedData
    }
}
private let STREAM_SIZE: Int32 = Int32(MemoryLayout<z_stream>.size)
