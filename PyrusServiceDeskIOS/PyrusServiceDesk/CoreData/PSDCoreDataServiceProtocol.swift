import Foundation
import CoreData

protocol CoreDataServiceProtocol: AnyObject {
    func fetchChats() throws -> [DBChat]
    func fetchCommands() throws -> [DBTicketCommand]
    func fetchMessages(searchString: String) throws -> [DBMessage]
    func fetchClients() throws -> [DBClient]
    func save(completion: ((Result<Void, Error>) -> Void)?, block: @escaping (NSManagedObjectContext) throws -> Void)
    func deleteAllObjects(forEntityName entityName: String)
    func deleteCommand(id: String) throws
    func deleteChats(ids: [Int64]) throws
    func deleteClients(ids: [String]) throws
}
