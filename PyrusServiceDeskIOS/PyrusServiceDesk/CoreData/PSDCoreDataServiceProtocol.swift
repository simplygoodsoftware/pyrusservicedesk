import Foundation
import CoreData

protocol CoreDataServiceProtocol: AnyObject {
    func fetchChats() throws -> [DBChat]
    func fetchMessages() throws -> [DBMessage]
    func fetchCommands() throws -> [DBTicketCommand]
    func fetchMessages(searchString: String) throws -> [DBMessage]
    func fetchChats(searchString: String) throws -> [DBChat]
    func fetchClients() throws -> [DBClient]
    func save(completion: ((Result<Void, Error>) -> Void)?, block: @escaping (NSManagedObjectContext) throws -> Void)
    func deleteAllObjects(forEntityName entityName: String)
    func deleteCommand(id: String) throws
    func deleteChats(ids: [Int64]) throws
    func deleteClients(ids: [String]) throws
    func fetchChatsAndMessages(searchString: String,
                               completion: @escaping (Result<([DBMessage], [DBChat]), Error>) -> Void)
}
