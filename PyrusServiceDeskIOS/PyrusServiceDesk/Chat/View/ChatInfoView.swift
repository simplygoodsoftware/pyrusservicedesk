class ChatInfoView: UIView, UITableViewDataSource, UITableViewDelegate {
    var items = [String]()
    let tableView = UITableView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        design()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateItems(items: [String]) {
        self.items = items
        tableView.reloadData()
    }
    
    private func design() {
        backgroundColor = .clear
    
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.isScrollEnabled = false
        addSubview(tableView)
        tableView.backgroundColor = .clear
        tableView.separatorInset = .zero
        tableView.separatorStyle = .none
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: self.topAnchor, constant: 0),
            tableView.bottomAnchor.constraint(equalTo: self.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: self.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: self.trailingAnchor)
        ])
    }
    
    // MARK: - UITableViewDataSource
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        cell.textLabel?.text = items[indexPath.row]
        cell.textLabel?.numberOfLines = 2
        
        if indexPath.row == 0 {
            cell.accessoryView = UIImageView(image: UIImage(systemName: "doc.on.doc")?.imageWith(color: .label))
            cell.selectionStyle = .default
        } else {
            cell.accessoryView = nil
            cell.isUserInteractionEnabled = false
            cell.selectionStyle = .none
        }
        
        cell.backgroundColor = .navBarColor
        return cell
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    // MARK: - UITableViewDelegate
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let pasteboard = UIPasteboard.general
        pasteboard.string = items[indexPath.row]
    }
}
