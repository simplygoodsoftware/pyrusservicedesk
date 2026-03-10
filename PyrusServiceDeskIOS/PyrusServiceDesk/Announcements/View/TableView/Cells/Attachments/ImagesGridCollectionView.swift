import UIKit

final class GridImageCell: UICollectionViewCell {
    static let reuseID = "GridImageCell"
    
    var isRead: Bool = false {
        didSet {
            imageView.backgroundColor = isRead ? .imagePreviewColor : .newImagePreviewColor
        }
    }

    let imageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFill
        iv.clipsToBounds = true
        iv.isUserInteractionEnabled = true
        return iv
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(imageView)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            imageView.topAnchor.constraint(equalTo: contentView.topAnchor),
            imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
        ])
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        imageView.image = nil
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
}

final class ImagesGridCollectionView: UICollectionView, UICollectionViewDataSource, UICollectionViewDelegate {

    var onSelectItem: ((AnnouncementCellAttachmentModel) -> Void)?

    weak var attachDelegate: AnnouncementsAttachmentsDelegate?
    private var images: [AnnouncementCellAttachmentModel] = []

    private var onePixel: CGFloat = 1

    init() {
        let layout = UICollectionViewCompositionalLayout { _, _ in
            return ImagesGridCollectionView.emptySectionLayout()
        }
        super.init(frame: .zero, collectionViewLayout: layout)
        backgroundColor = .systemBackground
        contentInsetAdjustmentBehavior = .always
        dataSource = self
        delegate = self

        register(GridImageCell.self, forCellWithReuseIdentifier: GridImageCell.reuseID)
    }

    required init?(coder: NSCoder) {
        let layout = UICollectionViewCompositionalLayout { _, _ in
            return ImagesGridCollectionView.emptySectionLayout()
        }
        super.init(coder: coder)
        collectionViewLayout = layout
        backgroundColor = .systemBackground
        contentInsetAdjustmentBehavior = .always
        dataSource = self
        delegate = self
        allowsSelection = true
        isUserInteractionEnabled = true
        register(GridImageCell.self, forCellWithReuseIdentifier: GridImageCell.reuseID)
    }

    func update(images: [AnnouncementCellAttachmentModel]) {
        self.images = images
        applyLayout(forCount: images.count)
        reloadData()
    }
    
    func loadImage(attach: PSDAnnouncementAttachment, indexPath: IndexPath, cell: GridImageCell) {
        Task {
            do {
                let data = try await AnnouncementAttachmentsRepository.shared.data(
                    for: attach.id,
                    authorId: PyrusServiceDesk.authorId ?? ""
                )
                
                if let image = UIImage(data: data),
                let indexPath = self.indexPath(for: cell),
                   images[indexPath.row].attachment == attach
                {
                    cell.imageView.image = image
                }
            } catch {
                print("Ошибка загрузки картинки объявления:", error)
            }
        }
    }

    // MARK: DataSource

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        images.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: GridImageCell.reuseID, for: indexPath) as? GridImageCell else {
            return UICollectionViewCell()
        }
        loadImage(attach: images[indexPath.item].attachment, indexPath: indexPath, cell: cell)
        cell.isRead = images[indexPath.item].isRead
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView,
                        didSelectItemAt indexPath: IndexPath) {
        onSelectItem?(images[indexPath.item])
    }

    // MARK: Layout builder

    private func applyLayout(forCount count: Int) {
        let spacing = onePixel

        func row(items: [NSCollectionLayoutItem], height: CGFloat) -> NSCollectionLayoutGroup {
            let size = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                              heightDimension: .absolute(height))
            let g = NSCollectionLayoutGroup.horizontal(layoutSize: size, subitems: items)
            g.interItemSpacing = .fixed(spacing)
            return g
        }
        
        func item(fractionalWidth: CGFloat) -> NSCollectionLayoutItem {
            let size = NSCollectionLayoutSize(widthDimension: .fractionalWidth(fractionalWidth),
                                              heightDimension: .fractionalHeight(1.0))
            let it = NSCollectionLayoutItem(layoutSize: size)
            it.contentInsets = .zero
            return it
        }
        
        func section(rows: [NSCollectionLayoutGroup]) -> NSCollectionLayoutSection {
            let totalH = rows.reduce(CGFloat(0)) { $0 + $1.layoutSize.heightDimension.dimension } + spacing * CGFloat(max(0, rows.count - 1))
            let vSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                               heightDimension: .absolute(totalH))
            let vGroup = NSCollectionLayoutGroup.vertical(layoutSize: vSize, subitems: rows)
            vGroup.interItemSpacing = .fixed(spacing)
            let sec = NSCollectionLayoutSection(group: vGroup)
            sec.interGroupSpacing = spacing
            sec.contentInsets = .zero
            return sec
        }

        let sectionLayout: NSCollectionLayoutSection

        switch count {
        case 0:
            sectionLayout = ImagesGridCollectionView.emptySectionLayout()
            
        case 1:
            // Одна ячейка на всю доступную высоту collectionView
                let spacing = onePixel

                // Элемент: на всю ширину и всю высоту группы
                let itemSize = NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .fractionalHeight(1.0)
                )
                let fullItem = NSCollectionLayoutItem(layoutSize: itemSize)
                fullItem.contentInsets = .zero

                // Горизонтальная группа: ширина = 100% секции, высота = 100% высоты контейнера (collectionView)
                let groupSize = NSCollectionLayoutSize(
                    widthDimension: .fractionalWidth(1.0),
                    heightDimension: .fractionalHeight(1.0)
                )
                let group = NSCollectionLayoutGroup.horizontal(layoutSize: groupSize, subitems: [fullItem])

                // Секция — одна группа
                let section = NSCollectionLayoutSection(group: group)
                section.interGroupSpacing = spacing
                section.contentInsets = .zero

                let layout = UICollectionViewCompositionalLayout(section: section)
                let cfg = UICollectionViewCompositionalLayoutConfiguration()
                cfg.interSectionSpacing = spacing
                layout.configuration = cfg

                setCollectionViewLayout(layout, animated: false)
            return
            
        case 2:
            sectionLayout = section(rows: [row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 217)])
            
        case 3:
            let spacing: CGFloat = 1   // толщина «перемычек»
            
            // Большая левая карточка
            let bigItemSize = NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(0.5),
                heightDimension: .fractionalHeight(1.0)
            )
            let bigItem = NSCollectionLayoutItem(layoutSize: bigItemSize)
            bigItem.contentInsets = .init(top: 0, leading: 0, bottom: 0, trailing: 0)
            
            // Маленькая карточка (используется 2 раза в правой колонке)
            let smallItemSize = NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(1.0),
                heightDimension: .fractionalHeight(0.5)
            )
            let smallItem = NSCollectionLayoutItem(layoutSize: smallItemSize)
            smallItem.contentInsets = .init(top: 0, leading: spacing, bottom: spacing/2, trailing: 0)
            
            let smallItem2 = NSCollectionLayoutItem(layoutSize: smallItemSize)
            smallItem2.contentInsets = .init(top: spacing/2, leading: spacing, bottom: 0, trailing: 0)
            
            // Правая вертикальная группа из двух маленьких
            let rightGroupSize = NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(0.5),
                heightDimension: .fractionalHeight(1.0)
            )
            let rightGroup = NSCollectionLayoutGroup.vertical(
                layoutSize: rightGroupSize,
                subitems: [smallItem, smallItem2]
            )
            
            // Контейнер: левая большая + правая группа
            let containerSize = NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(1.0),
                heightDimension: .absolute(333)
            )
            let container = NSCollectionLayoutGroup.horizontal(
                layoutSize: containerSize,
                subitems: [bigItem, rightGroup]
            )
            
            let section = NSCollectionLayoutSection(group: container)
            section.contentInsets = .init(top: spacing, leading: spacing, bottom: spacing, trailing: spacing)
            sectionLayout = section
            
        case 4:
            let h: CGFloat = 223.5
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: h),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: h),
            ])
            
        case 5:
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 111.25),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 111.25),
                row(items: [item(fractionalWidth: 1.0)], height: 223.5),
            ])
            
        case 6:
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 153),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 140),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: 153),
            ])
            
        case 7:
            let h: CGFloat = 167
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: h),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: h),
                row(items: [item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0)], height: h),
            ])
            
        case 8:
            let h: CGFloat = 167
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0)], height: h),
                row(items: [item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0), item(fractionalWidth: 1.0/3.0)], height: h),
                row(items: [item(fractionalWidth: 4.0/7.0), item(fractionalWidth: 3.0/7.0)], height: h),
            ])
            
        default:
            let h: CGFloat = 167
            sectionLayout = section(rows: [row(items: [item(fractionalWidth: 1.0/3.0),
                                                       item(fractionalWidth: 1.0/3.0),
                                                       item(fractionalWidth: 1.0/3.0)], height: h)])
        }

        let layout = UICollectionViewCompositionalLayout(section: sectionLayout)
        let cfg = UICollectionViewCompositionalLayoutConfiguration()
        cfg.interSectionSpacing = onePixel
        layout.configuration = cfg
        setCollectionViewLayout(layout, animated: false)
    }

    private static func emptySectionLayout() -> NSCollectionLayoutSection {
        let s = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0), heightDimension: .absolute(0.1))
        let it = NSCollectionLayoutItem(layoutSize: s)
        let g = NSCollectionLayoutGroup.horizontal(layoutSize: s, subitems: [it])
        return NSCollectionLayoutSection(group: g)
    }
}

private extension UIColor {
    static let imagePreviewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#3A3B3D") ?? .black
        default:
            return UIColor(hex: "#ECEDEF") ?? .systemGray
        }
    }
    
    static let newImagePreviewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#35354F") ?? .black
        default:
            return UIColor(hex: "#D1DFFA") ?? .systemGray
        }
    }
}
