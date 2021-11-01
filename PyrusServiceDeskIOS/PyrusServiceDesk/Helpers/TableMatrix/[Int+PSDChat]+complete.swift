
import Foundation
extension Array where Element == [PSDRowMessage]{
    ///complete array with unsent messages from storage
    mutating func completeWithUnsentMessages(){
        let messagesFromStorage = PSDMessagesStorage.messagesFromStorage()
        let sortedMessages = messagesFromStorage.sorted(by: {$0.date > $1.date})
        complete(with: sortedMessages, startMessage: nil, completion: { _,_, messages in
            let res = messagesFromStorage.filter { !messages.contains($0)}
            for message in res{
                PSDMessagesStorage.removeFromStorage(messageId: message.clientId)
            }
        })
    }

    mutating func addFakeMessagge(messageId: Int) -> ([IndexPath],IndexSet) {
        let fakeUser = PSDPlaceholderUser()
        let fakeMessage = PSDPlaceholderMessage(owner: fakeUser, messageId: "\(messageId)")
        let lastSection = self.count > 0 ? self.count - 1 : 0
        let lastMessage = self[lastSection].last
        let oldIndex = index(of: fakeMessage)
        var indexPaths =  [IndexPath]()
        var addSections = [Int]()
        guard oldIndex.row == 0, oldIndex.section == 0 else{
            return (indexPaths, IndexSet(addSections))
        }
        guard lastMessage?.message.owner as? PSDPlaceholderUser == nil else {
            return (indexPaths, IndexSet(addSections))
        }
        let messages = PSDObjectsCreator.parseMessageToRowMessage(fakeMessage)
        var addRow = false
        if let lastMessage = lastMessage,
           fakeMessage.date.compareWithoutTime(with: lastMessage.message.date)  == .equal {
            addRow = true
            indexPaths.append(IndexPath(row: self[lastSection].count, section: lastSection))
        }
        else{
            addSections.append(lastSection + 1)
        }
        let oldCount = addRow ? self[lastSection].count : self.count
        let _ = completeWithMessages(messages)
        let newCount = addRow ? self[lastSection].count : self.count
        if oldCount == newCount {
            PyrusLogger.shared.logEvent("Неверный результат добавления фейкового сообщения. Добавлялись ячейки, а не секции = \(addRow), старое количество = \(oldCount), новое количество = \(newCount)")
        }
        return (indexPaths, IndexSet(addSections))
    }
    mutating func removeFakeMessages() -> ([IndexPath],IndexSet) {
        var indexPaths =  [IndexPath]()
        var reloadSections = [Int]()
        var selfCopy = [[PSDRowMessage]]()
        selfCopy.append(contentsOf: self)
        var needStop = false
        let end = count
        guard end > 0 else {
            return (indexPaths, IndexSet(reloadSections))
        }
        for i in 1...end{
            for (row,message) in selfCopy[selfCopy.count-i].enumerated().reversed(){
                guard let _ = message.message as? PSDPlaceholderMessage else{
                    needStop = message.message.owner.personId != PyrusServiceDesk.userId
                    if needStop{
                        break
                    }
                    continue
                }
                let ip  = IndexPath(row: row, section: selfCopy.count-i)
                self.removeMessage(at: ip)
                if self[selfCopy.count-i].count == 0{
                    reloadSections.append(selfCopy.count-i)
                    self.remove(at: selfCopy.count-i)
                    break
                }else{
                    indexPaths.append(ip)
                }
            }
            if needStop{
                break
            }
        }
        return (indexPaths, IndexSet(reloadSections))
    }
    ///Complete array with messages. Messages always add to last section, and checked for duplication by localId
    ///- parameter messages: messages to add
    mutating func completeWithMessages(_ messages: [PSDRowMessage])->Bool{
        if(messages.count > 0){
            let lastSection = self.count > 0 ? self.count - 1 : 0
            let nextSection = self.count > 0 ? self.count : 0
            let lastMessage = self[lastSection].last
            let startMessageIndexPath = IndexPath.init(row: 0, section: lastSection)///need to check dublicate start search dublicates in current date section
            for message in messages{
                //if self have this message ignore it
                if(self.has(findMessage: message, startFrom: startMessageIndexPath)){
                    continue
                }
                if lastMessage != nil && message.message.date.compareWithoutTime(with: lastMessage!.message.date)  == .equal{
                    self[lastSection].append(message)
                }
                else{
                    if self.count - 1  < nextSection{
                        self.append([PSDRowMessage]())
                    }
                    
                    self[nextSection].append(message)
                }
            }
        }
        return true
    }
    /**
     Complete tableMatrix from PSDChat massages.
     - parameters:
     - chat: PSDChat from where we need to take messages
     - startMessage: Last received from server message or nil if we did not
     - completion: A result of adding messages
     - indexPaths: [IndexPath] with indexPaths need to be inserted
     - sections: IndexSet need to be inserted
     */
     mutating func complete(from chat:PSDChat, startMessage: PSDMessage?, completion: @escaping (_ indexPaths: [IndexPath], _ sections:IndexSet, _ messages: [PSDMessage]) -> Void){
        complete(with: chat.messages, startMessage: startMessage, completion: completion)
    }
    mutating private func complete(with messages:[PSDMessage], startMessage: PSDMessage?, completion: ( (_ indexPaths: [IndexPath], _ sections:IndexSet, _ messages: [PSDMessage]) -> Void)?){
        let startMessageIndexPath = self.index(of: startMessage)
        
        var index:Int = 0//the index of message in received chat
        if let startMessage = startMessage{
            index = self.index(of:startMessage, in:messages)
            index = index + 1//start with next message
        }
        
        //completion for update table
        var indexPaths : [IndexPath] =  [IndexPath]()
        var reloadSections : [Int] = [Int]()
        var adddedMessages = [PSDMessage]()
        if messages.count > index{
            for i in index..<messages.count{
                let message = messages[i]
                //if self have this message ignore it
                var alreadyHasMessage = false
                let rowMessages = PSDObjectsCreator.parseMessageToRowMessage(message)
                for rowMessage in rowMessages{
                    if(self.has(findMessage: rowMessage, startFrom: startMessageIndexPath)){
                        alreadyHasMessage = true
                        break
                    }
                }
                if alreadyHasMessage {
                    continue
                }
                let section = self.section(forMessage: message)
                //detect this section exist
                if self.count > section{
                    //if section is exist check if this is correct date or need to move indexes down
                    if self[section].count > 0 && self[section][0].message.date.compareWithoutTime(with: message.date) == .equal{
                        //has some messages with same date add new
                        let row = self.row(forMessage: message, section: section)
                        self[section].insert(contentsOf:PSDObjectsCreator.parseMessageToRowMessage(message), at: row)
                        if(!reloadSections.contains(section)){
                            for i in 0...rowMessages.count-1 {
                                indexPaths.append(IndexPath.init(row: row+i, section: section))
                            }
                        }
                      
                    }
                    else{
                        //no messages with this date need insert new
                        self.insert(PSDObjectsCreator.parseMessageToRowMessage(message), at: section)
                        reloadSections.append(section)
                    }
                }
                else{
                    self.insert(PSDObjectsCreator.parseMessageToRowMessage(message), at: section)
                    reloadSections.append(section)
                }
                adddedMessages.append(message)
                
            }
                
        }
        completion?(indexPaths,IndexSet(reloadSections), adddedMessages)
    }
    ///Find indexPath of message by its messageId start from bottom.
    private func index(of searchMessage:PSDMessage?)->IndexPath{
        if searchMessage != nil{
            for (section,messages) in self.enumerated().reversed(){
                for (row,message) in messages.enumerated().reversed(){
                    if message.message.messageId == searchMessage!.messageId
                    {
                        return IndexPath.init(row: row, section: section)
                    }
                }
            }
        }
        return IndexPath.init(row: 0, section: 0)
    }
    ///Return section for message according to its date
    private func section(forMessage: PSDMessage)->Int{
        for (section,messages) in self.enumerated().reversed(){
            if messages.count > 0{
                if messages[0].message.date.compareWithoutTime(with: forMessage.date)  == .equal{
                    return section
                }
                if messages[0].message.date.compareWithoutTime(with: forMessage.date)  == .more{
                    return section + 1
                }
            }
        }
        return 0
    }
    ///return row for message according to its time
    private func row(forMessage: PSDMessage, section: Int)->Int{
        for (row,message) in self[section].enumerated().reversed(){
            if message.message.state == .sending{
                continue
            }
            if message.message.date.compareTime(with: forMessage.date) == .more || message.message.date.compareTime(with: forMessage.date) == .equal{
                return row + 1
            }
        }
        return 0
    }
    ///Find message in [PSDMessage]. I finded return its index, if not return 0
    private func index(of searchMessage:PSDMessage, in messages:[PSDMessage])->Int{
        var index = messages.count
        for mesage in messages.reversed(){
            index = index-1
            if(mesage.messageId == searchMessage.messageId){
                return index
            }
        }
        return 0
    }
    ///Detect if dictionary has message with same id as findMessage
    ///- parameter startFrom: Is last received from server message, check only messages sending after it.
    private func has(findMessage: PSDRowMessage?, startFrom:IndexPath)->Bool{
        if let findMessage = findMessage{
            let end :Int = count - startFrom.section
            for i in 1...end{
                for (row,message) in self[count-i].enumerated().reversed(){
                    if message.message.state == .sent && !message.hasId() && message.text == findMessage.text && message.attachment?.name == findMessage.attachment?.name && message.rating == findMessage.rating{
                        message.message.messageId = findMessage.message.messageId
                    }
                    if message.message.clientId.lowercased() == findMessage.message.clientId.lowercased(){
                        return true
                    }
                    if message.message.messageId == findMessage.message.messageId && findMessage.hasId(){
                        if (message.message.owner.personId == PyrusServiceDesk.userId) || (message.rating ?? 0 != 0){
                            message.attachment?.serverIdentifer = findMessage.attachment?.serverIdentifer //set new server id that comes from server, because old may be uncorrect
                            return true
                        }else if message.attachment?.serverIdentifer == findMessage.attachment?.serverIdentifer{  //if from server comes many attachments in one message we separate them into different messages. All of them has same ids.
                            return true
                        }
                    }
                    if startFrom.row == row && startFrom.section == count-i{
                        return false
                    }
                }
            }
        }
        return false
    }
}


