import Foundation
import CoreData

final class CoreDataService {

    private lazy var persistentContainer: NSPersistentContainer = {
        let persistentContainer = NSPersistentContainer(name: "PSDChats")
        persistentContainer.loadPersistentStores { _, error in
            guard let error else { return }
            print("Failed to load persistent stores: \(error)")
        }
        print("Persistent stores loaded successfully")
        return persistentContainer
    }()

    private var viewContext: NSManagedObjectContext {
        persistentContainer.viewContext
    }
    
    private var backgroundContext: NSManagedObjectContext {
        persistentContainer.newBackgroundContext()
    }
}

extension CoreDataService: CoreDataServiceProtocol {

    func fetchChats() throws -> [DBChat] {
        let fetchRequest = DBChat.fetchRequest()
        return try viewContext.fetch(fetchRequest)
    }
    
    func fetchCommands() throws -> [DBTicketCommand] {
        let fetchRequest = DBTicketCommand.fetchRequest()
        return try viewContext.fetch(fetchRequest)
    }

    func fetchClients() throws -> [DBClient] {
        let fetchRequest = DBClient.fetchRequest()
        return try viewContext.fetch(fetchRequest)
    }

    func save(block: @escaping (NSManagedObjectContext) throws -> Void) {
        let backgroundContext = backgroundContext
        backgroundContext.perform {
            do {
                try block(backgroundContext)
                if backgroundContext.hasChanges {
                    try backgroundContext.save()
                    print("Successful save")
                }
            } catch {
                print("Save error: \(error)")
            }
        }
    }
    
    func deleteCommand(id: String) throws {
        let fetchRequest = NSFetchRequest<DBTicketCommand>(entityName: "DBTicketCommand")
        fetchRequest.predicate = NSPredicate(format: "id == %@", id as CVarArg)
        
        do {
            let privateContext = persistentContainer.newBackgroundContext()
            let dbTicketCommand = try privateContext.fetch(fetchRequest).first
            if let dbTicketCommand {
                privateContext.delete(dbTicketCommand)
                try privateContext.save()
                print("Command with ID: \(id) deleted successfully")
            } else {
                print("Command with ID: \(id) not found")
            }
        } catch {
            print("Error deleting command with ID: \(id), \(error)")
            throw error
        }
    }
    
    func deleteChat(id: Int) throws {
        let fetchRequest = NSFetchRequest<DBChat>(entityName: "DBChat")
        fetchRequest.predicate = NSPredicate(format: "chatId == %@", id as CVarArg)
        
        do {
            let privateContext = persistentContainer.newBackgroundContext()
            let dbChannel = try privateContext.fetch(fetchRequest).first
            if let dbChannel = dbChannel {
                privateContext.delete(dbChannel)
                try privateContext.save()
                print("Chat with ID: \(id) deleted successfully")
            } else {
                print("Chat with ID: \(id) not found")
            }
        } catch {
            print("Error deleting chat with ID: \(id), \(error)")
            throw error
        }
    }
    
    func deleteAllObjects(forEntityName entityName: String) {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        let backgroundContext = backgroundContext
        do {
            try backgroundContext.execute(deleteRequest)
            try backgroundContext.save()
            print("All \(entityName)s deleted successfully")
        } catch let error as NSError {
            print("Error deleting objects: \(error), \(error.userInfo)")
        }
    }
}

