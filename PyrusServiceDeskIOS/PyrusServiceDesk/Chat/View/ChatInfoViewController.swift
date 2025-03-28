//
//  ChatInfoViewController.swift
//  Helpy
//
//  Created by Станислава Бобрускина on 25.03.2025.
//  Copyright © 2025 Pyrus. All rights reserved.
//

class ChatInfoViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
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
        view.backgroundColor = .black.withAlphaComponent(0.3)
       
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.isScrollEnabled = false
        view.addSubview(tableView)
        tableView.backgroundColor = .clear
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
           // tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 0),
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
        tableView.topAnchor.constraint(equalTo: view.bottomAnchor, constant: -totalHeight).isActive = true
//        preferredContentSize = CGSize(
//            width: 300,
//            height: totalHeight - 0.5
//        )
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
        } else {
            cell.isUserInteractionEnabled = false
            cell.selectionStyle = .none
        }
        
        cell.backgroundColor = .clear
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
        dismiss(animated: true)
    }
}
