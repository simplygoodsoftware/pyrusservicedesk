//
//  PSDAnnouncementCell.swift
//  Helpy
//
//  Created by Станислава Бобрускина on 12.02.2026.
//  Copyright © 2026 Pyrus. All rights reserved.
//

import UIKit

final class PSDAnnouncementCell: UITableViewCell {
    static let identifier = "PSDAnnouncementCell"

    private let bubbleView = UIView()
    private let messageLabel = UILabel()
    private let timeLabel = UILabel()

    // Ограничим максимальную ширину «пузыря»
    private var bubbleMaxWidth: NSLayoutConstraint!

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .systemBackground

        // Bubble
        bubbleView.layer.cornerRadius = 16
        bubbleView.layer.masksToBounds = true

        // Message
        messageLabel.numberOfLines = 0
        messageLabel.font = .systemFont(ofSize: 15)
        messageLabel.adjustsFontForContentSizeCategory = true
        messageLabel.textColor = .label

        // Time
        timeLabel.font = .systemFont(ofSize: 13)
        timeLabel.adjustsFontForContentSizeCategory = true
        timeLabel.textColor = .secondaryLabel
        timeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        timeLabel.setContentHuggingPriority(.required, for: .horizontal)

        contentView.addSubview(bubbleView)
        bubbleView.addSubview(messageLabel)
        bubbleView.addSubview(timeLabel)
        bubbleView.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        timeLabel.translatesAutoresizingMaskIntoConstraints = false

//        // Внутренний стек: текст сверху, внизу — время, прижатое вправо
//        let bottomRow = UIStackView(arrangedSubviews: [UIView(), timeLabel])
//        bottomRow.axis = .horizontal
//        bottomRow.alignment = .firstBaseline
//        bottomRow.spacing = 8
//
//        let stack = UIStackView(arrangedSubviews: [messageLabel, bottomRow])
//        stack.axis = .vertical
//        stack.spacing = 8
//        stack.translatesAutoresizingMaskIntoConstraints = false
//        bubbleView.addSubview(stack)

        NSLayoutConstraint.activate([
            // Расположение пузыря (входящее — слева)
            bubbleView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            bubbleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 8),
            bubbleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -8),
            
            messageLabel.topAnchor.constraint(equalTo: bubbleView.topAnchor, constant: 10),
            messageLabel.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: 10),
            messageLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -10),
            
            timeLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -10),
            timeLabel.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 5),
            bubbleView.bottomAnchor.constraint(equalTo: timeLabel.bottomAnchor, constant: 10),
            
            contentView.bottomAnchor.constraint(equalTo: bubbleView.bottomAnchor)
//            bubbleView.bottomAnchor.constraint(equalTo: contentView.layoutMarginsGuide.bottomAnchor),

//            // Внутренние отступы пузыря
//            stack.topAnchor.constraint(equalTo: bubbleView.topAnchor, constant: 10),
//            stack.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: 10),
//            stack.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -10),
//            stack.bottomAnchor.constraint(equalTo: bubbleView.bottomAnchor, constant: -10),
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func configure(with vm: PSDAnnouncement) {
        messageLabel.text = vm.text
        timeLabel.text = vm.date.messageTime()

        bubbleView.backgroundColor = vm.isRead ? .bubbleViewColor : .newBubbleViewColor
        messageLabel.textColor = .label
    }
}

private extension UIColor {
    static let timeLabel = UIColor(hex: "#9199A1") ?? .systemGray
    static let secondColor = UIColor(hex: "#FFB049")
    
    static let bubbleViewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#2C2C2F") ?? .black
        default:
            return UIColor(hex: "#F4F5F7") ?? .systemGray
        }
    }
    
    static let newBubbleViewColor = UIColor {
        switch $0.userInterfaceStyle {
        case .dark:
            return UIColor(hex: "#29293D") ?? .black
        default:
            return UIColor(hex: "#EEF3FD") ?? .systemGray
        }
    }
}
