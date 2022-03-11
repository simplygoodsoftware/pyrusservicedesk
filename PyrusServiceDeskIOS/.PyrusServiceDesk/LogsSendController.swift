import Foundation
class LogsSendController: UIViewController&FileChooser {
    var label: String = "SendLog".localizedPSD()
    var chooserDelegate: FileChooserDelegate?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        showLoading()
        modalTransitionStyle = .crossDissolve
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        sendLogs()
    }

    private func showLoading() {
        view.backgroundColor = UIColor.clear

        let prepareView = UIView()
        prepareView.layer.cornerRadius = PREPARE_CORNER_RADIUS
        prepareView.backgroundColor = .prepareViewColor
        view.addSubview(prepareView)


        let prepareLabel = UILabel()
        prepareLabel.textColor = .white
        prepareLabel.text = "PrepareLog".localizedPSD()
        prepareLabel.font = .label
        prepareView.addSubview(prepareLabel)
        
        prepareView.translatesAutoresizingMaskIntoConstraints = false
        prepareLabel.translatesAutoresizingMaskIntoConstraints = false
        
        prepareView.leftAnchor.constraint(equalTo: view.leftAnchor, constant: DISTANCE_TO_BOARD).isActive = true
        prepareView.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -DISTANCE_TO_BOARD).isActive = true
        prepareView.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        prepareView.heightAnchor.constraint(equalToConstant: PREPARE_HEIGHT).isActive = true
        
        prepareLabel.topAnchor.constraint(equalTo: prepareView.topAnchor).isActive = true
        prepareLabel.leftAnchor.constraint(equalTo: prepareView.leftAnchor, constant: DISTANCE_TO_BOARD).isActive = true
        prepareLabel.bottomAnchor.constraint(equalTo: prepareView.bottomAnchor).isActive = true
        prepareLabel.rightAnchor.constraint(equalTo: prepareView.rightAnchor, constant: -DISTANCE_TO_BOARD).isActive = true
    }
    private func sendLogs() {
        PyrusLogger.shared.collectLogs(in: self)
    }
    
}
extension LogsSendController: LogsSendProtocol {
    func sendData(_ data: Data?, with url: URL?) {
        guard let data = data, let url = url else {
            chooserDelegate?.didEndWithCancel()
            return
        }
        chooserDelegate?.didEndWithSuccess(data, url: url)
    }
}
private extension UIColor {
    static let prepareViewColor = #colorLiteral(red: 0.07843137255, green: 0.07843137255, blue: 0.07843137255, alpha: 0.9)
}
private extension UIFont {
    static let label = UIFont.systemFont(ofSize: 16)
}
private let PREPARE_HEIGHT: CGFloat = 45.0
private let PREPARE_CORNER_RADIUS: CGFloat = 10.0
private let DISTANCE_TO_BOARD: CGFloat = 15
private let DISTANCE_TO_BOTOM: CGFloat = 35
