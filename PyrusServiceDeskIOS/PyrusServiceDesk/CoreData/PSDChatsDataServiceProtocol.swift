import Foundation

protocol PSDChatsDataServiceProtocol {
    func saveChatModels(with chatModels: [PSDChat])
    func deleteChats(chatModels: [PSDChat])
    func getAllChats() -> [PSDChat]
    func deleteChannel(with chatId: Int)
    func deleteAllObjects()
    func getAllCommands() -> [TicketCommand] 
    func saveTicketCommand(with ticketCommand: TicketCommand)
    func deleteCommand(with id: String, serverTicketId: Int?)
    func saveClientModels(with clientModels: [PSDClientInfo])
    func saveClientModel(with clientModel: PSDClientInfo)
    func getAllClients() -> [PSDClientInfo]
}
