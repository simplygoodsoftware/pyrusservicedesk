import UIKit

@objc final class BubleTextView: BubleView {
    private lazy var mainLabel: UILabel = {
        let label = UILabel()
        label.textAlignment = .natural
        label.font = .mainLabel
        label.textColor = .white
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    override func create(with view: UIView) {
        clipsToBounds = false
        let backView = UIView()
        backView.backgroundColor = .clear
        backView.addSubview(mainLabel)
        
        mainLabel.topAnchor.constraint(equalTo: backView.topAnchor, constant: MAIN_TO_SUPER).isActive = true
        mainLabel.leadingAnchor.constraint(equalTo: backView.leadingAnchor, constant: MAIN_TO_SUPER).isActive = true
        backView.trailingAnchor.constraint(greaterThanOrEqualTo: mainLabel.trailingAnchor, constant: MAIN_TO_SUPER).isActive = true
        backView.bottomAnchor.constraint(equalTo: mainLabel.bottomAnchor, constant: MAIN_TO_SUPER).isActive = true
        super.create(with: backView)
    }
    @objc func load(withAttributedTitle title: NSAttributedString, maxWidth: CGFloat, centerOfView: CGFloat, showOnTop: Bool) {
        let titleSize = title.boundingRect(with: textSizeMax(maxWidth),
                                           options: [.usesLineFragmentOrigin, .usesFontLeading],
                                           context: nil)
        mainLabel.attributedText = title
        loadWithTitleSize(titleSize,
                          maxWidth: maxWidth,
                          centerOfView: centerOfView,
                          showOnTop: showOnTop)
        
    }
    @objc func load(withTitle title: String, maxWidth: CGFloat, centerOfView: CGFloat, showOnTop: Bool) {
        let attributes: [NSAttributedString.Key: Any] = [.font: UIFont.mainLabel]
        let titleSize = (title as NSString).boundingRect(with: textSizeMax(maxWidth),
                                                            options: [.usesLineFragmentOrigin, .usesFontLeading],
                                                            attributes: attributes,
                                                            context: nil)
        mainLabel.text = title
        loadWithTitleSize(titleSize,
                          maxWidth: maxWidth,
                          centerOfView: centerOfView,
                          showOnTop: showOnTop)
    }
    private func loadWithTitleSize(_ titleSize: CGRect, maxWidth: CGFloat, centerOfView: CGFloat, showOnTop: Bool) {
        var frame = self.frame
        frame.size.height = ceil(titleSize.size.height) + 2 * HEIGHT_PADDING + 17
        if showOnTop {
            frame.origin.y = frame.origin.y - ceil(frame.size.height)
        } else {
            frame.origin.y = frame.origin.y - TOP_PADING
        }
        self.frame = frame
        let textSizeMax = textSizeMax(maxWidth)
        widthViewConstraint?.constant = min(textSizeMax.width + 2 * MAIN_TO_SUPER, ceil(titleSize.size.width) + 2 * MAIN_TO_SUPER)
        loadWith(centerOfView: centerOfView, showOnTop: showOnTop)
    }
    private func textSizeMax(_ maxWidth: CGFloat) -> CGSize {
        return CGSize(width: maxWidth - 2 * WIDTH_MARGIN - 2 * WIDTH_PADDING - 2 * MAIN_TO_SUPER, height: CGFloat.greatestFiniteMagnitude)
    }
}
//MARK: UI constants
private extension BubleTextView {
    var MAIN_TO_SUPER: CGFloat { 10 }
    var DETAIL_TOP: CGFloat { 2 }
    var DETAIL_BOTTOM: CGFloat { 7 }
    var WIDTH_MARGIN: CGFloat { 22 }
    var HEIGHT_PADDING: CGFloat { 13 }
    var WIDTH_PADDING: CGFloat { 16 }
    var TOP_PADING: CGFloat { 5 }
}

private extension UIFont {
    static let mainLabel = UIFont.systemFont(ofSize: 14)
    static let detailLabel = UIFont.systemFont(ofSize: 12)
}
private extension UIColor {
    static let detailLabel = #colorLiteral(red: 0.7233663201, green: 0.7233663201, blue: 0.7233663201, alpha: 1)
}

@objc protocol BubleViewDelegate: NSObjectProtocol {
    @objc func bubleWasRemoved()
}

@objc class BubleView: UIView {
    enum Style {
        case dark
        case userInterface
        
        var backgroundColor: UIColor {
            switch self {
            case .dark:
                return .red//.bubleBackColor
            case.userInterface:
                return .red//.bubleThemedBackColor
            }
        }
        
        var textColor: UIColor {
            switch self {
            case .dark:
                return .white
            case .userInterface:
                return .black//.pTextColor
            }
        }
        
        var hasShadow: Bool {
            switch self {
            case .dark:
                return false
            case .userInterface:
                return true
            }
        }
    }
    
    var cornerRadius: CGFloat = RADIUS {
        didSet {
            mainView.layer.cornerRadius = cornerRadius
        }
    }
    
    var shadowOpacity: Float = SHADOW_OPACITY {
        didSet {
            layer.shadowOpacity = shadowOpacity
        }
    }
    
    var style: Style = .dark {
        didSet {
            mainView.backgroundColor = style.backgroundColor
            triangleView.tintColor = style.backgroundColor
            
            clipsToBounds = false
            layer.shadowRadius = style.hasShadow ? SHADOW_RADIUS : 0
            layer.shadowOffset = .zero
            layer.shadowColor = UIColor.black.cgColor
            layer.shadowOpacity = shadowOpacity
        }
    }
    
    @objc weak var delegate: BubleViewDelegate?
    @objc private(set) lazy var closeButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "ic_remove"), for: .normal)
        button.imageView?.contentMode = .center
        button.setTitle(nil, for: .normal)
        button.addTarget(self, action: #selector(closeButtonPressed), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.widthAnchor.constraint(equalToConstant: CLOSE_BUTTON_WIDTH).isActive = true
        button.heightAnchor.constraint(equalToConstant: CLOSE_BUTTON_WIDTH).isActive = true
        return button
    }()
    private(set) lazy var mainView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = cornerRadius
        view.backgroundColor = style.backgroundColor
        view.translatesAutoresizingMaskIntoConstraints = false
        widthViewConstraint = view.widthAnchor.constraint(equalToConstant: DEFAULT_VIEW_WIDTH)
        widthViewConstraint?.isActive = true
        return view
    }()
    private(set) lazy var triangleView: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage(named: "triangle_dark.png")?.withRenderingMode(.alwaysTemplate)
        imageView.tintColor =  style.backgroundColor
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.widthAnchor.constraint(equalToConstant: TRIANGLE_SIZE.width).isActive = true
        imageView.heightAnchor.constraint(equalToConstant: TRIANGLE_SIZE.height).isActive = true
        return imageView
    }()
    @objc private(set) var centerTriangleConstraint: NSLayoutConstraint?
    private(set) var widthViewConstraint:  NSLayoutConstraint?
    private var topTriangleConstraint: NSLayoutConstraint?
    private var topMainViewConstraint: NSLayoutConstraint?
    private var bottomTriangleConstraint: NSLayoutConstraint?
    private var topTriangleToSuperViewConstraint: NSLayoutConstraint?
    
    func loadWith(centerOfView: CGFloat, showOnTop: Bool) {
        alpha = DISAPPEAR_ALPHA
        centerTriangleConstraint?.constant = centerOfView
        topMainViewConstraint?.isActive = showOnTop
        topTriangleConstraint?.isActive = showOnTop
        topTriangleToSuperViewConstraint?.isActive = !showOnTop
        bottomTriangleConstraint?.isActive = !showOnTop
        
        let imageName = showOnTop ? "triangle_dark.png" : "triangle_dark_rotate.png"
        let image = UIImage(named: imageName)?.withRenderingMode(.alwaysTemplate)
        triangleView.image = image
        setNeedsLayout()
        layoutIfNeeded()
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        create(with: UIView())
    }
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    func create(with view: UIView) {
        addSubview(mainView)
        topMainViewConstraint = mainView.topAnchor.constraint(equalTo: topAnchor,constant: -CLOSE_BUTTON_TOP )
        topMainViewConstraint?.isActive = true
        mainView.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: MAIN_VIEW_LEADING).isActive = true
        trailingAnchor.constraint(greaterThanOrEqualTo: mainView.trailingAnchor, constant: MAIN_VIEW_LEADING).isActive = true
        
        addSubview(triangleView)
        centerTriangleConstraint = triangleView.centerXAnchor.constraint(equalTo: centerXAnchor)
        centerTriangleConstraint?.isActive = true
        let centerTriangleToMainConstraint = triangleView.centerXAnchor.constraint(equalTo: mainView.centerXAnchor)
        centerTriangleToMainConstraint.priority = .defaultLow
        centerTriangleToMainConstraint.isActive = true
        topTriangleConstraint = triangleView.topAnchor.constraint(equalTo: mainView.bottomAnchor, constant: -MAIN_VIEW_TO_TRIANGLE)
        topTriangleConstraint?.isActive = true
        topTriangleToSuperViewConstraint = triangleView.topAnchor.constraint(equalTo: topAnchor, constant: MAIN_VIEW_TO_TRIANGLE)
        topTriangleToSuperViewConstraint?.isActive = false
        bottomTriangleConstraint = mainView.topAnchor.constraint(equalTo: triangleView.bottomAnchor, constant: -MAIN_VIEW_TO_TRIANGLE)
        bottomTriangleConstraint?.isActive = false
        
        mainView.addSubview(view)
        view.translatesAutoresizingMaskIntoConstraints = false
        mainView.pinEdges(to: view)
        mainView.clipsToBounds = false
        self.isUserInteractionEnabled = true
        addSubview(closeButton)
        closeButton.topAnchor.constraint(equalTo: mainView.topAnchor, constant: CLOSE_BUTTON_TOP).isActive = true
        closeButton.trailingAnchor.constraint(equalTo: mainView.trailingAnchor, constant: -CLOSE_BUTTON_TOP).isActive = true
    }
    @objc private func closeButtonPressed() {
        animateHide()
    }
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        return super.hitTest(point, with: event)
    }
    
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        var rect = bounds
        rect.origin.y = rect.origin.y - TOUCH_DIST
        rect.size.width = rect.size.width + (TOUCH_DIST * 2)
        rect.size.height = rect.size.height + (TOUCH_DIST * 2)
        guard rect.contains(point) else {
            return super.point(inside: point, with: event)
        }
        return true
    }
    @objc func animateHide() {
        UIView.animate(withDuration: ANIMATE_DURATION,
                       animations: {
                        self.alpha = self.DISAPPEAR_ALPHA
                       }, completion: {_ in
                        self.removeFromSuperview()
                        self.delegate?.bubleWasRemoved()
                       })
    }
    @objc func animateShow() {
        UIView.animate(withDuration: ANIMATE_DURATION,
                       animations: {
                        self.alpha = self.APPEAR_ALPHA
                       })
    }
    
}

extension BubleView {
    var SHADOW_RADIUS: CGFloat { 4.0 }
    static let SHADOW_OPACITY: Float = 0.4
    var ANIMATE_DURATION: Double { 0.1 }
    var APPEAR_ALPHA: CGFloat { 1 }
    var DISAPPEAR_ALPHA: CGFloat { 0 }
    var TOUCH_DIST: CGFloat { 10 }
    static let RADIUS: CGFloat = 8
    var CLOSE_BUTTON_WIDTH: CGFloat { 40 }
    var CLOSE_BUTTON_TOP: CGFloat { -17 }
    var DEFAULT_VIEW_WIDTH: CGFloat { 351 }
    var MAIN_VIEW_LEADING: CGFloat { 12 }
    var MAIN_VIEW_TO_TRIANGLE: CGFloat { 1 }
    var TRIANGLE_SIZE: CGSize { CGSize(width: 20, height: 10) }
}

@objc extension UIView {
    func pinEdges(to other: UIView, leadingOffset: CGFloat = 0, trailingOffset: CGFloat = 0, topOffset: CGFloat = 0, bottomOffset: CGFloat = 0, safeAreaLayoutGuide: Bool = false) {
        leadingAnchor.constraint(equalTo: safeAreaLayoutGuide ? other.safeAreaLayoutGuide.leadingAnchor : other.leadingAnchor, constant: leadingOffset).isActive = true
        trailingAnchor.constraint(equalTo: safeAreaLayoutGuide ? other.safeAreaLayoutGuide.trailingAnchor : other.trailingAnchor, constant: trailingOffset).isActive = true
        topAnchor.constraint(equalTo: safeAreaLayoutGuide ? other.safeAreaLayoutGuide.topAnchor : other.topAnchor, constant: topOffset).isActive = true
        bottomAnchor.constraint(equalTo: safeAreaLayoutGuide ? other.safeAreaLayoutGuide.bottomAnchor : other.bottomAnchor, constant: bottomOffset).isActive = true
    }
}
