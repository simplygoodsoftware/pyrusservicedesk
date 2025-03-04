import UIKit

final class PSDChatCellConfigurator: TableViewCellConfiguratorProtocol {

    private let psdUserMessageCellIdentifier = ReuseIdentifier<PSDUserMessageCell>(identifier:  PSDUserMessageCell.identifier)
    private let psdSupportMessageCellIdentifier = ReuseIdentifier<PSDSupportMessageCell>(identifier:  PSDSupportMessageCell.identifier)

    let tableView: UITableView

    init(tableView: UITableView) {
        self.tableView = tableView
        tableView.register(PSDUserMessageCell.self, forCellReuseIdentifier: PSDUserMessageCell.identifier)
        tableView.register(PSDSupportMessageCell.self, forCellReuseIdentifier: PSDSupportMessageCell.identifier)
    }

    func getCell(model: [[PSDRowMessage]], indexPath: IndexPath) -> PSDChatMessageCell {
        let message: PSDRowMessage
        if model.count > indexPath.section && model[indexPath.section].count > indexPath.row {
            message = model[indexPath.section][indexPath.row]
        } else {
            return PSDChatMessageCell()
        }
        
        let cell: PSDChatMessageCell
        if (message.rating ?? 0) != 0 || (message.message.owner.personId == PyrusServiceDesk.userId) && !(PyrusServiceDesk.multichats && !message.message.isOutgoing) {
            cell = getPSDUserMessageCell(model: model, indexPath: indexPath)
        } else {
            let needShowAvatar = model.needShowAvatar(at: indexPath)
            cell = getPSDSupportMessageCell(model: message, needShowAvatar: needShowAvatar, indexPath: indexPath)
        }
        
        cell.needShowName = model.needShowName(at: indexPath)
        cell.drawEmpty = model.emptyMessage(at: indexPath)
        cell.firstMessageInDate = indexPath.row == model[indexPath.section].count - 1
        
        return cell
    }
}

private extension PSDChatCellConfigurator {
    func getPSDUserMessageCell(model: [[PSDRowMessage]], indexPath: IndexPath) -> PSDUserMessageCell {
        let cell = getCell(reuseIdentifier: psdUserMessageCellIdentifier, indexPath: indexPath)
        return cell
    }
    
    func getPSDSupportMessageCell(model: PSDRowMessage, needShowAvatar: Bool, indexPath: IndexPath) -> PSDSupportMessageCell {
        let cell = getCell(reuseIdentifier: psdSupportMessageCellIdentifier, indexPath: indexPath)
        cell.needShowAvatar = needShowAvatar
        if cell.needShowAvatar {
            cell.avatarView.owner = model.message.owner
        }
        return cell
    }
}
