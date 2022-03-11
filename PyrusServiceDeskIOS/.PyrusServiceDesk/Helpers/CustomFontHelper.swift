import Foundation
extension UIFont {

    private func withTraits(traits:UIFontDescriptor.SymbolicTraits...) -> UIFont? {
        guard let descriptor = self.fontDescriptor.withSymbolicTraits(UIFontDescriptor.SymbolicTraits(traits)) else{
            return nil
        }
        return UIFont(descriptor: descriptor, size: 0)
    }

    func bold() -> UIFont? {
        return withTraits(traits: .traitBold)
    }

    func italic() -> UIFont? {
        return withTraits(traits: .traitItalic)
    }

    func boldItalic() -> UIFont? {
        return withTraits(traits: .traitBold, .traitItalic)
    }

}
