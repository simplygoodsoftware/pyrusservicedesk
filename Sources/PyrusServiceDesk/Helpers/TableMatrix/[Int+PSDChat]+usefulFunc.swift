import Foundation
extension Array where Element == [PSDRowMessage]{
    mutating func removeMessage(at indexPath: IndexPath){
        if self.count > indexPath.section && self[indexPath.section].count > indexPath.row{
            self[indexPath.section].remove(at: indexPath.row)
        }
        
    }
    ///Create new matrix with no elements or with welcome message if it was setted.
    mutating func defaultMatrix(){
        if CustomizationHelper.welcomeMessage.count>0
        {
            self = [[PSDObjectsCreator.createWelcomeMessage()]]
        }
        else{
            self = [[PSDRowMessage]()]
        }
    }
    ///Find indexPath of message by its local id start from bottom.
    func findIndexPath(ofMessage localId:String)-> [IndexPath: PSDRowMessage]{
        var indexPaths = [IndexPath:PSDRowMessage]()
        for (section,messages) in self.enumerated().reversed(){
            for (row,message) in messages.enumerated().reversed(){
                if message.message.clientId.lowercased() == localId.lowercased()
                {
                    indexPaths[IndexPath.init(row: row, section: section)] = message
                }
            }
        }
        return indexPaths
    }
    ///Return last index path
    func indexPathsAfterSent(for message: PSDMessage) -> [IndexPath]?{
        guard self.count > 0 else {
            return nil
        }
        let lastSection = count - 1
        let lastSectionDate = self[lastSection][0].message.date
        var firstRow = 0
        var section = lastSection + 1
        if lastSectionDate.compareWithoutTime(with: message.date) == .equal{
            firstRow = self[lastSection].count - 1
            section = lastSection
        }
        var indexPaths = [IndexPath]()
        for i in 0...PSDObjectsCreator.rowMessagesCount(for: message)-1{
            indexPaths.append(IndexPath(row: firstRow + i, section: section))
        }
        return indexPaths
    }
    ///Creates table matrix in format [[PSDMessage]] where in every index are messages with same date
    mutating func create(from chat : PSDChat)
    {
        let unsentMessages : [[PSDRowMessage]] = self
        var section: Int = 0
        var previousMessage : PSDMessage? = nil
        self.defaultMatrix()
        if !self.isEmpty && self[0].count>0 && chat.messages.count>0{//change date of welcome message. Make it same as first message in chat
            self[0][0].message.date = chat.messages[0].date
        }
        for message in chat.messages{
            if previousMessage == nil{
                self[section].append(contentsOf: PSDObjectsCreator.parseMessageToRowMessage(message))
            }
            else if message.date.compareWithoutTime(with: previousMessage!.date)  == .equal{
                self[section].append(contentsOf: PSDObjectsCreator.parseMessageToRowMessage(message))
            }
            else{
                section = section + 1
                self.insert(PSDObjectsCreator.parseMessageToRowMessage(message), at: section)
                
            }
            previousMessage = message
        }
        let comp = self.completeWithMessages(unsentMessages[0])//if user has had time to type new messages - add them to bottom
        if(comp){
            self.completeWithUnsentMessages()///if user has unsent messages add them to bottom
        }
        
    }
    ///Detect if need show avatar. If current user is same as next don't show avatar
    func needShowAvatar(at indexPath: IndexPath)->Bool{
        if indexPath.row == 0 && indexPath.section == 0 &&  CustomizationHelper.welcomeMessage.count>0 {
            return false//if first message is welcome - dont show avatar
        }
        let currentUser = personForMessage(at: IndexPath.init(row: indexPath.row, section: indexPath.section))
        let nextUser = personForMessage(at: IndexPath.init(row: indexPath.row+1, section: indexPath.section))
        if currentUser as? PSDPlaceholderUser != nil{
            let previousUser = personForMessage(at: IndexPath.init(row: indexPath.row-1, section: indexPath.section))
            if let previousUser = previousUser, previousUser.personId != PyrusServiceDesk.userId{
                return false
            }else{
                return true
            }
        }
        if(!(currentUser?.equalTo(user:nextUser) ?? false) || nextUser == nil){
            return true
        }
        return false
    }
    ///If current user is same as previous don't show name
    func needShowName(at indexPath: IndexPath)->Bool{
        let previousUser = personForMessage(at: IndexPath.init(row: indexPath.row-1, section: indexPath.section))
        let currentUser = personForMessage(at: IndexPath.init(row: indexPath.row, section: indexPath.section))
        if currentUser as? PSDPlaceholderUser != nil{
            if let previousUser = previousUser, previousUser.personId != PyrusServiceDesk.userId{
                return false
            }else{
                return true
            }
        }
        if(!(currentUser?.equalTo(user:previousUser) ?? false) || previousUser == nil){
            return true
        }
        return false
    }
    ///Find ower for massage at IndexPath. If tableMatrix has info - return it, or nil if not.
    private func personForMessage(at indexPath: IndexPath)->PSDUser?{
        if(indexPath.row>=0 && count>indexPath.section && self[indexPath.section].count > indexPath.row){
            return self[indexPath.section][indexPath.row].message.owner
        }
        return nil
    }
    ///Returns first date in section
    ///- parameter section: section wich date need to be retrn
    func date(of section:Int)->Date?
    {
        if count > section &&  self[section].count > 0
        {
            let rowMessage = self[section][0]
            return rowMessage.message.date
        }
        return nil
    }
    
    func has(indexPath:IndexPath)->Bool{
        if self.count > indexPath.section && self[indexPath.section].count > indexPath.row{
            return true
        }
        return false
    }
    ///Returns the date of last  message from user
    func lastUserMessageDate() -> Date? {
        for messageByDate in self.reversed(){
            for messageRow in messageByDate.reversed(){
                if messageRow.message.owner.personId == PyrusServiceDesk.userId && messageRow.hasId(){
                    return messageRow.message.date
                }
            }
        }
        return nil
    }
}

