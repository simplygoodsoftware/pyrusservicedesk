import Foundation

protocol ButtonsCollectionDelegate: NSObjectProtocol {
    func didTapOnButton(_ text: String)
}

class ButtonsView: UIView {
    weak var tapDelegate: ButtonsCollectionDelegate?
    private var buttons: [String]?
    lazy var collectionView: UICollectionView = {
        let layout = AlignedCollectionViewFlowLayout()
        layout.horizontalAlignment = .right
        if #available(iOS 10.0, *) {
            layout.estimatedItemSize = CGSize(width: 1, height: 1)
            layout.itemSize = UICollectionViewFlowLayout.automaticSize
        }
        layout.minimumLineSpacing = ROWS_SPACING
        layout.minimumInteritemSpacing = ROWS_SPACING
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.register(ButtonViewCell.self, forCellWithReuseIdentifier: CELL_IDENT)
        collectionView.isScrollEnabled = false
        collectionView.semanticContentAttribute = UISemanticContentAttribute.forceRightToLeft
        return collectionView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(collectionView)
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: LEADING_FOOTER).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateWithButtons(_ buttons: [String]?) {
        self.buttons = buttons
        collectionView.reloadData()
        layoutIfNeeded()
        superview?.layoutIfNeeded()
    }
}

extension ButtonsView: UICollectionViewDelegate, UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        buttons?.count ?? 0
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: CELL_IDENT, for: indexPath) as? ButtonViewCell
        else {
            return UICollectionViewCell()
        }
        guard
            let buttons = buttons,
            buttons.count > indexPath.row
        else {
            return cell
        }
        cell.text = buttons[indexPath.row]
        cell.maxWidth = collectionView.frame.size.width
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard
            let buttons = buttons,
            buttons.count > indexPath.row
        else {
            return
        }
        tapDelegate?.didTapOnButton(buttons[indexPath.row])
    }
}
                            
private extension ButtonsView {
    var CELL_IDENT: String { "ButtonCell" }
    var LEADING_FOOTER: CGFloat { 18.0 }
    var ROWS_SPACING: CGFloat { 10.0 }
}
