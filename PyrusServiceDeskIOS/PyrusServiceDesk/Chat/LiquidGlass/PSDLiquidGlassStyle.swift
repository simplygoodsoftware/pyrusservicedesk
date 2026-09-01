import UIKit

/// Единая точка принятия решения об использовании Liquid Glass оформления.
///
/// Экраны спрашивают именно этот тип, а не `PyrusServiceDesk.liquidGlass` напрямую:
/// так проверка доступности системного API не размазывается по коду, и добавление
/// новых условий (например, отключение оформления на конкретных экранах)
/// не потребует правок на местах.
enum PSDLiquidGlassStyle {
    
    /// Поддерживает ли текущая версия системы Liquid Glass.
    static var isSupportedBySystem: Bool {
        if #available(iOS 26.0, *) {
            return true
        }
        return false
    }
    
    /// Нужно ли рисовать интерфейс в стиле Liquid Glass.
    static var isEnabled: Bool {
        PyrusServiceDesk.liquidGlass && isSupportedBySystem
    }
    
    /// Цвет иконок на стеклянных кнопках: чёрный в светлой теме, белый в тёмной.
    ///
    /// Стекло само подстраивается под фон, поэтому иконкам не нужны цвета кастомизации —
    /// только контраст со стеклом. Цвет динамический: при смене темы перекрашивать
    /// ничего не надо, достаточно, чтобы иконка была template-картинкой с tintColor.
    static var iconColor: UIColor {
        UIColor.themedColor(lightColor: .black, darkColor: .white)
    }
}
