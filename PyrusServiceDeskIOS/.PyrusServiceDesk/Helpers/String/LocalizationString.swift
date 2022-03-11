import Foundation
let PSD_BUNDLE = Bundle(for: PyrusServiceDesk.self)
extension String {
    func localizedPSD() -> String {
        
        return PSD_BUNDLE.localizedString(forKey: self, value: nil, table: nil)
    }
}

