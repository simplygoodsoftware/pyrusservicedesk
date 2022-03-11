//
//  InputAttachmentCollectionView.swift
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 20.04.2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
protocol AttachmentCollectionViewDelegateProtocol : NSObjectProtocol{
    func attachmentRemoved()
}
class AttachmentCollectionView : UICollectionView{
    weak var attachmentChangeDelegate : AttachmentCollectionViewDelegateProtocol?
    var presenter : AttachmentCollectionViewPresenterProtocol?
    override init(frame: CGRect, collectionViewLayout layout: UICollectionViewLayout) {
        super.init(frame: frame, collectionViewLayout: layout)
        backgroundColor = .clear
        customInit()
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        customInit()
    }
    private func customInit(){
        delegate = self
        dataSource = self
        register(AttachmentPreviewCollectionViewCell.self, forCellWithReuseIdentifier: AttachmentCollectionView.previewCellIdentifier)
        register(AttachmentFileCollectionViewCell.self, forCellWithReuseIdentifier: AttachmentCollectionView.fileCellIdentifier)
    }
    fileprivate static let previewCellIdentifier = "AttachPreviewCell"
    fileprivate static let fileCellIdentifier = "AttachFileCell"
    private func scrollToLastRow(){
        let curRow: Int = self.numberOfItems(inSection: 0)-1
        if curRow > 0{
            scrollToItem(at: IndexPath.init(row: curRow, section: 0), at: .right, animated: true)
        }
    }
}
extension AttachmentCollectionView: UICollectionViewDelegate, UICollectionViewDataSource{
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return presenter?.attachmentsNumber() ?? 0
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell : AttachmentCollectionViewCell?
        if presenter?.canHasPreviewAttachment(at: indexPath.row) ?? false{
            let previewCell = collectionView.dequeueReusableCell(withReuseIdentifier: AttachmentCollectionView.previewCellIdentifier, for: indexPath) as? AttachmentPreviewCollectionViewCell
            previewCell?.image = presenter?.imageForAttachment(at:indexPath.row)
            cell = previewCell
        }else{
            let fileCell = collectionView.dequeueReusableCell(withReuseIdentifier: AttachmentCollectionView.fileCellIdentifier, for: indexPath) as? AttachmentFileCollectionViewCell
            fileCell?.fileName = presenter?.nameForAttachment(at: indexPath.row) ?? ""
            cell = fileCell
        }
        cell?.delegate = self
        return cell ?? UICollectionViewCell()
    }
}
extension AttachmentCollectionView: AttachmentCollectionViewProtocol{
    func reloadCollection(){
        reloadData()
    }
    func addItem(at index: Int){
        let indexPath = IndexPath.init(row: index, section: 0)
        performBatchUpdates({
            self.insertItems(at: [indexPath])
        }, completion: { _ in
            self.scrollToLastRow()
        })
    }
    func removeItem(at index: Int){
        let indexPath = IndexPath.init(row: index, section: 0)
        performBatchUpdates({
            self.deleteItems(at: [indexPath])
        }, completion: nil)
    }
}
extension AttachmentCollectionView : AttachmentCollectionViewCellDelegate{
    func removePressed(for cell:AttachmentCollectionViewCell){
        if let ip = indexPath(for: cell){
            presenter?.removeAttachment(at:ip.row)
            attachmentChangeDelegate?.attachmentRemoved()
        }
    }
}
