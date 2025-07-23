import UIKit

class LockView: UIButton {
    private lazy var lockImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.PSDImage(name: "lockAudio"))
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        return imageView
    }()
    
    private lazy var stopImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.PSDImage(name: "arrows"))
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.alpha = 0
        return imageView
    }()
    
    private var heightConstraint: NSLayoutConstraint?
    
    var isLockVisible: Bool = false {
        didSet {
            UIView.animate(withDuration: 0.2) {
                self.lockImageView.alpha = self.isLockVisible ? 1 : 0
                self.stopImageView.alpha = self.isLockVisible ? 1 : 0
                self.heightConstraint?.constant = self.isLockVisible ? 88 : 0
                self.layoutIfNeeded()
            }
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    private func setupView() {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = .psdBackground
        layer.cornerRadius = 22
        alpha = 0
        isUserInteractionEnabled = false
        
        // Настройка изображений
        lockImageView.translatesAutoresizingMaskIntoConstraints = false
        lockImageView.contentMode = .scaleAspectFit
        stopImageView.translatesAutoresizingMaskIntoConstraints = false
        stopImageView.contentMode = .scaleAspectFit
        
        addSubview(lockImageView)
        addSubview(stopImageView)
        
        NSLayoutConstraint.activate([
            widthAnchor.constraint(equalToConstant: 44),
            
            lockImageView.centerXAnchor.constraint(equalTo: centerXAnchor),
            lockImageView.topAnchor.constraint(equalTo: topAnchor, constant: 10),
            lockImageView.widthAnchor.constraint(equalToConstant: 24),
            lockImageView.heightAnchor.constraint(equalToConstant: 24),
            
            stopImageView.centerXAnchor.constraint(equalTo: centerXAnchor),
            stopImageView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10),
            stopImageView.widthAnchor.constraint(equalToConstant: 24),
            stopImageView.heightAnchor.constraint(equalToConstant: 24)
        ])
        
        heightConstraint = heightAnchor.constraint(equalToConstant: 0)
        heightConstraint?.isActive = true
        
        addTarget(self, action: #selector(tapLock), for: .touchUpInside)
    }
    
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        let isInside = super.point(inside: point, with: event)
        print("Point \(point) is inside: \(isInside)")
        return isInside
    }
    
    func showLock() {
        UIView.animate(withDuration: 0.2) {
            self.alpha = 1
            self.isLockVisible = true
        }
    }
    
    func lockRecord() {
        UIView.animate(withDuration: 0.2) {
            self.stopImageView.image = UIImage.PSDImage(name: "stopRecord")
            self.heightConstraint?.constant = 44
            self.layoutIfNeeded()
        }
        isUserInteractionEnabled = true
    }
    
    func hideLock() {
        UIView.animate(withDuration: 0.1, animations: {
            self.alpha = 0
            self.lockImageView.alpha = 0
            self.stopImageView.alpha = 0
           
            self.layoutIfNeeded()
        }, completion: {_ in 
            self.heightConstraint?.constant = 0
            self.stopImageView.image = UIImage.PSDImage(name: "arrows")
        })
    }
    
    @objc func tapLock() {
        print("stop")
    }
}

