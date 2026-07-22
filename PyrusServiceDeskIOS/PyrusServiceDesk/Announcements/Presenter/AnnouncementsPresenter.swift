import UIKit

final class AnnouncementsPresenter: NSObject {

    /// Построение view-моделей и атрибутированных строк — на фоне,
    /// чтобы не парсить rich text на главном потоке при каждом обновлении.
    private let prepareQueue = DispatchQueue(label: "com.pyrus.announcements.prepare", qos: .userInitiated)

    /// Кеш атрибутированных строк. Доступ только с prepareQueue.
    private var attributedTextCache = [String: CachedAttributedText]()

    weak var view: AnnouncementsViewProtocol?

    private struct CachedAttributedText {
        let contentHash: Int
        let text: NSAttributedString
    }
}

// MARK: - AnnouncementsPresenterProtocol

extension AnnouncementsPresenter: AnnouncementsPresenterProtocol {
    func doWork(_ action: AnnouncementsPresenterCommand) {
        switch action {
        case .updateAnnouncements(announcements: let announcements, lastReadIds: let lastReadIds):
            updateAnnouncements(announcements: announcements, lastReadIds: lastReadIds)
        case .endRefresh:
            view?.show(.endRefresh)
        case .updateTitle(title: let title):
            view?.show(.updateTitle(title: title ?? "Announcements".localizedPSD()))
        case .updateTitles(titles: let titles, selectedIndex: let selectedIndex):
            let titles = titles.map({ TitleWithBadge(title: $0) })
            view?.show(.updateTitle(title: "Announcements".localizedPSD()))
            view?.show(.updateTitles(titles: titles, selectedIndex: selectedIndex))
        case .updateSelected(index: let index):
            view?.show(.updateSelected(index: index))
        case .updateIcon(image: let image):
            view?.show(.updateIcon(image: image ?? UIImage.PSDImage(name: "iiko")))
        case .deleteSegmentControl:
            view?.show(.deleteSegmentControl)
        case .startRefresh:
            view?.show(.startRefresh)
        case .connectionError:
            view?.show(.connectionError)
        }
    }
}

// MARK: - Построение view-моделей

private extension AnnouncementsPresenter {

    /// Делит объявления каждого клиента на непрочитанные/прочитанные по lastReadId,
    /// строит атрибутированные строки на фоне и отдаёт результат на main.
    /// - Важно: массивы в `announcements` отсортированы «новые сначала» (по orderIndex).
    func updateAnnouncements(announcements: [String: [PSDAnnouncement]], lastReadIds: [String: String]) {
        // Снимаем зависимые от main данные до ухода на фон.
        let clientsById: [String: PSDClientInfo] = Dictionary(
            PyrusServiceDesk.clients.map { ($0.clientId, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        let unreadCountsById: [String: Int] = clientsById.mapValues { $0.announcementsUnreadCount }

        prepareQueue.async { [weak self] in
            guard let self else { return }

            var unreadAnnouncements: [PSDAnnouncement] = []
            var readAnnouncements: [PSDAnnouncement] = []

            for (appId, clientAnnouncements) in announcements {
                let lastReadId = lastReadIds[appId] ?? ""

                var isRead = true
                if !lastReadId.isEmpty {
                    if clientAnnouncements.contains(where: { $0.id == lastReadId }) {
                        isRead = false // идём от новых к старым, до lastReadId всё непрочитанное
                    } else {
                        // lastReadId не найден (объявление удалено или выпало из окна выгрузки).
                        // Фолбэк — серверный счётчик непрочитанных.
                        let unreadCount = unreadCountsById[appId] ?? 0
                        for (index, announcement) in clientAnnouncements.enumerated() {
                            var copy = announcement
                            copy.isRead = index >= unreadCount
                            copy.isRead ? readAnnouncements.append(copy) : unreadAnnouncements.append(copy)
                        }
                        continue
                    }
                }

                for announcement in clientAnnouncements {
                    if announcement.id == lastReadId {
                        isRead = true
                    }
                    var copy = announcement
                    copy.isRead = isRead
                    isRead ? readAnnouncements.append(copy) : unreadAnnouncements.append(copy)
                }
            }

            // Сортировка между клиентами — по дате; при равенстве — по orderIndex,
            // чтобы порядок не «прыгал» относительно внутриклиентской сортировки.
            let byRecency: (PSDAnnouncement, PSDAnnouncement) -> Bool = {
                $0.date != $1.date ? $0.date > $1.date : $0.orderIndex > $1.orderIndex
            }
            unreadAnnouncements.sort(by: byRecency)
            readAnnouncements.sort(by: byRecency)

            var items = [AnnouncementsViewModel]()
            items.reserveCapacity(unreadAnnouncements.count + readAnnouncements.count + 1)
            var seenIds = Set<String>()

            func appendModels(for announcements: [PSDAnnouncement]) {
                for announcement in announcements {
                    // Диффабл падает на дубликатах идентификаторов — защищаемся.
                    guard seenIds.insert(announcement.id).inserted else { continue }
                    let model = PSDAnnouncementCellModel(
                        announcement: announcement,
                        client: clientsById[announcement.appId],
                        attributedText: self.attributedText(for: announcement)
                    )
                    items.append(AnnouncementsViewModel(data: model, type: .announcement))
                }
            }

            appendModels(for: unreadAnnouncements)
            if !items.isEmpty {
                items.append(AnnouncementsViewModel(data: AnnouncementsReadModel(id: 0), type: .announcementsRead))
            }
            appendModels(for: readAnnouncements)

            self.trimCache(existingIds: seenIds)

            DispatchQueue.main.async { [weak self] in
                self?.view?.show(.updateAnnouncements(announcements: items))
            }
        }
    }

    /// Атрибутированная строка объявления: кеш по id + хешу контента.
    /// Вызывать только с prepareQueue.
    func attributedText(for announcement: PSDAnnouncement) -> NSAttributedString? {
        guard let content = announcement.content else { return nil }

        var hasher = Hasher()
        hasher.combine(content)
        let contentHash = hasher.finalize()

        if let cached = attributedTextCache[announcement.id], cached.contentHash == contentHash {
            return cached.text
        }

        let text = content.toAttributedString().addingPhoneNumberLinks()
        attributedTextCache[announcement.id] = CachedAttributedText(contentHash: contentHash, text: text)
        return text
    }

    /// Вызывать только с prepareQueue.
    func trimCache(existingIds: Set<String>) {
        attributedTextCache = attributedTextCache.filter { existingIds.contains($0.key) }
    }
}
