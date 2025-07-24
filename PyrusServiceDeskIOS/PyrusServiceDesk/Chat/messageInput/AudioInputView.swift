//
//  AudioInputView.swift
//  Helpy
//
//  Created by Станислава Бобрускина on 06.05.2025.
//  Copyright © 2025 Pyrus. All rights reserved.
//


import UIKit

class AudioInputView: UIView {
    private enum Constants {
        static let stateLabelAlpha: CGFloat = 0.6
        static let uploadString = "Uploading".localizedPSD()
        static var sizeString: String = " "
        static let stateLabelHeight: CGFloat = 22
        static let nameLabelHeight: CGFloat = 19
        static let distance: CGFloat = 10.0
    }
        
    var presenter: AudioPlayerPresenter?
    
    var state: AudioState = .stopped {
        didSet {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                switch state {
                case .loading:
                    break
                case .playing:
                    hasBeenTracked = false
                    playImageView.image = UIImage.PSDImage(name: "pause")?.imageWith(color: .white)
                case .paused:
                    if !hasBeenTracked {
                        playImageView.image = UIImage.PSDImage(name: "playIcon")?.imageWith(color: .white)
                    }
                case .stopped:
                    playImageView.image = UIImage.PSDImage(name: "playIcon")?.imageWith(color: .white)
                    if !slider.isTracking {
                        slider.setValue(0, animated: false)
                    }
                    presenter?.changeTime(0)
                case .needLoad:
                    break
                }
            }
        }
    }
    
    private lazy var playImageView = UIImageView(image: UIImage.PSDImage(name: "playIcon")?.imageWith(color: .white))

    private lazy var playView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 16
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .appColor
        return view
    }()
    
    private lazy var stateLabel: UILabel = {
        let label = UILabel()
        label.text = "00:00"
        label.font = .systemFont(ofSize: 12)
        label.textColor = .stateLabelColor
        return label
    }()
    
    var slider: UISlider = {
        let slider = AudioCellSlider()
        slider.minimumTrackTintColor = .appColor
        slider.maximumTrackTintColor = .trackColor
        slider.setThumbImage(UIImage.PSDImage(name: "darkCircle"), for: .normal)
        return slider
    }()
    
    private var stateWidthConstraint: NSLayoutConstraint?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
        setupConstraints()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
    }
    
    private func setupViews() {
        addSubview(stateLabel)
        addSubview(slider)
        addSubview(playView)
        layer.cornerRadius = 19
        backgroundColor = .audioBackgroundColor
    }
    
    private func setupConstraints() {
        stateLabel.translatesAutoresizingMaskIntoConstraints = false
        slider.translatesAutoresizingMaskIntoConstraints = false
        
        playImageView.translatesAutoresizingMaskIntoConstraints = false
        playView.addSubview(playImageView)
        
        NSLayoutConstraint.activate([
            playView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 3),
            playView.centerYAnchor.constraint(equalTo: centerYAnchor),
            playView.heightAnchor.constraint(equalToConstant: 32),
            playView.widthAnchor.constraint(equalToConstant: 32),
            playImageView.centerXAnchor.constraint(equalTo: playView.centerXAnchor),
            playImageView.centerYAnchor.constraint(equalTo: playView.centerYAnchor),
            playImageView.widthAnchor.constraint(equalToConstant: 16),
            playImageView.heightAnchor.constraint(equalToConstant: 16),
            
            heightAnchor.constraint(equalToConstant: 38),
            
            stateLabel.leadingAnchor.constraint(equalTo: slider.trailingAnchor, constant: 10),
            stateLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -8),
            stateLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            
            slider.leadingAnchor.constraint(equalTo: playView.trailingAnchor, constant: 10),
            slider.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -56),
            slider.centerYAnchor.constraint(equalTo: centerYAnchor),
            slider.heightAnchor.constraint(equalToConstant: 16)
        ])
                
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(buttonTapped))
        playView.addGestureRecognizer(tapGestureRecognizer)
        playView.isUserInteractionEnabled = true
        
        slider.addTarget(self, action: #selector(endDragging), for: .touchUpInside)
        slider.addTarget(self, action: #selector(endDragging), for: .touchUpOutside)
        slider.addTarget(self, action: #selector(startDragging), for: .touchDown)
    }
    
    @objc func endDragging() {
        if state == .playing {
            presenter?.startPlay(progress: slider.value)
        } else {
            presenter?.changeTime(CGFloat(slider.value))
        }
    }
    
    private var hasBeenTracked: Bool = false
    
    @objc func startDragging() {
        hasBeenTracked = true
    }
        
    @objc func buttonTapped() {
        presenter?.buttonWasPressed()
    }
    
    
    private func maxStateLabelWidth() -> CGFloat {
        let downloadWidth = Constants.uploadString.size(withAttributes: [.font: UIFont.stateFont]).width
        let attachmentSizeWidth = Constants.sizeString.size(withAttributes: [.font: UIFont.stateFont]).width
        return max(downloadWidth, attachmentSizeWidth)
    }
}

extension AudioInputView: AudioPlayerViewProtocol {
    func changeProgress(_ progress: CGFloat) { }
    
    func changeState(_ state: AudioState) {
        self.state = state
    }
    
    func playingProgress(_ progress: CGFloat) {
        if !slider.isTracking && state == .playing {// && (Float(progress) >= slider.value || progress == 0) {
            slider.setValue(Float(progress), animated: true)
        }
    }
    
    func setTime(string: String) {
        stateLabel.text = string
    }
    
    func changeLoadingProggress(_ progress: Float) {
        DispatchQueue.main.async { [weak self] in
            self?.playView.layer.updateLoadingProgress(progress)
        }
    }
}

private extension UIFont {
    static let nameFont = CustomizationHelper.systemFont(ofSize: 16.0)
    static let stateFont = CustomizationHelper.systemFont(ofSize: 14.0)
}

private extension UIColor {
    static let stateLabelColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#BFBFBF") ?? .white
        default:
            return UIColor(hex: "#666666") ?? .darkAppColor
        }
    }
    
    static let trackColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#BFBFBF")?.withAlphaComponent(0.3) ?? .white
        default:
            return UIColor(hex: "#D9D9D9") ?? .darkAppColor
        }
    }
    
    static let audioBackgroundColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#515254") ?? .white
        default:
            return UIColor(hex: "#F3F2F8") ?? .darkAppColor
        }
    }
}
