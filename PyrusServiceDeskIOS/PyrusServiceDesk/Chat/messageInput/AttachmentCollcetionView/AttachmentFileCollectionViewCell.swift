//
//  AttachmentFileCollectionViewCell.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 20.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
class AttachmentFileCollectionViewCell: AttachmentCollectionViewCell {
    private static let dist : CGFloat = 8.0
    private static let linesNumber : Int = 2
    var fileName : String = ""{
        didSet{
            label.text = fileName
        }
    }
    private static let imageName = "file2"
    private lazy var imageView : UIImageView = {
        let imageView = UIImageView()
        let image =  UIImage.PSDImage(name: AttachmentFileCollectionViewCell.imageName)?.withRenderingMode(.alwaysTemplate)
        imageView.image = image
        imageView.tintColor = UIColor.psdGray
        return imageView
    }()
    private lazy var label : UILabel = {
        let label = UILabel()
        label.textColor = UIColor.psdGray
        label.textAlignment = .center
        label.numberOfLines = AttachmentFileCollectionViewCell.linesNumber
        label.font = .label
        return label
    }()
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
        holderView.addSubview(label)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        label.translatesAutoresizingMaskIntoConstraints = false
        
        imageView.centerXAnchor.constraint(equalTo: holderView.centerXAnchor, constant: 0).isActive = true
        imageView.topAnchor.constraint(greaterThanOrEqualTo: holderView.topAnchor, constant: AttachmentFileCollectionViewCell.dist).isActive = true
        imageView.bottomAnchor.constraint(equalTo: label.topAnchor, constant: -AttachmentFileCollectionViewCell.dist/2).isActive = true
        
        label.leftAnchor.constraint(equalTo: holderView.leftAnchor, constant: AttachmentFileCollectionViewCell.dist).isActive = true
        label.rightAnchor.constraint(equalTo: holderView.rightAnchor, constant: -AttachmentFileCollectionViewCell.dist).isActive = true
        label.bottomAnchor.constraint(lessThanOrEqualTo: holderView.bottomAnchor, constant: -AttachmentFileCollectionViewCell.dist).isActive = true
    }
}
private extension UIFont {
    static let label = CustomizationHelper.systemFont(ofSize: 9)
}
