import UIKit

protocol RatingCommentDelegate: AnyObject {
    func sendRatingComment(comment: String?, rating: Int)
}

class RatingCommentViewController: UIViewController {

    weak var delegate: RatingCommentDelegate?
    
    private let rating: Int
    private let ratingText: String?
        
    init(rating: Int, ratingText: String?) {
        self.rating = rating
        self.ratingText = ratingText
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 22)
        label.numberOfLines = 2
        label.textAlignment = .left
        label.textColor = UIColor.getTextColor(for: PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let commentLabel: UILabel = {
        let label = UILabel()
        label.text = "LeaveYourComment".localizedPSD()
        label.font = UIFont.systemFont(ofSize: 13)
        let backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground
        label.textColor = UIColor.getSecondTextColor(for: backgroundColor)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let textView: UITextView = {
        let textView = UITextView()
        textView.layer.borderWidth = 1
        textView.layer.cornerRadius = 8
        textView.font = UIFont.systemFont(ofSize: 14)
        let backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground
        textView.layer.borderColor = UIColor.getBorderColor(for: backgroundColor).cgColor//UIColor(hex: "#D6D9DC")?.cgColor
        textView.tintColor = PyrusServiceDesk.mainController?.customization?.themeColor ?? UIColor.getTextColor(for: backgroundColor)
        textView.backgroundColor = backgroundColor
        textView.textColor = UIColor.getTextColor(for: backgroundColor)
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.keyboardAppearance = CustomizationHelper.keyboardStyle
        return textView
    }()

    private let closeButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Close".localizedPSD(), for: .normal)
        button.setTitleColor(PyrusServiceDesk.mainController?.customization?.themeColor ?? CustomizationHelper.supportMassageTextColor, for: .normal)
        button.backgroundColor = CustomizationHelper.supportMassageBackgroundColor//UIColor(hex: "#ECEDEF")
        button.titleLabel?.font = .systemFont(ofSize: 17)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let sendButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Send".localizedPSD(), for: .normal)
        button.setTitleColor(CustomizationHelper.userMassageTextColor, for: .normal)
        button.backgroundColor = CustomizationHelper.userMassageBackgroundColor//.appColor
        button.titleLabel?.font = .systemFont(ofSize: 17)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    ///Размеры Liquid Glass оформления.
    private enum GlassLayout {
        static let closeButtonSize: CGFloat = 32
        static let sendButtonHeight: CGFloat = 48
        static let textViewHeight: CGFloat = 120
        static let textViewCornerRadius: CGFloat = 12
        static let sideInset: CGFloat = 16
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground

        setupLayout()
        closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)
        sendButton.addTarget(self, action: #selector(sendTapped), for: .touchUpInside)
    }

    private func setupLayout() {
        titleLabel.text = ratingText ?? "EvaluateQuality".localizedPSD()

        [titleLabel, commentLabel, textView, closeButton, sendButton].forEach {
            view.addSubview($0)
        }

        if PSDLiquidGlassStyle.isEnabled {
            applyLiquidGlassAppearance()
            addLiquidGlassConstraints()
        } else {
            addLegacyConstraints()
        }
    }

    private func addLegacyConstraints() {
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: 24),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),

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

    ///Оформление по макету: круглый крестик по центру заголовка вместо кнопки
    ///«Закрыть», поле с большим радиусом, «Отправить» — пилюля во всю ширину.
    ///Цвета остаются из кастомизации, меняется только форма.
    private func applyLiquidGlassAppearance() {
        closeButton.setTitle(nil, for: .normal)
        closeButton.setImage(PSDNavigationItemFactory.image(for: .close), for: .normal)
        closeButton.tintColor = PSDLiquidGlassStyle.iconColor
        closeButton.backgroundColor = CustomizationHelper.grayViewColor
        closeButton.layer.cornerRadius = GlassLayout.closeButtonSize / 2
        closeButton.clipsToBounds = true

        sendButton.layer.cornerRadius = GlassLayout.sendButtonHeight / 2
        sendButton.titleLabel?.font = .boldSystemFont(ofSize: 17)

        textView.layer.cornerRadius = GlassLayout.textViewCornerRadius
        applyLiquidGlassColors()
    }
    
    ///Цвета экрана, разрешённые в актуальной теме.
    ///
    ///Лейблы и поле создаются как `let` до загрузки вью: `getTextColor(for:)` там
    ///разрешает динамический цвет фона вне trait-контекста — то есть всегда по светлой
    ///теме. В тёмной получалась каша: чёрный заголовок и светлые панели на тёмном фоне.
    ///Здесь всё разрешается по фактическим traits и пересобирается при их смене.
    private func applyLiquidGlassColors() {
        let background = (PyrusServiceDesk.mainController?.customization?.customBackgroundColor ?? .psdBackground)
            .resolvedColor(with: traitCollection)
        view.backgroundColor = background
        titleLabel.textColor = UIColor.getTextColor(for: background)
        commentLabel.textColor = UIColor.getSecondTextColor(for: background)
        textView.backgroundColor = background
        textView.textColor = UIColor.getTextColor(for: background)
        textView.layer.borderColor = UIColor.getBorderColor(for: background).cgColor
    }
    
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        guard PSDLiquidGlassStyle.isEnabled,
              traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection)
        else { return }
        applyLiquidGlassColors()
    }

    private func addLiquidGlassConstraints() {
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: 24),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: GlassLayout.sideInset),
            titleLabel.trailingAnchor.constraint(equalTo: closeButton.leadingAnchor, constant: -12),

            //Крестик выровнен по центру заголовка.
            closeButton.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            closeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -GlassLayout.sideInset),
            closeButton.widthAnchor.constraint(equalToConstant: GlassLayout.closeButtonSize),
            closeButton.heightAnchor.constraint(equalToConstant: GlassLayout.closeButtonSize),

            commentLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            commentLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: GlassLayout.sideInset),

            textView.topAnchor.constraint(equalTo: commentLabel.bottomAnchor, constant: 6),
            textView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: GlassLayout.sideInset),
            textView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -GlassLayout.sideInset),
            textView.heightAnchor.constraint(equalToConstant: GlassLayout.textViewHeight),

            sendButton.topAnchor.constraint(equalTo: textView.bottomAnchor, constant: 24),
            sendButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: GlassLayout.sideInset),
            sendButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -GlassLayout.sideInset),
            sendButton.heightAnchor.constraint(equalToConstant: GlassLayout.sendButtonHeight),
        ])
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }

    @objc private func sendTapped() {
        if !textView.text.isEmpty {
            delegate?.sendRatingComment(comment: textView.text, rating: rating)
        }
        dismiss(animated: true)
    }
}
