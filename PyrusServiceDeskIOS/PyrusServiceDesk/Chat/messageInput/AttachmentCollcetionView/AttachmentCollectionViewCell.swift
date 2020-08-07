//
//  AttachmentCollectionViewCell.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 20.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
protocol AttachmentCollectionViewCellDelegate : NSObjectProtocol{
    func removePressed(for cell:AttachmentCollectionViewCell)
}
class AttachmentCollectionViewCell : UICollectionViewCell{
    weak var delegate : AttachmentCollectionViewCellDelegate?
    private static let buttonSize : CGFloat = 15
    private static let buttonImageName = "delete"
    static let distToBoard : CGFloat = 7
    private static let holderRadius : CGFloat = 6.0
    private static let holderBorderWidth : CGFloat = 1.0
    private lazy var removeButton : UIButton = {
        let button = UIButton()
        button.backgroundColor = #colorLiteral(red: 0.9490196078, green: 0.9490196078, blue: 0.968627451, alpha: 1)
        button.setImage(UIImage.PSDImage(name: AttachmentCollectionViewCell.buttonImageName), for: .normal)
        button.layer.cornerRadius = AttachmentCollectionViewCell.buttonSize/2
        return button
    }()
    lazy var holderView : UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        view.layer.cornerRadius = AttachmentCollectionViewCell.holderRadius
        view.layer.borderWidth = AttachmentCollectionViewCell.holderBorderWidth
        view.layer.borderColor = UIColor.psdLightGray.cgColor
        view.clipsToBounds = true
        return view
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
        addSubview(holderView)
        addSubview(removeButton)
        removeButton.translatesAutoresizingMaskIntoConstraints = false
        holderView.translatesAutoresizingMaskIntoConstraints = false
        
        holderView.leftAnchor.constraint(equalTo: leftAnchor, constant: AttachmentCollectionViewCell.distToBoard).isActive = true
        holderView.rightAnchor.constraint(equalTo: rightAnchor, constant: -AttachmentCollectionViewCell.distToBoard).isActive = true
        holderView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -AttachmentCollectionViewCell.distToBoard).isActive = true
        holderView.topAnchor.constraint(equalTo: removeButton.topAnchor, constant: AttachmentCollectionViewCell.distToBoard).isActive = true
        
        removeButton.rightAnchor.constraint(equalTo: rightAnchor, constant: 0).isActive = true
        removeButton.topAnchor.constraint(equalTo: topAnchor, constant: DEFAULT_LAYOUT_MARGINS).isActive = true
        removeButton.widthAnchor.constraint(equalToConstant: AttachmentCollectionViewCell.buttonSize).isActive = true
        removeButton.heightAnchor.constraint(equalToConstant: AttachmentCollectionViewCell.buttonSize).isActive = true
        
        removeButton.addTarget(self, action: #selector(removeButtonPressed), for: .touchUpInside)
    }
    @objc private func removeButtonPressed(){
        delegate?.removePressed(for: self)
    }
}
