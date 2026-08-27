import UIKit

/// Собирает кнопки навигационного бара чата для Liquid Glass оформления.
///
/// Стеклянную подложку под элементами бара на iOS 26+ рисует сама система,
/// поэтому здесь достаточно иконки без текста — как в макете.
enum PSDChatNavigationItemFactory {
    
    private enum Constants {
        static let backSymbolName = "chevron.backward"
        static let infoSymbolName = "info.circle"
        static let symbolPointSize: CGFloat = 17
        static let symbolWeight: UIImage.SymbolWeight = .semibold
    }
    
    /// Кнопка «Назад» — шеврон без подписи.
    /// - Returns: `nil`, если системные символы недоступны на текущей версии iOS.
    static func makeBackItem(target: Any?,
                             action: Selector,
                             tintColor: UIColor) -> UIBarButtonItem? {
        makeItem(symbolName: Constants.backSymbolName,
                 accessibilityLabel: "Back".localizedPSD(),
                 target: target,
                 action: action,
                 tintColor: tintColor)
    }
    
    /// Кнопка информации о заявке.
    /// - Returns: `nil`, если системные символы недоступны на текущей версии iOS.
    static func makeInfoItem(target: Any?,
                             action: Selector,
                             tintColor: UIColor) -> UIBarButtonItem? {
        makeItem(symbolName: Constants.infoSymbolName,
                 accessibilityLabel: nil,
                 target: target,
                 action: action,
                 tintColor: tintColor)
    }
    
    private static func makeItem(symbolName: String,
                                 accessibilityLabel: String?,
                                 target: Any?,
                                 action: Selector,
                                 tintColor: UIColor) -> UIBarButtonItem? {
        guard #available(iOS 14.0, *) else { return nil }
        
        let configuration = UIImage.SymbolConfiguration(pointSize: Constants.symbolPointSize,
                                                        weight: Constants.symbolWeight)
        guard let image = UIImage(systemName: symbolName, withConfiguration: configuration) else {
            return nil
        }
        
        let item = UIBarButtonItem(image: image, style: .plain, target: target, action: action)
        item.tintColor = tintColor
        if let accessibilityLabel = accessibilityLabel {
            item.accessibilityLabel = accessibilityLabel
        }
        return item
    }
}
