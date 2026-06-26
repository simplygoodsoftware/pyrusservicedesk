import Foundation

enum ChatsInteractorCommand {
    case viewDidload
    case viewWillAppear
    case reloadChats
    case selectChat(index: Int)
    case newChat
    case deleteFilter
    case updateSelected(index: Int)
}

protocol ChatsInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: ChatsInteractorCommand)
}
