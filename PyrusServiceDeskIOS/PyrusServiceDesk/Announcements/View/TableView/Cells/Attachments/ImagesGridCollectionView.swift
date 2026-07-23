import UIKit

// MARK: - Метрики грида
// Единый источник высот для ячейки объявления и layout-билдера грида —
// раньше значения были захардкожены в двух местах и могли разъехаться.

enum AnnouncementsImageGridMetrics {
    static let spacing: CGFloat = 1
    static let standardRowHeight: CGFloat = 167
    static let singleImageMaxHeightRatio: CGFloat = 0.6
    static let singleImageTopInset: CGFloat = 2
    static let imagesPerDefaultRow = 3

    static let pairRowHeight: CGFloat = 217
    static let tripleLayoutHeight: CGFloat = 333
    static let quadRowHeight: CGFloat = 223.5
    static let fiveSmallRowHeight: CGFloat = 111.25
    static let sixTopRowHeight: CGFloat = 153
    static let sixMiddleRowHeight: CGFloat = 140
    /// Дефолтная пропорция одиночного вложения, когда сервер не прислал
    /// размеры (типично для видео) — 16:9 от ширины грида.
    static let singleItemDefaultAspectRatio: CGFloat = 9.0 / 16.0

    /// Высоты рядов для заданного количества картинок (2+).
    static func rowHeights(forCount count: Int) -> [CGFloat] {
        switch count {
        case 2: return [pairRowHeight]
        case 3: return [tripleLayoutHeight]
        case 4: return [quadRowHeight, quadRowHeight]
        case 5: return [fiveSmallRowHeight, fiveSmallRowHeight, quadRowHeight]
        case 6: return [sixTopRowHeight, sixMiddleRowHeight, sixTopRowHeight]
        case 7, 8: return [standardRowHeight, standardRowHeight, standardRowHeight]
        default:
            let rows = max(1, Int(ceil(Double(count) / Double(imagesPerDefaultRow))))
            return Array(repeating: standardRowHeight, count: rows)
        }
    }

    /// Полная высота грида, которую резервирует ячейка объявления.
    static func totalHeight(
        forCount count: Int,
        singleImageAttachment: PSDAnnouncementAttachment?,
        gridWidth: CGFloat
    ) -> CGFloat {
        switch count {
        case 0:
            return 0
        case 1:
            let scaled = AnnouncementsHelper.scaledHeight(
                originalWidth: singleImageAttachment?.width ?? 0,
                originalHeight: singleImageAttachment?.height ?? 0,
                maxWidth: gridWidth
            )
            let height: CGFloat
            if scaled > 0 {
                let maxHeight = UIScreen.main.bounds.height * singleImageMaxHeightRatio
                height = min(scaled, maxHeight)
            } else {
                // scaledHeight вернул 0 — размеры неизвестны (обычно видео).
                height = gridWidth * singleItemDefaultAspectRatio
            }
            return singleImageTopInset + height
        default:
            let rows = rowHeights(forCount: count)
            return rows.reduce(0, +) + spacing * CGFloat(max(0, rows.count - 1))
        }
    }
}

// MARK: - Ячейка картинки

final class GridImageCell: UICollectionViewCell {
    static let reuseID = "GridImageCell"

    private enum Layout {
        static let imageFadeDuration: TimeInterval = 0.2
    }

    private var loadTask: Task<Void, Never>?
    /// Id вложения, которое сейчас показано или грузится.
    /// Повторный configure с тем же вложением не перезапускает загрузку —
    /// это источник моргания при любом reload с теми же данными.
    private var currentAttachmentId: String?

    /// Цвет плейсхолдера живёт на contentView, а не на imageView:
    /// фейд картинки идёт по альфе imageView, и плейсхолдер под ней
    /// должен оставаться видимым до конца анимации.
    private var isRead: Bool = false {
        didSet {
            contentView.backgroundColor = isRead ? .imagePreviewColor : .newImagePreviewColor
        }
    }

    private var displayScale: CGFloat {
        let scale = traitCollection.displayScale
        return scale > 0 ? scale : UIScreen.main.scale
    }

    let imageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFill
        imageView.clipsToBounds = true
        imageView.isUserInteractionEnabled = true
        return imageView
    }()

    /// Оверлей «Play» для видео-вложений (см. VideoPlayBadgeView).
    private let playBadgeView: VideoPlayBadgeView = {
        let view = VideoPlayBadgeView()
        view.isHidden = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(imageView)
        contentView.addSubview(playBadgeView)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            imageView.topAnchor.constraint(equalTo: contentView.topAnchor),
            imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
            playBadgeView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            playBadgeView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func configure(with model: AnnouncementCellAttachmentModel) {
        isRead = model.isRead

        let attachment = model.attachment
        playBadgeView.isHidden = !attachment.isVideo

        // То же вложение: картинка уже показана или грузится —
        // ничего не трогаем, любой reload проходит для глаза бесшовно.
        if attachment.id == currentAttachmentId, imageView.image != nil || loadTask != nil {
            return
        }

        currentAttachmentId = attachment.id
        loadTask?.cancel()
        loadTask = nil

        // Мгновенный путь: миниатюра уже декодирована ранее (в т.ч. префетчем) —
        // подставляем синхронно и без анимации, иначе кеш-хиты мерцали бы.
        if let cached = AnnouncementThumbnailCache.shared.image(for: attachment.id) {
            setImage(cached, animated: false)
            return
        }

        setImage(nil, animated: false)

        let scale = displayScale // читается на main, до ухода задачи на фон
        loadTask = Task { [weak self] in
            do {
                let data = try await AnnouncementAttachmentsRepository.shared.data(
                    for: attachment.id,
                    authorId: PyrusServiceDesk.authorId ?? ""
                )
                guard !Task.isCancelled else { return }
                // Декод — на фоне: UIImage(data:) декодирует при первом рендере
                // на главном потоке и дёргает скролл. Для видео — кадр ролика.
                let image: UIImage?
                if attachment.isVideo {
                    image = await AnnouncementImageDecoder.videoThumbnail(
                        from: data,
                        fileName: attachment.name,
                        maxDimension: AnnouncementImageDecoder.gridThumbnailMaxDimension,
                        scale: scale
                    )
                } else {
                    image = await AnnouncementImageDecoder.downsampledImage(
                        from: data,
                        maxDimension: AnnouncementImageDecoder.gridThumbnailMaxDimension,
                        scale: scale
                    )
                }
                guard !Task.isCancelled, self?.currentAttachmentId == attachment.id else { return }
                if let image {
                    AnnouncementThumbnailCache.shared.set(image, for: attachment.id)
                }
                // Загруженная картинка появляется с плавным фейдом.
                self?.setImage(image, animated: true)
            } catch is CancellationError {
            } catch {
                // Плейсхолдер (цвет фона) уже стоит. Сбрасываем id,
                // чтобы следующий configure сделал ретрай — но только если
                // ячейку не переиспользовали под другое вложение.
                if self?.currentAttachmentId == attachment.id {
                    self?.currentAttachmentId = nil
                }
            }
        }
    }

    /// Единая точка установки картинки.
    /// - animated == false: мгновенно (кеш, очистка под плейсхолдер).
    /// - animated == true: появление через альфу поверх плейсхолдера.
    private func setImage(_ image: UIImage?, animated: Bool) {
        imageView.layer.removeAllAnimations()
        imageView.image = image

        guard animated, image != nil else {
            imageView.alpha = 1
            return
        }

        imageView.alpha = 0
        UIView.animate(
            withDuration: Layout.imageFadeDuration,
            delay: 0,
            options: [.allowUserInteraction, .curveEaseOut]
        ) {
            self.imageView.alpha = 1
        }
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        loadTask?.cancel()
        loadTask = nil
        // Картинку и currentAttachmentId намеренно НЕ сбрасываем:
        // при reloadData ячейка почти всегда возвращается из пула на то же
        // вложение — показываем её мгновенно, без «моргания» плейсхолдером.
        // Если вложение другое, configure(with:) сам очистит imageView.
    }
}

// MARK: - Бейдж «Play»

/// Индикатор видео поверх превью. Параметры из дизайна:
/// круг 40×40, фон #000000 с прозрачностью 40% поверх background blur (12),
/// белый треугольник по центру.
///
/// Про блюр: UIKit не позволяет задать произвольный радиус, а «плотные» стили
/// (.regular и т.п.) несут собственную матовую подложку — в паре с чёрным 40%
/// блюр переставал читаться и круг выглядел плоским. Ближайший к макету
/// системный вариант — самый «прозрачный» тёмный материал
/// (.systemUltraThinMaterialDark): сквозь него видно смазанный фон,
/// а тёмность добирается лёгкой подложкой dimAlpha.
final class VideoPlayBadgeView: UIView {

    private enum Layout {
        static let size: CGFloat = 40
        /// Материал уже тёмный: суммарная тёмность с подложкой ≈ 40% из макета.
        static let dimAlpha: CGFloat = 0.1
        static let triangleSize = CGSize(width: 14, height: 16)
        static let triangleCornerRadius: CGFloat = 1.5
    }

    override var intrinsicContentSize: CGSize {
        CGSize(width: Layout.size, height: Layout.size)
    }

    init() {
        super.init(frame: CGRect(origin: .zero, size: CGSize(width: Layout.size, height: Layout.size)))
        isUserInteractionEnabled = false
        clipsToBounds = true
        layer.cornerRadius = Layout.size / 2

        let blurView = UIVisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterialDark))
        blurView.translatesAutoresizingMaskIntoConstraints = false

        let dimView = UIView()
        dimView.backgroundColor = UIColor.black.withAlphaComponent(Layout.dimAlpha)
        dimView.translatesAutoresizingMaskIntoConstraints = false

        let triangleView = PlayTriangleView(
            size: Layout.triangleSize,
            cornerRadius: Layout.triangleCornerRadius
        )
        triangleView.translatesAutoresizingMaskIntoConstraints = false

        addSubview(blurView)
        addSubview(dimView)
        addSubview(triangleView)

        NSLayoutConstraint.activate([
            widthAnchor.constraint(equalToConstant: Layout.size),
            heightAnchor.constraint(equalToConstant: Layout.size),

            blurView.topAnchor.constraint(equalTo: topAnchor),
            blurView.leadingAnchor.constraint(equalTo: leadingAnchor),
            blurView.trailingAnchor.constraint(equalTo: trailingAnchor),
            blurView.bottomAnchor.constraint(equalTo: bottomAnchor),

            dimView.topAnchor.constraint(equalTo: topAnchor),
            dimView.leadingAnchor.constraint(equalTo: leadingAnchor),
            dimView.trailingAnchor.constraint(equalTo: trailingAnchor),
            dimView.bottomAnchor.constraint(equalTo: bottomAnchor),

            // Строго геометрический центр круга.
            triangleView.centerXAnchor.constraint(equalTo: centerXAnchor, constant: 2),
            triangleView.centerYAnchor.constraint(equalTo: centerYAnchor),
            triangleView.widthAnchor.constraint(equalToConstant: Layout.triangleSize.width),
            triangleView.heightAnchor.constraint(equalToConstant: Layout.triangleSize.height),
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
}

/// Треугольник «Play» со скруглёнными углами, нарисованный вручную.
/// SF Symbol (play.fill) несёт в себе поля под шрифтовые метрики,
/// из-за чего в круге вставал не по геометрическому центру.
private final class PlayTriangleView: UIView {

    private let shapeSize: CGSize

    override var intrinsicContentSize: CGSize { shapeSize }

    init(size: CGSize, cornerRadius: CGFloat) {
        shapeSize = size
        super.init(frame: CGRect(origin: .zero, size: size))
        backgroundColor = .clear
        isOpaque = false

        // Скругление углов у залитого треугольника: путь строится с отступом
        // на радиус, а обводка той же белой краской с round-join возвращает
        // фигуре исходный габарит, скругляя вершины.
        let insetRect = CGRect(origin: .zero, size: size).insetBy(dx: cornerRadius, dy: cornerRadius)
        let path = UIBezierPath()
        path.move(to: CGPoint(x: insetRect.minX, y: insetRect.minY))
        path.addLine(to: CGPoint(x: insetRect.maxX, y: insetRect.midY))
        path.addLine(to: CGPoint(x: insetRect.minX, y: insetRect.maxY))
        path.close()

        let shapeLayer = CAShapeLayer()
        shapeLayer.path = path.cgPath
        shapeLayer.fillColor = UIColor.white.cgColor
        shapeLayer.strokeColor = UIColor.white.cgColor
        shapeLayer.lineWidth = cornerRadius * 2
        shapeLayer.lineJoin = .round
        layer.addSublayer(shapeLayer)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
}

// MARK: - Грид

final class ImagesGridCollectionView: UICollectionView, UICollectionViewDataSource, UICollectionViewDelegate {

    var onSelectItem: ((AnnouncementCellAttachmentModel) -> Void)?

    private var images: [AnnouncementCellAttachmentModel] = []
    /// Количество картинок, под которое построен текущий layout.
    /// Пересоздание compositional layout — дорогая операция, делаем только при изменении.
    private var currentLayoutCount: Int = -1

    init() {
        let layout = UICollectionViewCompositionalLayout { _, _ in
            ImagesGridCollectionView.emptySectionLayout()
        }
        super.init(frame: .zero, collectionViewLayout: layout)
        backgroundColor = .clear
        contentInsetAdjustmentBehavior = .never
        dataSource = self
        delegate = self
        register(GridImageCell.self, forCellWithReuseIdentifier: GridImageCell.reuseID)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func update(images: [AnnouncementCellAttachmentModel]) {
        let previous = self.images
        self.images = images

        if images.count != currentLayoutCount {
            applyLayout(forCount: images.count)
            currentLayoutCount = images.count
        }

        guard images != previous else { return }

        // Если состав вложений не изменился (поменялся только isRead, например
        // при reconfigure ячейки объявления) — обновляем видимые ячейки на месте.
        // reloadData здесь заставил бы каждую картинку пройти prepareForReuse
        // и перезагрузиться, что выглядит как моргание всей сетки.
        let sameAttachments = images.count == previous.count
            && zip(images, previous).allSatisfy { $0.attachment == $1.attachment }

        if sameAttachments {
            for indexPath in indexPathsForVisibleItems {
                guard
                    images.indices.contains(indexPath.item),
                    let cell = cellForItem(at: indexPath) as? GridImageCell
                else { continue }
                cell.configure(with: images[indexPath.item])
            }
        } else {
            reloadData()
        }
    }

    // MARK: DataSource

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        images.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: GridImageCell.reuseID, for: indexPath) as? GridImageCell,
            images.indices.contains(indexPath.item)
        else {
            return UICollectionViewCell()
        }
        cell.configure(with: images[indexPath.item])
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard images.indices.contains(indexPath.item) else { return }
        onSelectItem?(images[indexPath.item])
    }

    // MARK: Layout builder

    private func applyLayout(forCount count: Int) {
        let spacing = AnnouncementsImageGridMetrics.spacing

        func row(items: [NSCollectionLayoutItem], height: CGFloat) -> NSCollectionLayoutGroup {
            let size = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                              heightDimension: .absolute(height))
            let group = NSCollectionLayoutGroup.horizontal(layoutSize: size, subitems: items)
            group.interItemSpacing = .fixed(spacing)
            return group
        }

        func item(fractionalWidth: CGFloat) -> NSCollectionLayoutItem {
            let size = NSCollectionLayoutSize(widthDimension: .fractionalWidth(fractionalWidth),
                                              heightDimension: .fractionalHeight(1.0))
            let item = NSCollectionLayoutItem(layoutSize: size)
            item.contentInsets = .zero
            return item
        }

        func section(rows: [NSCollectionLayoutGroup]) -> NSCollectionLayoutSection {
            let totalHeight = rows.reduce(CGFloat(0)) { $0 + $1.layoutSize.heightDimension.dimension }
                + spacing * CGFloat(max(0, rows.count - 1))
            let verticalSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                                      heightDimension: .absolute(totalHeight))
            let verticalGroup = NSCollectionLayoutGroup.vertical(layoutSize: verticalSize, subitems: rows)
            verticalGroup.interItemSpacing = .fixed(spacing)
            let section = NSCollectionLayoutSection(group: verticalGroup)
            section.interGroupSpacing = spacing
            section.contentInsets = .zero
            return section
        }

        let heights = AnnouncementsImageGridMetrics.rowHeights(forCount: count)
        let sectionLayout: NSCollectionLayoutSection

        switch count {
        case 0:
            sectionLayout = ImagesGridCollectionView.emptySectionLayout()

        case 1:
            // Одна картинка занимает всю высоту, отведённую ячейкой.
            let itemSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                                  heightDimension: .fractionalHeight(1.0))
            let fullItem = NSCollectionLayoutItem(layoutSize: itemSize)
            fullItem.contentInsets = .zero
            let group = NSCollectionLayoutGroup.horizontal(layoutSize: itemSize, subitems: [fullItem])
            let section = NSCollectionLayoutSection(group: group)
            section.contentInsets = .zero
            sectionLayout = section

        case 2:
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: heights[0])
            ])

        case 3:
            // Большая слева + две маленькие справа.
            let bigItem = item(fractionalWidth: 0.5)

            let smallItemSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                                       heightDimension: .fractionalHeight(0.5))
            let smallTop = NSCollectionLayoutItem(layoutSize: smallItemSize)
            smallTop.contentInsets = .init(top: 0, leading: spacing, bottom: spacing / 2, trailing: 0)
            let smallBottom = NSCollectionLayoutItem(layoutSize: smallItemSize)
            smallBottom.contentInsets = .init(top: spacing / 2, leading: spacing, bottom: 0, trailing: 0)

            let rightGroupSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(0.5),
                                                        heightDimension: .fractionalHeight(1.0))
            let rightGroup = NSCollectionLayoutGroup.vertical(layoutSize: rightGroupSize,
                                                              subitems: [smallTop, smallBottom])

            let containerSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0),
                                                       heightDimension: .absolute(heights[0]))
            let container = NSCollectionLayoutGroup.horizontal(layoutSize: containerSize,
                                                               subitems: [bigItem, rightGroup])
            sectionLayout = NSCollectionLayoutSection(group: container)

        case 4, 5:
            var rows = heights.dropLast().map { height in
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: height)
            }
            if count == 4, let last = heights.last {
                rows.append(row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: last))
            } else if let last = heights.last {
                rows.append(row(items: [item(fractionalWidth: 1.0)], height: last))
            }
            sectionLayout = section(rows: Array(rows))

        case 6:
            sectionLayout = section(rows: heights.map { height in
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: height)
            })

        case 7:
            let third = 1.0 / 3.0
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: heights[0]),
                row(items: [item(fractionalWidth: 0.5), item(fractionalWidth: 0.5)], height: heights[1]),
                row(items: [item(fractionalWidth: third), item(fractionalWidth: third), item(fractionalWidth: third)], height: heights[2]),
            ])

        case 8:
            let third = 1.0 / 3.0
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: third), item(fractionalWidth: third), item(fractionalWidth: third)], height: heights[0]),
                row(items: [item(fractionalWidth: third), item(fractionalWidth: third), item(fractionalWidth: third)], height: heights[1]),
                row(items: [item(fractionalWidth: 4.0 / 7.0), item(fractionalWidth: 3.0 / 7.0)], height: heights[2]),
            ])

        default:
            // 9+ — ряды по 3; compositional layout повторит группу на все элементы,
            // а totalHeight(forCount:) резервирует высоту под все ряды.
            let third = 1.0 / 3.0
            sectionLayout = section(rows: [
                row(items: [item(fractionalWidth: third), item(fractionalWidth: third), item(fractionalWidth: third)],
                    height: AnnouncementsImageGridMetrics.standardRowHeight)
            ])
        }

        let layout = UICollectionViewCompositionalLayout(section: sectionLayout)
        let configuration = UICollectionViewCompositionalLayoutConfiguration()
        configuration.interSectionSpacing = spacing
        layout.configuration = configuration
        setCollectionViewLayout(layout, animated: false)
    }

    private static func emptySectionLayout() -> NSCollectionLayoutSection {
        let size = NSCollectionLayoutSize(widthDimension: .fractionalWidth(1.0), heightDimension: .absolute(0.1))
        let item = NSCollectionLayoutItem(layoutSize: size)
        let group = NSCollectionLayoutGroup.horizontal(layoutSize: size, subitems: [item])
        return NSCollectionLayoutSection(group: group)
    }
}

// MARK: - Цвета

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
