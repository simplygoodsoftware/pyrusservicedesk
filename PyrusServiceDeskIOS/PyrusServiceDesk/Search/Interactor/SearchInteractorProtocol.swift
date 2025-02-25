import Foundation

enum SearchInteractorCommand {
    case viewDidload
    case viewWillAppear
    case search(text: String)
    case selectChat(index: Int)
    case reloadChats
}

protocol SearchInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: SearchInteractorCommand)
}

