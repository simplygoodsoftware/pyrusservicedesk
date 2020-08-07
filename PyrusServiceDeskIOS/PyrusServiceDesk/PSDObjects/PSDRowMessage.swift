//
//  PSDRowMessage.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 23.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
///The messages to feed in tableView. One PSDMessage can has several PSDRowMessage.
class PSDRowMessage: NSObject {
    //messages saved in PyrusServiceDeskCreator is not denited
    var text: String
    var rating : Int?
    var attachment: PSDAttachment?
    var message : PSDMessage
    init(message: PSDMessage, attachment: PSDAttachment?){
        self.message = message
        self.text = message.text
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
