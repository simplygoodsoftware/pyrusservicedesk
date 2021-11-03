import Foundation

extension String {
    func hostString() -> String? {
        if
            count > 0,
            let urlDomain = URL(string: self),
            let host = urlDomain.hostString()
        {
            return host
        }
        return nil
    }
    private static var MIN_ENCODER: Int = 1000000
    func encodeToInt() -> Int {
        var b = String.MIN_ENCODER
        let str = self.addingPercentEncoding(withAllowedCharacters: NSCharacterSet.urlQueryAllowed) ?? self
        for (i,char) in str.asciiValues.enumerated() {
            b = b + (Int(char) * (i + 1))
        }
        return b
    }
}

private extension StringProtocol {
    var asciiValues: [UInt8] { compactMap(\.asciiValue) }
}

private extension URL {
    func hostString() -> String? {
        if host != nil {
            return host
        }
        if scheme == nil {
            let urlString = String(format: "http://%@", absoluteString)
            let url = URL(string: urlString)
            return url?.host
        }
        return host
    }
}

