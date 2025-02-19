import Foundation

@objc public class PSDClientInfo: NSObject {
    let imageRepository: ImageRepositoryProtocol?
    let clientId: String
    var clientName: String {
        didSet {
            if let image {
                imageRepository?.saveImage(image, name: clientId, id: nil, type: .clientIcon)
            }
        }
    }
    
    var clientIcon: String {
        didSet {
            DispatchQueue.global().async { [weak self] in
                guard let self else { return }
                let url = PyrusServiceDeskAPI.PSDURL(url: self.clientIcon)
                PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
                    guard let data = data, error == nil else{
                        return
                    }
                    if data.count != 0,
                       let image = UIImage(data: data) {
                        self.image = image
                    }
                }.resume()
            }
        }
    }
    
    var image: UIImage? = nil {
        didSet {
            if let image {
                imageRepository?.saveImage(image, name: clientId, id: nil, type: .clientIcon)
            }
        }
    }
    
    init(clientId: String, clientName: String, clientIcon: String, image: UIImage? = nil) {
        self.clientId = clientId
        self.clientName = clientName
        self.clientIcon = clientIcon
        self.image = image
        imageRepository = ImageRepository()
        super.init()
        DispatchQueue.global().async { [weak self] in
            guard let self else { return }
            if let image = self.imageRepository?.loadImage(name: clientId, id: nil, type: .clientIcon) {
                self.image = image
            } else {
                let url = PyrusServiceDeskAPI.PSDURL(url: self.clientIcon)
                PyrusServiceDesk.mainSession.dataTask(with: url) { data, response, error in
                    guard let data = data, error == nil else{
                        return
                    }
                    if data.count != 0,
                       let image = UIImage(data: data) {
                        self.image = image
                    }
                }.resume()
            }
        }
    }
    
    override public func isEqual(_ object: Any?) -> Bool {
        guard let other = object as? PSDClientInfo else {
            return false
        }
        return self.clientId == other.clientId
    }
}
