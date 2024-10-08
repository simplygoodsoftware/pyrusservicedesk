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
        if
            chat.showRating,
            let ratingText = chat.showRatingText
        {
            _ = addRatingMessage(ratingText)
        }
        
    }
    ///Detect if need show avatar. If current user is same as next don't show avatar
    func needShowAvatar(at indexPath: IndexPath)->Bool{
        if emptyMessage(at: indexPath) {
            return false
        }
        if indexPath.row == 0 && indexPath.section == 0 &&  CustomizationHelper.welcomeMessage.count>0 {
            return true//if first message is welcome - do show avatar
        }
        let currentUser = personForMessage(at: IndexPath.init(row: indexPath.row, section: indexPath.section))
        let nextUser = personForMessage(at: nextNotEmpty(indexPath))
        if currentUser as? PSDPlaceholderUser != nil{
            let previousUser = personForMessage(at: previousNotEmpty(indexPath))
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
        if emptyMessage(at: indexPath) {
            return false
        }
        let previousUser = personForMessage(at: previousNotEmpty(indexPath))
        let currentUser = personForMessage(at: IndexPath.init(row: indexPath.row, section: indexPath.section))
        if
            let currentUser = currentUser,
            let name = currentUser.name,
            String.exceptionSupportName(name)
        {
            return false
        }
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
    
    ///Check if row message has empty data (ex.: that can be if support sent only buttons)
    func emptyMessage(at indexPath: IndexPath) -> Bool {
        if
            indexPath.row >= 0,
            count > indexPath.section,
            self[indexPath.section].count > indexPath.row
        {
            let rowMessage = self[indexPath.section][indexPath.row]
            return rowMessage.message.attachments?.count ?? 0 == 0 && rowMessage.attributedText?.string.count ?? 0 == 0
        }
        return false
    }
    
    private func previousNotEmpty(_ currentIP: IndexPath) -> IndexPath {
        return notEmptyIndexPath(currentIP, step: -1)
    }
    
    private func nextNotEmpty(_ currentIP: IndexPath) -> IndexPath {
        return notEmptyIndexPath(currentIP, step: 1)
    }
    
    private func notEmptyIndexPath(_ currentIP: IndexPath, step: Int) -> IndexPath {
        var row = currentIP.row
        var isEmpty = true
        while isEmpty {
            row = row + step
            isEmpty = emptyMessage(at: IndexPath(row: row, section: currentIP.section))
        }
        return IndexPath(row: row, section: currentIP.section)
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
    
    func lastMessage() -> PSDMessage? {
        let lastSection = count - 1
        guard lastSection >= 0 else {
            return nil
        }
        let lastRow = self[lastSection].count - 1
        guard lastRow >= 0 else {
            return nil
        }
        return self[lastSection][lastRow].message
    }
}

