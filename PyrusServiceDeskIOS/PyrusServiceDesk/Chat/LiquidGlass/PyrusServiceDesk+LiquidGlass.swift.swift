import UIKit

/// Хранилище значения флага.
/// Расширения не могут содержать хранимые свойства, поэтому значение живёт
/// в отдельном типе, видимом только внутри этого файла.
private enum PSDLiquidGlassFlagStorage {
    static var isEnabled: Bool = true
}

public extension PyrusServiceDesk {
    
    /// Включает оформление интерфейса Service Desk в стиле Liquid Glass.
    ///
    /// Флаг учитывается только на iOS 26 и выше — на более ранних версиях системы
    /// интерфейс остаётся в прежнем оформлении независимо от значения флага.
    /// Значение имеет смысл выставлять до показа экранов Service Desk.
    ///
    /// Внутри библиотеки флаг напрямую не читают: решение об оформлении
    /// принимает `PSDLiquidGlassStyle`.
    static var liquidGlass: Bool {
        get { PSDLiquidGlassFlagStorage.isEnabled }
        set { PSDLiquidGlassFlagStorage.isEnabled = newValue }
    }
}
