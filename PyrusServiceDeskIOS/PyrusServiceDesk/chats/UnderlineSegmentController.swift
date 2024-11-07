import UIKit

protocol UnderlineSegmentControllerDelegate: NSObjectProtocol {
    func didSelectSegment(_ index: Int)
}
struct TitleWithBadge {
    var title: String
    var badge: Int?
}

class UnderlineSegmentController: UIView {
    var selectedIndex: Int = 0
    override func layoutSubviews() {
        super.layoutSubviews()
        selectionView.roundCorners(corners: [.topLeft, .topRight], radius: 2)
        if (scrollView.contentSize.width + scrollView.contentInset.left + scrollView.contentInset.right) > scrollView.frame.size.width {
            scrollView.isScrollEnabled = true
        } else {
            scrollView.isScrollEnabled = false
        }
    }
    
    public func selectIndex(_ selectedIndex: Int) {
        guard let sender = stackView.arrangedSubviews[selectedIndex] as? UIButton else {
            return
        }
        changeSelection(sender)
    }
    
    public func updateTitle(titles: [TitleWithBadge], selectIndex: Int) {
        stackView.removeAllArrangedSubviews()
        for (i,titleWithBadge) in titles.enumerated() {
            let btn = ButtonSegmentView()
            btn.delegate = self
            btn.updateTitle(titleWithBadge.title)
            stackView.addArrangedSubview(btn)
            btn.heightAnchor.constraint(equalTo: stackView.heightAnchor).isActive = true
            
            if i == selectIndex {
                changeSelection(btn)
            }
        }
        setNeedsLayout()
        layoutIfNeeded()
        scrollView.contentOffset = CGPoint(x: -21, y: 0)
    }
    
    public weak var delegate: UnderlineSegmentControllerDelegate?
    private var selectionCenter: NSLayoutConstraint?
    private var selectionWidth: NSLayoutConstraint?
    
    private let scrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.backgroundColor = .clear
        scrollView.contentInset = UIEdgeInsets(top: 0, left: 21, bottom: 0, right: 5)
        return scrollView
    }()
    
    private let stackView: UIStackView = {
    let stackV = UIStackView()
        stackV.axis = .horizontal
        stackV.alignment = .leading
        stackV.distribution = .fill
        if Locale.current.languageCode == "en" {
            stackV.spacing = 26
        } else {
            stackV.spacing = 20
        }
        stackV.translatesAutoresizingMaskIntoConstraints = false
        return stackV
    }()
    
    private let selectionView: UIView = {
       let selectionV = UIView()
        selectionV.backgroundColor = .mainTintColor
        selectionV.translatesAutoresizingMaskIntoConstraints = false
        return selectionV
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor(hex: "#F9F9F9F0")
        addSubview(scrollView)
        scrollView.addSubview(stackView)
        addSubview(selectionView)
        
        scrollView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        scrollView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        scrollView.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        scrollView.heightAnchor.constraint(equalToConstant: 32).isActive = true
        
        stackView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor).isActive = true
        stackView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor).isActive = true
        stackView.topAnchor.constraint(equalTo: scrollView.topAnchor).isActive = true
        stackView.heightAnchor.constraint(equalToConstant: 32).isActive = true
        
        selectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        selectionView.heightAnchor.constraint(equalToConstant: 2).isActive = true
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension UnderlineSegmentController: ButtonSegmentViewDelegate {
    @objc func changeSelection(_ sender: UIButton) {
       selectedIndex = -1
            var isSelected = false
           for (i,btn) in self.stackView.arrangedSubviews.enumerated() {
               if btn == sender, let button = sender as? ButtonSegmentView {
                   isSelected = true
                   if let selectionWidth = self.selectionWidth {
                       selectionWidth.isActive = false
                   }
                   if let selectionCenter = self.selectionCenter {
                       selectionCenter.isActive = false
                   }
                   self.selectedIndex = i
                   self.selectionWidth = self.selectionView.widthAnchor.constraint(equalTo: button.widthAnchor)
                   self.selectionWidth?.isActive = true
                   self.selectionCenter = self.selectionView.centerXAnchor.constraint(equalTo: sender.centerXAnchor)
                   self.selectionCenter?.isActive = true
               } else {
                   isSelected = false
               }
               if let button = btn as? UIButton {
                   button.setTitleColor(isSelected ? .mainTintColor : .psdGray, for: .normal)
               }
               if let button = btn as? ButtonSegmentView {
                   button.titleView.textColor = isSelected ? .mainTintColor : .psdGray
               }
           }
       UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseInOut) {
           self.layoutIfNeeded()
       } completion: { _ in
           
       }
       delegate?.didSelectSegment(selectedIndex)
    }
}

extension UIColor {
    static let mainTintColor = UIColor(red: 58.0 / 255.0, green: 160.0 / 255.0, blue: 162.0 / 255.0, alpha: 1)
}

@objc extension UIView {
    func roundCorners(corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: bounds,
                                byRoundingCorners: corners,
                                cornerRadii: CGSize(width: radius, height: radius))
        // Create the shape layer and set its path
        let maskLayer = CAShapeLayer()
        maskLayer.frame = bounds
        maskLayer.path = path.cgPath
        
        // Set the newly created shape layer as the mask for the view's layer
        self.layer.mask = maskLayer
    }
    
}

extension UIStackView {
    ///Удаляем все связанные View из UIStackView
    @objc func removeAllArrangedSubviews() {
        let removedSubviews = arrangedSubviews.reduce([]) { allSubviews, subview -> [UIView] in
            self.removeArrangedSubview(subview)
            return allSubviews + [subview]
        }
        // Deactivate all constraints
        NSLayoutConstraint.deactivate(removedSubviews.flatMap({ $0.constraints }))
        // Remove the views from self
        removedSubviews.forEach({ $0.removeFromSuperview() })
    }
}
