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

