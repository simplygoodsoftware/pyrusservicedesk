import Foundation

class TicketCommandRepository {
    private let fileURL: URL
    private var commandsCache: [TicketCommand]?
    
    init(filename: String = "ticketCommands.json") {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        self.fileURL = paths[0].appendingPathComponent(filename)
    }
    
    func save(commands: [TicketCommand], completion: ((Error?) -> Void)? = nil) {
        self.commandsCache = commands
        
        DispatchQueue.global(qos: .background).async {
            do {
                let encoder = JSONEncoder()
                encoder.outputFormatting = .prettyPrinted
                let data = try encoder.encode(commands)
                try data.write(to: self.fileURL)
                DispatchQueue.main.async {
                    completion?(nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion?(error)
                }
            }
        }
    }
    
    func load(completion: @escaping (Result<[TicketCommand], Error>) -> Void) {
        if let cachedCommands = commandsCache {
            completion(.success(cachedCommands))
            return
        }
        
        DispatchQueue.global(qos: .background).async {
            do {
                let data = try Data(contentsOf: self.fileURL)
                let decoder = JSONDecoder()
                let commands = try decoder.decode([TicketCommand].self, from: data)
                
                self.commandsCache = commands
                
                DispatchQueue.main.async {
                    completion(.success(commands))
                }
            } catch {
                self.commandsCache = []
                DispatchQueue.main.async {
                    completion(.failure(error))
                }
            }
        }
    }
    
    func add(command: TicketCommand, completion: ((Error?) -> Void)? = nil) {
        load { result in
            switch result {
            case .success(var commands):
                commands.append(command)
                self.save(commands: commands, completion: completion)
            case .failure(let error):
                completion?(error)
            }
        }
    }
    
    func deleteCommand(withId commandId: String, completion: ((Error?) -> Void)? = nil) {
        load { result in
            switch result {
            case .success(var commands):
                commands.removeAll(where: {$0.commandId.lowercased() == commandId.lowercased()})
                self.save(commands: commands, completion: completion)
            case .failure(let error):
                completion?(error)
            }
        }
    }
    
    func clear(completion: ((Error?) -> Void)? = nil) {
        commandsCache = []
        save(commands: [], completion: completion)
    }
}
