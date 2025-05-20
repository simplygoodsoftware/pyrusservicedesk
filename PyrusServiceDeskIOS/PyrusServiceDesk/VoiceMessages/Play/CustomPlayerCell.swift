//import Foundation
//@objc class CustomPlayerCellPresenter: NSObject{
//    private var fileUrl: URL?
//    func setFileUrl(url: URL?){
//        fileUrl = url
//    }
//    func stopPlay(){
//        if fileUrl != nil {
//            OpusPlayer.shared.pausePlay(fileUrl!)
//        }
//    }
//    func stopAllPlay(){
//        OpusPlayer.shared.stopAllPlay()
//    }
//}
//
/////The cell with custom player. Use this player for play formats that is not supported by system. Now can play oggOpus files.
//@objc class CustomPlayerCell: AttachmentCollectionViewCell {
//    private var presenter: CustomPlayerCellPresenter = CustomPlayerCellPresenter.init()
//    private lazy var viewForAudioAttachment = AudioAttachmentView.init()
//    
//    @objc func loadMediaCell(from path: String, attachmentId: String){
//        let url = URL.init(fileURLWithPath: path)
//        presenter.setFileUrl(url: url)
//        viewForAudioAttachment.create(with: url, attachmentId: attachmentId)
//        updateStatus()
//    }
//    
//    override init(frame: CGRect) {
//        super.init(frame: frame)
//        customInit()
//    }
//    
//    required init?(coder: NSCoder) {
//        fatalError("init(coder:) has not been implemented")
//    }
//
//    private func customInit(){
//        viewForAudioAttachment.needTime = true
//        viewForAudioAttachment.bigMode = true
//        viewForAudioAttachment.layer.borderWidth = 0.0
//        let gesture = UITapGestureRecognizer.init(target: self, action: #selector(onClick))
//        viewForAudioAttachment.addGestureRecognizer(gesture)
//        viewForAudioAttachment.isUserInteractionEnabled = true
//        holderView.addGestureRecognizer(gesture)
//        holderView.isUserInteractionEnabled = true
//        setupUI()
//    }
//    
//    private func setupUI() {
//        holderView.addSubview(viewForAudioAttachment)
//        viewForAudioAttachment.translatesAutoresizingMaskIntoConstraints = false
//        NSLayoutConstraint.activate([
//            viewForAudioAttachment.centerXAnchor.constraint(equalTo: holderView.centerXAnchor),
//            viewForAudioAttachment.centerYAnchor.constraint(equalTo: holderView.centerYAnchor),
//        ])
//    }
//    
//    @objc func cellDidDisapear(){
//        self.presenter.stopPlay()
//    }
//    @objc private func onClick(){
//        viewForAudioAttachment.action()
//    }
//    /*func updateProgress(){
//        
//    }*/
//    @objc func updateStatus(){
//        viewForAudioAttachment.drawCurrentState()
//    }
//}
