//
//  AttachmentCollectionViewCell.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 20.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
class AttachmentPreviewCollectionViewCell: AttachmentCollectionViewCell {
    var image : UIImage?{
        didSet{
            imageView.image = image
        }
    }
    private var imageView : UIImageView = UIImageView()
    override init(frame: CGRect) {
        super.init(frame: frame)
        customInit()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        customInit()
    }
    private func customInit(){
        holderView.addSubview(imageView)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.topAnchor.constraint(equalTo: holderView.topAnchor).isActive = true
        imageView.leftAnchor.constraint(equalTo: holderView.leftAnchor).isActive = true
        imageView.rightAnchor.constraint(equalTo: holderView.rightAnchor).isActive = true
        imageView.bottomAnchor.constraint(equalTo: holderView.bottomAnchor).isActive = true
        imageView.contentMode = .scaleAspectFill
    }
}
