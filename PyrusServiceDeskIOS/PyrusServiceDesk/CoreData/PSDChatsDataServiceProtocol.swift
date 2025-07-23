import Foundation
import CoreData

protocol PSDChatsDataServiceProtocol {
    func saveChatModels(with chatModels: [PSDChat], completion: ((Result<Void, Error>) -> Void)?)
    func getAllChats() -> [PSDChat]
    func getAllChats(completion: @escaping ([PSDChat]) -> Void)
    func getChatsHeaders() -> [PSDChat]
    func getChatsHeaders(completion: @escaping ([PSDChat]) -> Void)

    func getAllMessages() -> [PSDMessage]
    func getAllMessages(completion: @escaping ([PSDMessage]) -> Void)
    
    func getAllCommands() -> [TicketCommand]
    func saveTicketCommand(with ticketCommand: TicketCommand, completion: ((Result<Void, Error>) -> Void)?)
    func deleteCommand(with id: String, serverTicketId: Int?)
    func resaveBeforeDeleteCommand(commanId: String, serverTicketId: Int?, completion: ((Result<Void, Error>) -> Void)?)
    
    func saveClientModels(with clientModels: [PSDClientInfo])
    func saveClientModel(with clientModels: [PSDClientInfo], context: NSManagedObjectContext)
    func getAllClients() -> [PSDClientInfo]
    
    func searchMessages(searchString: String) -> [SearchChatModel]
    func searchMessages(searchString: String, completion: @escaping (Result<[SearchChatModel], Error>) -> Void)
    
    func deleteAllObjects()
}
