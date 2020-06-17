
import Foundation
extension Array where Element == [PSDRowMessage]{
    ///complete array with unsent messages from storage
    mutating func completeWithUnsentMessages(){
        let messagesFromStorage = PSDMessagesStorage.messagesFromStorage()
        var rowMessages = [PSDRowMessage]()
        for message in messagesFromStorage{
            rowMessages.append(contentsOf: PSDObjectsCreator.parseMessageToRowMessage(message))
        }
        let _ = self.completeWithMessages(rowMessages)
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
     mutating func complete(from chat:PSDChat, startMessage: PSDMessage?, completion: @escaping (_ indexPaths: [IndexPath], _ sections:IndexSet) -> Void){
        let startMessageIndexPath = self.index(of: startMessage)
        
        var index:Int = 0//the index of message in received chat
        if startMessage != nil{
            index = self.index(of:startMessage!, in:chat.messages)
            index = index + 1//start with next message
        }
        
        //completion for update table
        var indexPaths : [IndexPath] =  [IndexPath]()
        var reloadSections : [Int] = [Int]()
        if chat.messages.count > index{
            for i in index..<chat.messages.count{
                let message = chat.messages[i]
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
                            indexPaths.append(IndexPath.init(row: row, section: section))
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
                
                
            }
                
        }
        completion(indexPaths,IndexSet(reloadSections))
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
            if message.message.state != .sent{
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
                    if message.message.localId == findMessage.message.localId{
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


