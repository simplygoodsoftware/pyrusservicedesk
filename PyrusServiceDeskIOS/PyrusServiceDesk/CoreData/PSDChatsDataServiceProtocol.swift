import Foundation
import CoreData

protocol PSDChatsDataServiceProtocol {
    func saveChatModels(with chatModels: [PSDChat], completion: ((Result<Void, Error>) -> Void)?)
    func getAllChats() -> [PSDChat]
    func deleteAllObjects()
    func getAllCommands() -> [TicketCommand] 
    func saveTicketCommand(with ticketCommand: TicketCommand, completion: ((Result<Void, Error>) -> Void)?)
    func deleteCommand(with id: String, serverTicketId: Int?)
    func saveClientModels(with clientModels: [PSDClientInfo])
    func saveClientModel(with clientModels: [PSDClientInfo], context: NSManagedObjectContext)
    func getAllClients() -> [PSDClientInfo]
    func resaveBeforeDeleteCommand(commanId: String, serverTicketId: Int?, completion: ((Result<Void, Error>) -> Void)?)
}
