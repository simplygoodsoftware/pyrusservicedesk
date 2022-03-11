//
//  File.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 20.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
protocol AttachmentCollectionViewPresenterProtocol : NSObjectProtocol{
    func attachmentsNumber() -> Int
    func attachmentsForSend()->[PSDAttachment]
    func canHasPreviewAttachment(at index:Int)->Bool
    func imageForAttachment(at index:Int)->UIImage?
    func nameForAttachment(at index:Int)->String
    func addAttachment(_ attachment : PSDAttachment)
    func removeAttachment(_ attachment : PSDAttachment)
    func removeAttachment(at index : Int)
    func cleanAll()
}
protocol AttachmentCollectionViewProtocol : NSObjectProtocol{
    func reloadCollection()
    func addItem(at index: Int)
    func removeItem(at index: Int)
    
}
class AttachmentCollectionViewPresenter: NSObject{
    private var attachments : [PSDAttachment]
    private weak var view : AttachmentCollectionViewProtocol?
    init(view: AttachmentCollectionViewProtocol, attachments:[PSDAttachment] = [PSDAttachment]()){
        self.attachments = attachments
        self.view = view
    }
}
extension AttachmentCollectionViewPresenter : AttachmentCollectionViewPresenterProtocol{
    func attachmentsForSend()->[PSDAttachment]{
        return attachments
    }
    func attachmentsNumber() -> Int{
        return attachments.count
    }
    func canHasPreviewAttachment(at index:Int)->Bool{
        if attachments.count <= index{
            return false
        }
        let attachment = attachments[index]
        return attachment.isImage
    }
    func imageForAttachment(at index:Int)->UIImage?{
        if attachments.count <= index{
            return nil
        }
        let attachment = attachments[index]
        return UIImage.init(data: attachment.data)
    }
    func nameForAttachment(at index:Int)->String{
        if attachments.count <= index{
            return ""
        }
        let attachment = attachments[index]
        return attachment.name
    }
    func addAttachment(_ attachment : PSDAttachment){
        attachments.append(attachment)
        
        if attachments.count > 1{
            view?.addItem(at: attachments.count - 1)
        }else{
            DispatchQueue.main.async {
                self.view?.reloadCollection()
            }
            
        }
    }
    func removeAttachment(at index : Int){
        if attachments.count <= index{
            return
        }
        attachments.remove(at: index)
        view?.removeItem(at: index)
    }
    func removeAttachment(_ attachment : PSDAttachment){
        for (i,att) in attachments.enumerated(){
            if attachment.isEqual(att){
                attachments.remove(at: i)
                 view?.removeItem(at: i)
            }
        }       
    }
    func cleanAll(){
        attachments.removeAll()
        view?.reloadCollection()
    }
}
