import Foundation
let PSD_BUNDLE: Bundle = {
    // 1. Swift Package Manager
    #if SWIFT_PACKAGE
    return Bundle.module
    #else
    // 2. CocoaPods / framework
    return Bundle(for: PyrusServiceDesk.self)
    #endif
}()

extension String {
    func localizedPSD() -> String {
        if 
            let languageCode = CustomizationHelper.customLocale,
            let path = PSD_BUNDLE.path(forResource: languageCode, ofType: "lproj"),
            let bundle = Bundle(path: path)
        {
            return NSLocalizedString(self , tableName: nil, bundle: bundle, value: "", comment: "")
        }
        return PSD_BUNDLE.localizedString(forKey: self, value: nil, table: nil)
    }
}

