import Foundation
struct PSDError: LocalizedError {
    
    var errorDescription: String? { return _description }
    
    private var _description: String
    
    init( description: String) {
        self._description = description
    }
}
