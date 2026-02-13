import UIKit

final class AvatarPileStackView: UIView {
    var itemSize: CGFloat = 16 { didSet { rebuild() } }
    var overlap: CGFloat = 8 { didSet { rebuild() } }          // перекрытие
    var maxVisible: Int = 6 { didSet { rebuild() } }
    var showsCounter = true

    private let stack = UIStackView()
    private var images: [UIImage] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        stack.axis = .horizontal
        stack.alignment = .center
        stack.distribution = .fill
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: topAnchor),
            stack.leadingAnchor.constraint(equalTo: leadingAnchor),
            stack.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: bottomAnchor),
            heightAnchor.constraint(equalToConstant: itemSize)
        ])
    }
    required init?(coder: NSCoder) { fatalError() }

    override var intrinsicContentSize: CGSize {
        let count = min(images.count, maxVisible) + (showsCounter && images.count > maxVisible ? 1 : 0)
        guard count > 0 else { return .zero }
        let width = itemSize + CGFloat(count - 1) * (itemSize - overlap)
        return CGSize(width: width, height: itemSize)
    }

    func set(images: [UIImage]) {
        self.images = images
        rebuild()
    }

    private func makeAvatarView(image: UIImage) -> UIImageView {
        let iv = UIImageView(image: image)
        iv.translatesAutoresizingMaskIntoConstraints = false
        iv.contentMode = .scaleAspectFill
        iv.clipsToBounds = true
        iv.layer.cornerRadius = itemSize/2
        NSLayoutConstraint.activate([
            iv.widthAnchor.constraint(equalToConstant: itemSize),
            iv.heightAnchor.constraint(equalToConstant: itemSize)
        ])
        return iv
    }

    private func makeCounterView(text: String) -> UILabel {
        let l = UILabel()
        l.translatesAutoresizingMaskIntoConstraints = false
        l.text = text
        l.font = .systemFont(ofSize: 12, weight: .semibold)
        l.textColor = .white
        l.textAlignment = .center
        l.backgroundColor = .tertiaryLabel
        l.clipsToBounds = true
        l.layer.cornerRadius = itemSize/2
        NSLayoutConstraint.activate([
            l.widthAnchor.constraint(equalToConstant: itemSize),
            l.heightAnchor.constraint(equalToConstant: itemSize)
        ])
        return l
    }

    private func rebuild() {
        stack.arrangedSubviews.forEach { $0.removeFromSuperview() }

        // глобальный отрицательный spacing для перекрытия
        stack.spacing = -(overlap)

        let visible = min(images.count, maxVisible)
        for i in 0..<visible {
            let v = makeAvatarView(image: images[i])
            stack.addArrangedSubview(v)
            // чуть увеличим zIndex, чтобы правые были «сверху»
            v.layer.zPosition = CGFloat(i)
        }

        if showsCounter, images.count > maxVisible {
            let counter = makeCounterView(text: "+\(images.count - maxVisible)")
            stack.addArrangedSubview(counter)
            counter.layer.zPosition = CGFloat(visible)
        }

        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }
}
