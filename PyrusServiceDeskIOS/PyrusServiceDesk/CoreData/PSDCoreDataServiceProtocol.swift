import Foundation
import CoreData

protocol CoreDataServiceProtocol: AnyObject {
    func fetchChats() throws -> [DBChat]
    func fetchCommands() throws -> [DBTicketCommand]
    func fetchClients() throws -> [DBClient]
    func save(block: @escaping (NSManagedObjectContext) throws -> Void)
    func deleteAllObjects(forEntityName entityName: String)
    func deleteCommand(id: String) throws
    func deleteChat(id: Int) throws
}
