import Foundation
///The messages to feed in tableView. One PSDMessage can has several PSDRowMessage.
class PSDRowMessage: NSObject {
    
    //messages saved in PyrusServiceDeskCreator is not denited
    var text: String
    var rating: Int?
    var attachment: PSDAttachment?
    var message: PSDMessage
    var attributedText: NSAttributedString?
    init(message: PSDMessage, attachment: PSDAttachment?, text: String){
        self.message = message
        self.text = text
        let color = message.owner.personId == PyrusServiceDesk.userId ? CustomizationHelper.userMassageTextColor : CustomizationHelper.supportMassageTextColor
        attributedText = (text as NSString).parseXMLToAttributedString(fontColor: color).0
        self.attachment = attachment
        self.rating = message.rating
        super.init()
    }
    func updateWith(message : PSDMessage){
        self.message = message
    }
    func hasId() -> Bool {
        if(self.message.messageId != "0" && self.message.messageId != ""){
            return true
        }
        return false
    }
}
