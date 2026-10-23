import UIKit

/// Оформление навигационного бара чата.
///
/// Ключевое отличие двух режимов не в том, какое оформление назначить,
/// а в том, назначать ли его вообще. На iOS 26 системный бар уже нарисован как надо:
/// прозрачный, с градиентным блюром над уезжающим под него контентом и стеклянными
/// капсулами под кнопками. Любой назначенный `UINavigationBarAppearance` — включая
/// пустой и включая сконфигурированный «по умолчанию» — выключает это поведение.
/// Поэтому в Liquid Glass мы оформление снимаем и больше ничего не трогаем.
@available(iOS 13.0, *)
enum PSDNavigationBarStyler {
    
    /// Прежнее оформление Service Desk — непрозрачный бар заданного цвета.
    static func applyLegacy(to navigationItem: UINavigationItem, backgroundColor: UIColor) {
        let barAppearance = UIBarAppearance()
        barAppearance.backgroundColor = backgroundColor
        
        let appearance = UINavigationBarAppearance(barAppearance: barAppearance)
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = backgroundColor
        appearance.backgroundEffect = nil
        
        navigationItem.standardAppearance = appearance
        navigationItem.scrollEdgeAppearance = appearance
    }
    
    /// Возвращает бару системное оформление, снимая своё.
    ///
    /// Сам `navigationBar` намеренно не трогаем: у него уже стоит системное оформление,
    /// и любая наша замена — даже `configureWithDefaultBackground()` — уводит бар
    /// из-под системного Liquid Glass. `nil` в `navigationItem` означает
    /// «использовать оформление бара», то есть ровно то, что нужно.
    static func applySystem(to navigationItem: UINavigationItem) {
        navigationItem.standardAppearance = nil
        navigationItem.scrollEdgeAppearance = nil
        navigationItem.compactAppearance = nil
    }
}
