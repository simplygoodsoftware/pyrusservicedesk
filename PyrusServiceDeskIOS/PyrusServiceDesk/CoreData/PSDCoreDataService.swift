import Foundation
import CoreData

enum EntityType {
    case chat, command, client
}

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
         let context = viewContext

            var result: Result<[DBChat], Error> = .success([])
            
            let startTime = DispatchTime.now()
            
            // Используем performAndWait для синхронного выполнения
            context.performAndWait {
                do {
                    let fetchRequest = DBChat.fetchRequest()
                    fetchRequest.sortDescriptors = [NSSortDescriptor(key: "date", ascending: false)]
                    
                    let fetchedObjects = try context.fetch(fetchRequest)
                    result = .success(fetchedObjects)
                } catch {
                    result = .failure(error)
                }
            }
            
            let endTime = DispatchTime.now()
            let nanoseconds = endTime.uptimeNanoseconds - startTime.uptimeNanoseconds
            print("Fetch executed in: \(Double(nanoseconds)/1_000_000) ms")
            
            return try result.get() // Разворачиваем Result
    }
    
    func fetchMessages(searchString: String) throws -> [DBMessage] {
        let fetchRequest = DBMessage.fetchRequest()
        let predicate = NSPredicate(format: "text CONTAINS[cd] %@", searchString)
        fetchRequest.predicate = predicate
        return try viewContext.fetch(fetchRequest)
    }
    
    func fetchChats(searchString: String) throws -> [DBChat] {
        let fetchRequest = DBChat.fetchRequest()
        let predicate = NSPredicate(format: "subject CONTAINS[cd] %@", searchString)
        fetchRequest.predicate = predicate
        return try viewContext.fetch(fetchRequest)
    }
    
    func fetchMessages(searchString: String, completion: @escaping (Result<[DBMessage], Error>) -> Void) {
        let backgroundContext = backgroundContext
        backgroundContext.perform {
            do {
                let fetchRequest = DBMessage.fetchRequest()
                let predicate = NSPredicate(format: "text CONTAINS[cd] %@", searchString)
                fetchRequest.predicate = predicate
                
                let results = try backgroundContext.fetch(fetchRequest)
                completion(.success(results))
            } catch {
                print("Fetch error: \(error)")
                completion(.failure(error))
            }
        }
    }
    
    func fetchChats(searchString: String, completion: @escaping (Result<[DBChat], Error>) -> Void) {
        let backgroundContext = backgroundContext
        backgroundContext.perform {
            do {
                let fetchRequest = DBChat.fetchRequest()
                let predicate = NSPredicate(format: "subject CONTAINS[cd] %@", searchString)
                fetchRequest.predicate = predicate
                
                let results = try backgroundContext.fetch(fetchRequest)
                completion(.success(results))
            } catch {
                print("Fetch error: \(error)")
                completion(.failure(error))
            }
        }
    }
    
    func fetchChatsAndMessages(searchString: String,
                               completion: @escaping (Result<([DBMessage], [DBChat]), Error>) -> Void) {
        let backgroundContext = backgroundContext
        backgroundContext.perform {
            do {
                let mfetchRequest = DBMessage.fetchRequest()
                let mpredicate = NSPredicate(format: "text CONTAINS[cd] %@", searchString)
                mfetchRequest.predicate = mpredicate
                let msortDescriptor = NSSortDescriptor(key: "date", ascending: false)
                mfetchRequest.sortDescriptors = [msortDescriptor]
                mfetchRequest.fetchLimit = 50
                let messages = try backgroundContext.fetch(mfetchRequest)
                
                let fetchRequest = DBChat.fetchRequest()
                let predicate = NSPredicate(format: "subject CONTAINS[cd] %@", searchString)
                fetchRequest.predicate = predicate
                let sortDescriptor = NSSortDescriptor(key: "date", ascending: false)
                fetchRequest.sortDescriptors = [sortDescriptor]
                fetchRequest.fetchLimit = 50
                let chats = try backgroundContext.fetch(fetchRequest)
                
                completion(.success((messages, chats)))
            } catch {
                print("Fetch error: \(error)")
                completion(.failure(error))
            }
        }
    }
    
    func fetchCommands() throws -> [DBTicketCommand] {
        let fetchRequest = DBTicketCommand.fetchRequest()
        return try viewContext.fetch(fetchRequest)
    }

    func fetchClients() throws -> [DBClient] {
        let fetchRequest = DBClient.fetchRequest()
        return try viewContext.fetch(fetchRequest)
    }

    func save(completion: ((Result<Void, Error>) -> Void)?, block: @escaping (NSManagedObjectContext) throws -> Void) {
        let backgroundContext = backgroundContext
        backgroundContext.perform {
            do {
                try block(backgroundContext)
                if backgroundContext.hasChanges {
                    try backgroundContext.save()
                    print("Successful save")
                }
                completion?(.success(()))
            } catch {
                print("Save error: \(error)")
                completion?(.failure(error))
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
    
    func deleteChats(ids: [Int64]) throws {
        let fetchRequest: NSFetchRequest<NSFetchRequestResult> = NSFetchRequest<NSFetchRequestResult>(entityName: "DBChat")
        fetchRequest.predicate = NSPredicate(format: "NOT(chatId IN %@)", ids)
        
        do {
            let privateContext = persistentContainer.newBackgroundContext()
            let batchDeleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
            try privateContext.execute(batchDeleteRequest)
            try privateContext.save()

            print("chats wdeleted successfully")
        } catch {
            print("Error deleting chats, \(error)")
            throw error
        }
    }
    
    func deleteClients(ids: [String]) throws {
        let fetchRequest: NSFetchRequest<NSFetchRequestResult> = NSFetchRequest<NSFetchRequestResult>(entityName: "DBClient")
        fetchRequest.predicate = NSPredicate(format: "NOT(appId IN %@)", ids)
        
        do {
            let privateContext = persistentContainer.newBackgroundContext()
            let batchDeleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
            try privateContext.execute(batchDeleteRequest)
            try privateContext.save()

            print("clients wdeleted successfully")
        } catch {
            print("Error deleting chats, \(error)")
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

