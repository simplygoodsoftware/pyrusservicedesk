import UIKit

protocol RatingCommentDelegate: AnyObject {
    func sendRatingComment(comment: String)
}

class RatingCommentViewController: UIViewController {

    weak var delegate: RatingCommentDelegate?
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "EvaluateQuality".localizedPSD()
        label.font = UIFont.boldSystemFont(ofSize: 22)
        label.textColor = .black
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let commentLabel: UILabel = {
        let label = UILabel()
        label.text = "LeaveYourComment".localizedPSD()
        label.font = UIFont.systemFont(ofSize: 13)
        label.textColor = .darkGray
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let textView: UITextView = {
        let textView = UITextView()
        textView.layer.borderColor = UIColor(hex: "#D6D9DC")?.cgColor
        textView.layer.borderWidth = 1
        textView.layer.cornerRadius = 8
        textView.font = UIFont.systemFont(ofSize: 14)
        textView.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor
        textView.translatesAutoresizingMaskIntoConstraints = false
        return textView
    }()

    private let closeButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Close".localizedPSD(), for: .normal)
        button.setTitleColor(PyrusServiceDesk.mainController?.customization?.themeColor, for: .normal)
        button.backgroundColor = UIColor(hex: "#ECEDEF")
        button.titleLabel?.font = .systemFont(ofSize: 17)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let sendButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Send".localizedPSD(), for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.backgroundColor = PyrusServiceDesk.mainController?.customization?.themeColor
        button.titleLabel?.font = .systemFont(ofSize: 17)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white

        setupLayout()
        closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)
        sendButton.addTarget(self, action: #selector(sendTapped), for: .touchUpInside)
    }

    private func setupLayout() {
        [titleLabel, commentLabel, textView, closeButton, sendButton].forEach {
            view.addSubview($0)
        }

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: 24),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),

            commentLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 24),
            commentLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),

            textView.topAnchor.constraint(equalTo: commentLabel.bottomAnchor, constant: 6),
            textView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            textView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            textView.heightAnchor.constraint(equalToConstant: 66),

            closeButton.topAnchor.constraint(equalTo: textView.bottomAnchor, constant: 24),
            closeButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            closeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            closeButton.heightAnchor.constraint(equalToConstant: 48),

            sendButton.topAnchor.constraint(equalTo: closeButton.bottomAnchor, constant: 16),
            sendButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            sendButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            sendButton.heightAnchor.constraint(equalToConstant: 48),
        ])
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }

    @objc private func sendTapped() {
        if !textView.text.isEmpty {
            delegate?.sendRatingComment(comment: textView.text)
        }
        dismiss(animated: true)
    }
}
