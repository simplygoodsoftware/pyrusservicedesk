import UIKit

/// Собирает кнопки навигационного бара для Liquid Glass оформления.
///
/// Стеклянную подложку под элементами бара на iOS 26+ рисует сама система,
/// поэтому здесь достаточно иконки без текста — как в макете.
/// Цвет иконок единый для всех стеклянных кнопок — `PSDLiquidGlassStyle.iconColor`.
enum PSDNavigationItemFactory {
    
    ///Системные символы, которыми рисуются кнопки бара.
    enum Symbol: String {
        case back = "chevron.backward"
        case info = "info.circle"
        case close = "xmark"
        case share = "square.and.arrow.up"
    }
    
    private enum Constants {
        static let symbolPointSize: CGFloat = 17
        static let symbolWeight: UIImage.SymbolWeight = .semibold
    }
    
    /// Кнопка «Назад» — шеврон без подписи.
    /// - Returns: `nil`, если системные символы недоступны на текущей версии iOS.
    static func makeBackItem(target: Any?,
                             action: Selector) -> UIBarButtonItem? {
        makeItem(symbol: .back,
                 accessibilityLabel: "Back".localizedPSD(),
                 target: target,
                 action: action)
    }
    
    /// Кнопка информации о заявке.
    static func makeInfoItem(target: Any?,
                             action: Selector) -> UIBarButtonItem? {
        makeItem(symbol: .info,
                 accessibilityLabel: nil,
                 target: target,
                 action: action)
    }
    
    /// Кнопка закрытия — крестик без подписи.
    static func makeCloseItem(target: Any?,
                              action: Selector) -> UIBarButtonItem? {
        makeItem(symbol: .close,
                 accessibilityLabel: "Close".localizedPSD(),
                 target: target,
                 action: action)
    }
    
    /// Картинка символа в едином для бара размере и начертании.
    /// Для элементов, которые нельзя пересоздать через фабрику, — например,
    /// `PSDShareBarItemView`, живущего своей логикой поверх `UIBarButtonItem`.
    static func image(for symbol: Symbol) -> UIImage? {
        guard #available(iOS 13.0, *) else { return nil }
        let configuration = UIImage.SymbolConfiguration(pointSize: Constants.symbolPointSize,
                                                        weight: Constants.symbolWeight)
        return UIImage(systemName: symbol.rawValue, withConfiguration: configuration)
    }
    
    private static func makeItem(symbol: Symbol,
                                 accessibilityLabel: String?,
                                 target: Any?,
                                 action: Selector) -> UIBarButtonItem? {
        guard let image = image(for: symbol) else { return nil }
        
        let item = UIBarButtonItem(image: image, style: .plain, target: target, action: action)
        item.tintColor = PSDLiquidGlassStyle.iconColor
        if let accessibilityLabel = accessibilityLabel {
            item.accessibilityLabel = accessibilityLabel
        }
        return item
    }
}

///Прежнее имя фабрики. Оставлено, чтобы не трогать существующие вызовы в экране чата.
typealias PSDChatNavigationItemFactory = PSDNavigationItemFactory
