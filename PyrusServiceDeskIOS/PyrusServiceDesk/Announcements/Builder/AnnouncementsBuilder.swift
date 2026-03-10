import Foundation

final class AnnouncementsBuilder: NSObject {
    func makeModule() -> AnnouncementsViewController {
        let presenter = AnnouncementsPresenter()
        let interactor = AnnouncementsInteractor(presenter: presenter)
        let controller = AnnouncementsViewController(interactor: interactor)
        presenter.view = controller
        return controller
    }
}
