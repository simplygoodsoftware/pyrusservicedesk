import Foundation
import CoreData

protocol CoreDataServiceProtocol: AnyObject {
    func fetchChats() throws -> [DBChat]
 //   func fetchMessages(for channelUUID: Int) throws -> [DBMessage]
    func save(block: @escaping (NSManagedObjectContext) throws -> Void)
    func deleteAllObjects(forEntityName entityName: String)
 //   func deleteAllMessages(for chatUUID: String)
    func deleteChat(id: Int) throws
}
