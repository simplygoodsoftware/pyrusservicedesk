import Foundation
class PSDPlaceholderUser: PSDUser {
    init(){
        super.init(personId: "", name: "", type: .support, imagePath: "")
    }
    
    required public init(from decoder: Decoder) throws {
        try super.init(from: decoder)
    }
}
