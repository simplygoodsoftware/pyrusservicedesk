protocol RateViewProtocol: NSObjectProtocol, UIView {
    var tapDelegate: PSDRateViewDelegate? { get set }
    func configure(with rateValues: [RatingTextValue])
}

class PSDEmojiRateView: UIView, RateViewProtocol {
    weak var tapDelegate: PSDRateViewDelegate?
    private var buttons: [RatingTextValue]?
    private var maxWidth: CGFloat = 0
    
    ///В Liquid Glass ячейки вдвое уже: смайлики стоят плотнее, и обнимающее их
    ///стекло с полями не упирается в края экрана.
    private var cellWidthDivider: CGFloat { PSDLiquidGlassStyle.isEnabled ? 2 : 1 }
    
    ///Стеклянная капсула под рядом эмодзи. Используется только в Liquid Glass оформлении.
    ///Позиционируется по фактическим фреймам ячеек, поэтому обнимает контент,
    ///сколько бы оценок ни настроил интегратор.
    private lazy var glassBackground = PSDGlassBackgroundView(shape: .rounded(radius: GlassLayout.cornerRadius))
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
        if PSDLiquidGlassStyle.isEnabled {
            insertSubview(glassBackground, belowSubview: collectionView)
            glassBackground.isHidden = true
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        //super обязателен: именно он применяет констрейнты к сабвью. Без него коллекция
        //не получала фрейм, не создавала ячейки до случайного внешнего layoutIfNeeded,
        //и стекло, считающее себя по её атрибутам, появлялось с непредсказуемой задержкой.
        super.layoutSubviews()
        PSDRateDebug.log("emojiRate layout h=\(frame.height) cells=\(collectionView.visibleCells.count)") //ВРЕМЕННО
        maxWidth = ((superview?.frame.width ?? 0) / 5 - ROWS_SPACING - 3) / cellWidthDivider
        updateGlassBackgroundFrame()
    }
    
    ///Стекло обнимает ряд эмодзи. Фрейм считается из модели — число значений,
    ///ширина ячейки, интервал — а не из атрибутов layout: ячейки самосайзящиеся
    ///(estimatedItemSize 1×1), их атрибуты стабилизируются за несколько проходов,
    ///и привязанное к ним стекло мигало вместе с ними, дёргая layout всей панели.
    ///Модель же известна синхронно, фрейм стабилен с первого прохода.
    private func updateGlassBackgroundFrame() {
        guard PSDLiquidGlassStyle.isEnabled else { return }
        guard let count = buttons?.count, count > 0, bounds.width > 0, bounds.height > 0, maxWidth > 0 else {
            glassBackground.isHidden = true
            return
        }
        
        let rowWidth = CGFloat(count) * maxWidth + CGFloat(count - 1) * ROWS_SPACING
        //Коллекция развёрнута на 180° вместе с таблицей: её контент прижат
        //к нижней кромке, над нижним отступом.
        let rowBottom = bounds.height - GlassLayout.collectionBottomInset
        let rowFrame = CGRect(x: (bounds.width - rowWidth) / 2,
                              y: rowBottom - GlassLayout.cellHeight,
                              width: rowWidth,
                              height: GlassLayout.cellHeight)
        let glassFrame = rowFrame.insetBy(dx: -GlassLayout.horizontalPadding,
                                          dy: -GlassLayout.verticalPadding)
        
        glassBackground.isHidden = false
        //Лишние присваивания фрейма будят Core Animation — ставим только при изменении.
        if glassBackground.frame != glassFrame {
            glassBackground.frame = glassFrame
        }
    }
    
    func configure(with rateValues: [RatingTextValue]) {
        //Команда показа рейтинга приходит повторно вместе с обновлениями чата.
        //Если значения не изменились, пересборка самосайзящихся ячеек только мигает
        //ими и дёргает layout всей панели — пропускаем.
        if let current = buttons,
           current.count == rateValues.count,
           zip(current, rateValues).allSatisfy({ $0.rating == $1.rating && $0.text == $1.text }) {
            return
        }
        rateArray = rateValues
        self.buttons = rateValues
        maxWidth = rateValues.count > 3 ? maxWidth : 82 / cellWidthDivider
        collectionView.reloadData()
        setNeedsLayout()
        layoutIfNeeded()
        collectionView.collectionViewLayout.invalidateLayout()
        superview?.setNeedsLayout()
        superview?.layoutIfNeeded()
        updateGlassBackgroundFrame()
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
    ///Геометрия стеклянной капсулы под эмодзи.
    enum GlassLayout {
        static let horizontalPadding: CGFloat = 16
        static let verticalPadding: CGFloat = 8
        ///Радиус: ряд (ячейка 40 + 2×8) даёт капсулу.
        static let cornerRadius: CGFloat = 28
        ///Высота ячейки. Совпадает с MIN_HEIGHT в RateViewCell.
        static let cellHeight: CGFloat = 40
        ///Нижний отступ коллекции — тот же, что в констрейнте её bottom в init.
        static let collectionBottomInset: CGFloat = 12
    }
    
    var CELL_IDENT: String { "RateViewCell" }
    var LEADING_FOOTER: CGFloat { TO_BOARD_DISTANCE + (AVATAR_SIZE * 2) + 24 }
    var ROWS_SPACING: CGFloat { 20.0 }
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
