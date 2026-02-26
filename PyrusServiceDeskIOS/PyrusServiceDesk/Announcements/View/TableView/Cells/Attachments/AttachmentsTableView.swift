import UIKit

// MARK: - Model

public struct AttachmentItem: Hashable {
    public let id: UUID
    public let fileName: String
    public let sizeText: String

    public init(id: UUID = .init(), fileName: String, sizeText: String) {
        self.id = id
        self.fileName = fileName
        self.sizeText = sizeText
    }
}

// MARK: - Table

public final class AttachmentsTableView: UITableView {

    // Публичные события
    var onSelectItem: ((AnnouncementCellAttachmentModel) -> Void)?
    var onTapIcon: ((AnnouncementCellAttachmentModel) -> Void)?

    // Данные
    private var items: [AnnouncementCellAttachmentModel] = []

    public override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: style)
        commonInit()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        backgroundColor = .clear
        separatorStyle = .none
        estimatedRowHeight = 54
        isScrollEnabled = false

        register(FileAttachmentCell.self, forCellReuseIdentifier: FileAttachmentCell.reuseID)

        dataSource = self
        delegate = self
    }

    func update(items: [AnnouncementCellAttachmentModel]) {
        self.items = items
        reloadData()
    }
}

// MARK: - DataSource & Delegate

extension AttachmentsTableView: UITableViewDataSource, UITableViewDelegate {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        items.count
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: FileAttachmentCell.reuseID, for: indexPath) as! FileAttachmentCell
        let item = items[indexPath.row]
        cell.configure(with: item)
        cell.onTapIcon = { [weak self] in
            self?.onTapIcon?(item)
        }
        return cell
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        onSelectItem?(items[indexPath.row])
    }
    
    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 54
    }
}
