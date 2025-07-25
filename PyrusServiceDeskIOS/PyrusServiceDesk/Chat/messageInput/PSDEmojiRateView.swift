protocol RateViewProtocol: NSObjectProtocol, UIView {
    var tapDelegate: PSDRateViewDelegate? { get set }
    func configure(with rateValues: [RatingTextValue])
}

class PSDEmojiRateView: UIView, RateViewProtocol {
    weak var tapDelegate: PSDRateViewDelegate?
    private var buttons: [RatingTextValue]?
    private var maxWidth: CGFloat = 0
    lazy var collectionView: UICollectionView = {
        let layout = CenterAlignedCollectionViewFlowLayout()
        if #available(iOS 10.0, *) {
            layout.estimatedItemSize = CGSize(width: 1, height: 1)
            layout.itemSize = UICollectionViewFlowLayout.automaticSize
        }
        layout.minimumLineSpacing = ROWS_SPACING
        layout.minimumInteritemSpacing = ROWS_SPACING
        layout.sectionInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0) // или кастомно
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.backgroundColor = .clear
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.register(RateViewCell.self, forCellWithReuseIdentifier: CELL_IDENT)
        collectionView.isScrollEnabled = false
        return collectionView
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(collectionView)
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: 0).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 0).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor, constant: 36).isActive = true
        //collectionView.heightAnchor.constraint(lessThanOrEqualToConstant: 136).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -12).isActive = true
        collectionView.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        maxWidth = (superview?.frame.width ?? 0) / 5 - ROWS_SPACING - 3
    }
    
    func configure(with rateValues: [RatingTextValue]) {
        self.buttons = rateValues
        maxWidth = rateValues.count > 3 ? maxWidth : 82
        collectionView.reloadData()
        setNeedsLayout()
        layoutIfNeeded()
        collectionView.collectionViewLayout.invalidateLayout()
        superview?.setNeedsLayout()
        superview?.layoutIfNeeded()
    }
}

extension PSDEmojiRateView: UICollectionViewDelegate, UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return buttons?.count ?? 0
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: CELL_IDENT, for: indexPath) as? RateViewCell
        else {
            return UICollectionViewCell()
        }
        guard
            let buttons = buttons,
            buttons.count > indexPath.row
        else {
            return cell
        }
        cell.text = buttons[indexPath.row].text
        cell.maxWidth = maxWidth//collectionView.frame.size.width
        cell.contentView.transform = CGAffineTransform(rotationAngle: CGFloat.pi)
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard
            let buttons = buttons,
            buttons.count > indexPath.row
        else {
            return
        }
        tapDelegate?.didTapRate(buttons[indexPath.row].rating)
    }
}
                            
private extension PSDEmojiRateView {
    var CELL_IDENT: String { "RateViewCell" }
    var LEADING_FOOTER: CGFloat { TO_BOARD_DISTANCE + (AVATAR_SIZE * 2) + 24 }
    var ROWS_SPACING: CGFloat { 8.0 }
}

class CenterAlignedCollectionViewFlowLayout: UICollectionViewFlowLayout {
    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        guard let attributes = super.layoutAttributesForElements(in: rect) else {
            return nil
        }

        let attributesCopy = attributes.map { $0.copy() as! UICollectionViewLayoutAttributes }

        var leftMargin = sectionInset.left
        var maxY: CGFloat = -1.0
        var rowAttributes: [UICollectionViewLayoutAttributes] = []

        for attr in attributesCopy {
            if attr.representedElementCategory != .cell {
                continue
            }

            if attr.frame.origin.y >= maxY {
                // new row starts
                centerRow(rowAttributes, collectionViewWidth: collectionView?.bounds.width ?? 0)
                rowAttributes.removeAll()
                leftMargin = sectionInset.left
            }

            attr.frame.origin.x = leftMargin
            leftMargin += attr.frame.width + minimumInteritemSpacing
            maxY = max(attr.frame.maxY, maxY)
            rowAttributes.append(attr)
        }

        // Центрируем последнюю строку
        centerRow(rowAttributes, collectionViewWidth: collectionView?.bounds.width ?? 0)

        return attributesCopy
    }

    private func centerRow(_ rowAttributes: [UICollectionViewLayoutAttributes], collectionViewWidth: CGFloat) {
        let totalWidth = rowAttributes.reduce(0) { $0 + $1.frame.width } +
                         CGFloat(max(rowAttributes.count - 1, 0)) * minimumInteritemSpacing

        let inset = max((collectionViewWidth - totalWidth) / 2, sectionInset.left)

        var currentX = inset
        for attr in rowAttributes {
            attr.frame.origin.x = currentX
            currentX += attr.frame.width + minimumInteritemSpacing
        }
    }
}
