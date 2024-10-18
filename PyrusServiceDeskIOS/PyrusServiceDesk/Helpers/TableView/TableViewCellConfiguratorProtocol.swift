import UIKit

protocol ReuseIdentifierType {
     associatedtype ReusableType
     var identifier: String { get }
 }

 struct ReuseIdentifier<Reusable>: ReuseIdentifierType {
     typealias ReusableType = Reusable
     let identifier: String

     init(identifier: String) {
         self.identifier = identifier
     }
 }

protocol TableViewCellConfiguratorProtocol {
     var tableView: UITableView { get }

     init(tableView: UITableView)
     func getCell<T: UITableViewCell>(reuseIdentifier: ReuseIdentifier<T>,
                                      indexPath: IndexPath) -> T
}

extension UITableView {

     func dequeueReusableCell<Identifier: ReuseIdentifierType>(withIdentifier identifier: Identifier, for indexPath: IndexPath) -> Identifier.ReusableType?
     where Identifier.ReusableType: UITableViewCell {
         return dequeueReusableCell(withIdentifier: identifier.identifier, for: indexPath) as? Identifier.ReusableType
     }
 }

 extension TableViewCellConfiguratorProtocol {

     func getCell<T: UITableViewCell>(reuseIdentifier: ReuseIdentifier<T>, indexPath: IndexPath) -> T {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: reuseIdentifier, for: indexPath) else { return T() }
        return cell
    }
}

