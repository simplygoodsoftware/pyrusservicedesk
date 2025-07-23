class PopoverContentController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    let items: [String]
    let tableView = UITableView()
    
    init(ticketId: String, userName: String, createdAt: String) {
        items = [ticketId, userName, createdAt]
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .psdBackground
    
       
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(ChatInfoCell.self, forCellReuseIdentifier: ChatInfoCell.identifier)
        tableView.isScrollEnabled = false
        view.addSubview(tableView)
        tableView.backgroundColor = .clear
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updatePreferredContentSize()
    }
    
    private func updatePreferredContentSize() {
        tableView.layoutIfNeeded()
        
        let totalHeight = tableView.contentSize.height
        
        preferredContentSize = CGSize(
            width: 300,
            height: totalHeight - 0.5
        )
    }
    
    // MARK: - UITableViewDataSource
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: ChatInfoCell.identifier, for: indexPath) as? ChatInfoCell else {
            return UITableViewCell()
        }
        
        cell.configure(title: items[indexPath.row], canCopy: indexPath.row == 0)
        
        
        if indexPath.row == 0 {
            cell.selectionStyle = .default
        } else {
            cell.isUserInteractionEnabled = false
            cell.selectionStyle = .none
        }
        
        cell.backgroundColor = .clear
        return cell
    }

    // MARK: - UITableViewDelegate
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let pasteboard = UIPasteboard.general
        pasteboard.string = items[indexPath.row].filter({ $0.isNumber })
        dismiss(animated: true)
    }
}

class ChatInfoCell: UITableViewCell {
    static var identifier = "ChatInfoCell"
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .psdLabel
        label.font = .titleLabel
        label.numberOfLines = 0
        label.lineBreakMode = .byTruncatingTail
        return label;
    }()
    
    private let copyButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(systemName: "doc.on.doc")?.imageWith(color: .label), for: .normal)
        button.isHidden = true
        return button;
    }()
    
    func configure(title: String, canCopy: Bool) {
        titleLabel.text = title
        copyButton.isHidden = !canCopy
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
     //   backgroundColor = .psdBackground
     //   selectedBackgroundView = self.selectedBackground
        
        contentView.addSubview(titleLabel)
        contentView.addSubview(copyButton)
        
        addConstraints()
    }
    
    private lazy var selectedBackground : UIView = {
        let view = UIView()
        view.backgroundColor = .psdLightGray
        return view
    }()
    
    private func addConstraints() {
       // contentView.backgroundColor = .psdBackground
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        copyButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: contentView.trailingAnchor, constant: -16),
            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
            contentView.bottomAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 14),
            
            copyButton.heightAnchor.constraint(equalToConstant: 28),
            copyButton.widthAnchor.constraint(equalToConstant: 28),
            copyButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -12),
            copyButton.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
        ])
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        copyButton.isHidden = true
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private extension UIFont {
    static let titleLabel = CustomizationHelper.systemFont(ofSize: 17)
}
