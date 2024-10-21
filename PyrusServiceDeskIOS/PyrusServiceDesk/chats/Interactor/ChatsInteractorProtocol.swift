import Foundation

enum ChatsInteractorCommand {
    case viewDidload
    case viewWillAppear
    case reloadChats
    case selectChat(index: Int)
    case newChat
    case deleteFilter
}

protocol ChatsInteractorProtocol: NSObjectProtocol {
    func doInteraction(_ action: ChatsInteractorCommand)
}
