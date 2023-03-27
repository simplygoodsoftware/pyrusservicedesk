import Foundation
private struct HTMLEscapeMap {
    let escapeSequence: NSString
    let uchar: UInt16
    init(_ escapeSequence: NSString, _ uchar: UInt16) {
        self.escapeSequence = escapeSequence
        self.uchar = uchar
    }
}

// Taken from http://www.w3.org/TR/xhtml1/dtds.html#a_dtd_Special_characters
// Ordered by uchar lowest to highest for bsearching
private let gAsciiHTMLEscapeMap: [HTMLEscapeMap] = [
    // A.2.2. Special characters
    HTMLEscapeMap("&quot;", 34),
    HTMLEscapeMap("&amp;", 38),
    HTMLEscapeMap("&apos;", 39),
    HTMLEscapeMap("&lt;", 60),
    HTMLEscapeMap("&gt;", 62),
    
    // A.2.1. Latin-1 characters
    HTMLEscapeMap("&nbsp;", 32),
    HTMLEscapeMap("&iexcl;", 161),
    HTMLEscapeMap("&cent;", 162),
    HTMLEscapeMap("&pound;", 163),
    HTMLEscapeMap("&curren;", 164),
    HTMLEscapeMap("&yen;", 165),
    HTMLEscapeMap("&brvbar;", 166),
    HTMLEscapeMap("&sect;", 167),
    HTMLEscapeMap("&uml;", 168),
    HTMLEscapeMap("&copy;", 169),
    HTMLEscapeMap("&ordf;", 170),
    HTMLEscapeMap("&laquo;", 171),
    HTMLEscapeMap("&not;", 172),
    HTMLEscapeMap("&shy;", 173),
    HTMLEscapeMap("&reg;", 174),
    HTMLEscapeMap("&macr;", 175),
    HTMLEscapeMap("&deg;", 176),
    HTMLEscapeMap("&plusmn;", 177),
    HTMLEscapeMap("&sup2;", 178),
    HTMLEscapeMap("&sup3;", 179),
    HTMLEscapeMap("&acute;", 180),
    HTMLEscapeMap("&micro;", 181),
    HTMLEscapeMap("&para;", 182),
    HTMLEscapeMap("&middot;", 183),
    HTMLEscapeMap("&cedil;", 184),
    HTMLEscapeMap("&sup1;", 185),
    HTMLEscapeMap("&ordm;", 186),
    HTMLEscapeMap("&raquo;", 187),
    HTMLEscapeMap("&frac14;", 188),
    HTMLEscapeMap("&frac12;", 189),
    HTMLEscapeMap("&frac34;", 190),
    HTMLEscapeMap("&iquest;", 191),
    HTMLEscapeMap("&Agrave;", 192),
    HTMLEscapeMap("&Aacute;", 193),
    HTMLEscapeMap("&Acirc;", 194),
    HTMLEscapeMap("&Atilde;", 195),
    HTMLEscapeMap("&Auml;", 196),
    HTMLEscapeMap("&Aring;", 197),
    HTMLEscapeMap("&AElig;", 198),
    HTMLEscapeMap("&Ccedil;", 199),
    HTMLEscapeMap("&Egrave;", 200),
    HTMLEscapeMap("&Eacute;", 201),
    HTMLEscapeMap("&Ecirc;", 202),
    HTMLEscapeMap("&Euml;", 203),
    HTMLEscapeMap("&Igrave;", 204),
    HTMLEscapeMap("&Iacute;", 205),
    HTMLEscapeMap("&Icirc;", 206),
    HTMLEscapeMap("&Iuml;", 207),
    HTMLEscapeMap("&ETH;", 208),
    HTMLEscapeMap("&Ntilde;", 209),
    HTMLEscapeMap("&Ograve;", 210),
    HTMLEscapeMap("&Oacute;", 211),
    HTMLEscapeMap("&Ocirc;", 212),
    HTMLEscapeMap("&Otilde;", 213),
    HTMLEscapeMap("&Ouml;", 214),
    HTMLEscapeMap("&times;", 215),
    HTMLEscapeMap("&Oslash;", 216),
    HTMLEscapeMap("&Ugrave;", 217),
    HTMLEscapeMap("&Uacute;", 218),
    HTMLEscapeMap("&Ucirc;", 219),
    HTMLEscapeMap("&Uuml;", 220),
    HTMLEscapeMap("&Yacute;", 221),
    HTMLEscapeMap("&THORN;", 222),
    HTMLEscapeMap("&szlig;", 223),
    HTMLEscapeMap("&agrave;", 224),
    HTMLEscapeMap("&aacute;", 225),
    HTMLEscapeMap("&acirc;", 226),
    HTMLEscapeMap("&atilde;", 227),
    HTMLEscapeMap("&auml;", 228),
    HTMLEscapeMap("&aring;", 229),
    HTMLEscapeMap("&aelig;", 230),
    HTMLEscapeMap("&ccedil;", 231),
    HTMLEscapeMap("&egrave;", 232),
    HTMLEscapeMap("&eacute;", 233),
    HTMLEscapeMap("&ecirc;", 234),
    HTMLEscapeMap("&euml;", 235),
    HTMLEscapeMap("&igrave;", 236),
    HTMLEscapeMap("&iacute;", 237),
    HTMLEscapeMap("&icirc;", 238),
    HTMLEscapeMap("&iuml;", 239),
    HTMLEscapeMap("&eth;", 240),
    HTMLEscapeMap("&ntilde;", 241),
    HTMLEscapeMap("&ograve;", 242),
    HTMLEscapeMap("&oacute;", 243),
    HTMLEscapeMap("&ocirc;", 244),
    HTMLEscapeMap("&otilde;", 245),
    HTMLEscapeMap("&ouml;", 246),
    HTMLEscapeMap("&divide;", 247),
    HTMLEscapeMap("&oslash;", 248),
    HTMLEscapeMap("&ugrave;", 249),
    HTMLEscapeMap("&uacute;", 250),
    HTMLEscapeMap("&ucirc;", 251),
    HTMLEscapeMap("&uuml;", 252),
    HTMLEscapeMap("&yacute;", 253),
    HTMLEscapeMap("&thorn;", 254),
    HTMLEscapeMap("&yuml;", 255),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&OElig;", 338),
    HTMLEscapeMap("&oelig;", 339),
    HTMLEscapeMap("&Scaron;", 352),
    HTMLEscapeMap("&scaron;", 353),
    HTMLEscapeMap("&Yuml;", 376),
    
    // A.2.3. Symbols
    HTMLEscapeMap("&fnof;", 402),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&circ;", 710),
    HTMLEscapeMap("&tilde;", 732),
    
    // A.2.3. Symbols cont'd
    HTMLEscapeMap("&Alpha;", 913),
    HTMLEscapeMap("&Beta;", 914),
    HTMLEscapeMap("&Gamma;", 915),
    HTMLEscapeMap("&Delta;", 916),
    HTMLEscapeMap("&Epsilon;", 917),
    HTMLEscapeMap("&Zeta;", 918),
    HTMLEscapeMap("&Eta;", 919),
    HTMLEscapeMap("&Theta;", 920),
    HTMLEscapeMap("&Iota;", 921),
    HTMLEscapeMap("&Kappa;", 922),
    HTMLEscapeMap("&Lambda;", 923),
    HTMLEscapeMap("&Mu;", 924),
    HTMLEscapeMap("&Nu;", 925),
    HTMLEscapeMap("&Xi;", 926),
    HTMLEscapeMap("&Omicron;", 927),
    HTMLEscapeMap("&Pi;", 928),
    HTMLEscapeMap("&Rho;", 929),
    HTMLEscapeMap("&Sigma;", 931),
    HTMLEscapeMap("&Tau;", 932),
    HTMLEscapeMap("&Upsilon;", 933),
    HTMLEscapeMap("&Phi;", 934),
    HTMLEscapeMap("&Chi;", 935),
    HTMLEscapeMap("&Psi;", 936),
    HTMLEscapeMap("&Omega;", 937),
    HTMLEscapeMap("&alpha;", 945),
    HTMLEscapeMap("&beta;", 946),
    HTMLEscapeMap("&gamma;", 947),
    HTMLEscapeMap("&delta;", 948),
    HTMLEscapeMap("&epsilon;", 949),
    HTMLEscapeMap("&zeta;", 950),
    HTMLEscapeMap("&eta;", 951),
    HTMLEscapeMap("&theta;", 952),
    HTMLEscapeMap("&iota;", 953),
    HTMLEscapeMap("&kappa;", 954),
    HTMLEscapeMap("&lambda;", 955),
    HTMLEscapeMap("&mu;", 956),
    HTMLEscapeMap("&nu;", 957),
    HTMLEscapeMap("&xi;", 958),
    HTMLEscapeMap("&omicron;", 959),
    HTMLEscapeMap("&pi;", 960),
    HTMLEscapeMap("&rho;", 961),
    HTMLEscapeMap("&sigmaf;", 962),
    HTMLEscapeMap("&sigma;", 963),
    HTMLEscapeMap("&tau;", 964),
    HTMLEscapeMap("&upsilon;", 965),
    HTMLEscapeMap("&phi;", 966),
    HTMLEscapeMap("&chi;", 967),
    HTMLEscapeMap("&psi;", 968),
    HTMLEscapeMap("&omega;", 969),
    HTMLEscapeMap("&thetasym;", 977),
    HTMLEscapeMap("&upsih;", 978),
    HTMLEscapeMap("&piv;", 982),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&ensp;", 8194),
    HTMLEscapeMap("&emsp;", 8195),
    HTMLEscapeMap("&thinsp;", 8201),
    HTMLEscapeMap("&zwnj;", 8204),
    HTMLEscapeMap("&zwj;", 8205),
    HTMLEscapeMap("&lrm;", 8206),
    HTMLEscapeMap("&rlm;", 8207),
    HTMLEscapeMap("&ndash;", 8211),
    HTMLEscapeMap("&mdash;", 8212),
    HTMLEscapeMap("&lsquo;", 8216),
    HTMLEscapeMap("&rsquo;", 8217),
    HTMLEscapeMap("&sbquo;", 8218),
    HTMLEscapeMap("&ldquo;", 8220),
    HTMLEscapeMap("&rdquo;", 8221),
    HTMLEscapeMap("&bdquo;", 8222),
    HTMLEscapeMap("&dagger;", 8224),
    HTMLEscapeMap("&Dagger;", 8225),
    // A.2.3. Symbols cont'd
    HTMLEscapeMap("&bull;", 8226),
    HTMLEscapeMap("&hellip;", 8230),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&permil;", 8240),
    
    // A.2.3. Symbols cont'd
    HTMLEscapeMap("&prime;", 8242),
    HTMLEscapeMap("&Prime;", 8243),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&lsaquo;", 8249),
    HTMLEscapeMap("&rsaquo;", 8250),
    
    // A.2.3. Symbols cont'd
    HTMLEscapeMap("&oline;", 8254),
    HTMLEscapeMap("&frasl;", 8260),
    
    // A.2.2. Special characters cont'd
    HTMLEscapeMap("&euro;", 8364),
    
    // A.2.3. Symbols cont'd
    HTMLEscapeMap("&image;", 8465),
    HTMLEscapeMap("&weierp;", 8472),
    HTMLEscapeMap("&real;", 8476),
    HTMLEscapeMap("&trade;", 8482),
    HTMLEscapeMap("&alefsym;", 8501),
    HTMLEscapeMap("&larr;", 8592),
    HTMLEscapeMap("&uarr;", 8593),
    HTMLEscapeMap("&rarr;", 8594),
    HTMLEscapeMap("&darr;", 8595),
    HTMLEscapeMap("&harr;", 8596),
    HTMLEscapeMap("&crarr;", 8629),
    HTMLEscapeMap("&lArr;", 8656),
    HTMLEscapeMap("&uArr;", 8657),
    HTMLEscapeMap("&rArr;", 8658),
    HTMLEscapeMap("&dArr;", 8659),
    HTMLEscapeMap("&hArr;", 8660),
    HTMLEscapeMap("&forall;", 8704),
    HTMLEscapeMap("&part;", 8706),
    HTMLEscapeMap("&exist;", 8707),
    HTMLEscapeMap("&empty;", 8709),
    HTMLEscapeMap("&nabla;", 8711),
    HTMLEscapeMap("&isin;", 8712),
    HTMLEscapeMap("&notin;", 8713),
    HTMLEscapeMap("&ni;", 8715),
    HTMLEscapeMap("&prod;", 8719),
    HTMLEscapeMap("&sum;", 8721),
    HTMLEscapeMap("&minus;", 8722),
    HTMLEscapeMap("&lowast;", 8727),
    HTMLEscapeMap("&radic;", 8730),
    HTMLEscapeMap("&prop;", 8733),
    HTMLEscapeMap("&infin;", 8734),
    HTMLEscapeMap("&ang;", 8736),
    HTMLEscapeMap("&and;", 8743),
    HTMLEscapeMap("&or;", 8744),
    HTMLEscapeMap("&cap;", 8745),
    HTMLEscapeMap("&cup;", 8746),
    HTMLEscapeMap("&int;", 8747),
    HTMLEscapeMap("&there4;", 8756),
    HTMLEscapeMap("&sim;", 8764),
    HTMLEscapeMap("&cong;", 8773),
    HTMLEscapeMap("&asymp;", 8776),
    HTMLEscapeMap("&ne;", 8800),
    HTMLEscapeMap("&equiv;", 8801),
    HTMLEscapeMap("&le;", 8804),
    HTMLEscapeMap("&ge;", 8805),
    HTMLEscapeMap("&sub;", 8834),
    HTMLEscapeMap("&sup;", 8835),
    HTMLEscapeMap("&nsub;", 8836),
    HTMLEscapeMap("&sube;", 8838),
    HTMLEscapeMap("&supe;", 8839),
    HTMLEscapeMap("&oplus;", 8853),
    HTMLEscapeMap("&otimes;", 8855),
    HTMLEscapeMap("&perp;", 8869),
    HTMLEscapeMap("&sdot;", 8901),
    HTMLEscapeMap("&lceil;", 8968),
    HTMLEscapeMap("&rceil;", 8969),
    HTMLEscapeMap("&lfloor;", 8970),
    HTMLEscapeMap("&rfloor;", 8971),
    HTMLEscapeMap("&lang;", 9001),
    HTMLEscapeMap("&rang;", 9002),
    HTMLEscapeMap("&loz;", 9674),
    HTMLEscapeMap("&spades;", 9824),
    HTMLEscapeMap("&clubs;", 9827),
    HTMLEscapeMap("&hearts;", 9829),
    HTMLEscapeMap("&diams;", 9830)
]

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

class HelpersStrings {
    
    static func insideDomain(url: URL) -> Bool {
        guard
            let domains = PyrusServiceDesk.trustedUrls,
            domains.count > 0
        else {
            return false
        }
        if
            let scheme = url.scheme,
            domains.contains(scheme)
        {
            return true
        }
        guard
            let host = url.absoluteString.hostString()
        else {
            return false
        }
        return domains.contains(host)
    }
    
    static func attributedString(byDecodingHTMLEntities strAttr: NSMutableAttributedString) -> NSMutableAttributedString {
        var range: NSRange = NSRange(location: 0, length: strAttr.length)
        var subrange: NSRange = (strAttr.string as NSString).range(of: "&", options: .backwards, range: range)

        // if no ampersands, we've got a quick way out
        guard subrange.length > 0 else {
            strAttr.trailingNewlineChopped()
            return strAttr
        }
        while subrange.length != 0 {
            var semiColonRange = NSMakeRange(subrange.location, NSMaxRange(range) - subrange.location)
            semiColonRange = (strAttr.string as NSString).range(of: ";", options: .literal, range: semiColonRange)
            range = NSMakeRange(0, subrange.location)
            guard semiColonRange.location != NSNotFound else {
                continue
            }
            let escapeRange = NSMakeRange(subrange.location, semiColonRange.location - subrange.location + 1)
            let escapeString = (strAttr.string as NSString).substring(with: escapeRange) as NSString
            let length = escapeString.length
            // a squence must be longer than 3 (&lt;) and less than 11 (&thetasym;)
            if (length < 4 || length > 10) {
                continue
            }
            if escapeString.character(at: 1) == unichar("#") {
                let char2 = escapeString.character(at: 2)
                if char2 == unichar("x") || char2 == unichar("X") {
                    // Hex escape squences &#xa3;
                    let hexSequence = escapeString.substring(with: NSMakeRange(3, length - 4))
                    let scanner = Scanner(string: hexSequence)
                    var value = UInt64()
                    if
                        scanner.scanHexInt64(&value),
                        value < CUnsignedShort.max,
                        value > 0,
                        scanner.scanLocation == length - 4
                    {
                        var uchar = UInt16(value)
                        let charString = NSString(characters: &uchar, length: 1)
                        strAttr.replaceCharacters(in: escapeRange, with: charString as String)
                    }
                } else {
                    // Decimal Sequences &#123;
                    let numberSequence = escapeString.substring(with: NSMakeRange(2, length - 3))
                    let scanner = Scanner(string: numberSequence)
                    var value = Int()
                    if
                        scanner.scanInt(&value),
                        value > 0,
                        scanner.scanLocation == length - 3 {
                        if value < CUnsignedShort.max {
                            var uchar = UInt16(value)
                            let charString = NSString(characters: &uchar, length: 1)
                            strAttr.replaceCharacters(in: escapeRange, with: charString as String)
                        } else{
                            if
                                let escapeStringData = escapeString.data(using: String.Encoding.utf8.rawValue),
                                let str = String(data:  escapeStringData, encoding: .utf8),
                                let strData = str.data(using: String.Encoding.utf8),
                                let newStr = try? NSAttributedString(
                                    data: strData,
                                    options: [NSAttributedString.DocumentReadingOptionKey.documentType: NSAttributedString.DocumentType.html,
                                              NSAttributedString.DocumentReadingOptionKey.characterEncoding: NSNumber(value: String.Encoding.utf8.rawValue)],
                                    documentAttributes: nil)
                            {
                                strAttr.replaceCharacters(in: escapeRange, with: newStr.string)
                            }
                        }
                    }
                }
            } else {
                // "standard" sequences
                for i in 0..<(MemoryLayout.size(ofValue: gAsciiHTMLEscapeMap) / MemoryLayout<HTMLEscapeMap>.size) {
                    if escapeString == gAsciiHTMLEscapeMap[i].escapeSequence {
                        var uchar = gAsciiHTMLEscapeMap[i].uchar
                        strAttr.replaceCharacters(in: escapeRange, with: NSString(characters: &uchar, length: 1) as String)
                        break
                    }
                }
            }
                
            subrange = (strAttr.string as NSString).range(of: "&", options: .backwards, range: range)
        }
        strAttr.trailingNewlineChopped()
        return strAttr
    }
}

private extension NSMutableAttributedString {
    func trailingNewlineChopped() {
        checkPrefix()
        checkSuffix()
    }
    
    private func checkSuffix() {
        if string.hasSuffix("\n") {
            deleteCharacters(in: NSRange(location: length - 1,length: 1))
        } else {
            return
        }
    }
    
    private func checkPrefix() {
        if string.hasPrefix("\n") {
            deleteCharacters(in: NSRange(location: 0,length: 1))
        } else {
            return
        }
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

