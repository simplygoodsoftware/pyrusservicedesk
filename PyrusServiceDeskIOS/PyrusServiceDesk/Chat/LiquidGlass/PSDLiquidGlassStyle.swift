import UIKit

/// Единая точка принятия решения об использовании Liquid Glass оформления.
///
/// Экраны спрашивают именно этот тип: так условия включения не размазываются
/// по коду, и их изменение не требует правок на местах.
enum PSDLiquidGlassStyle {
    
    private enum Constants {
        ///Системный ключ Info.plist приложения-носителя. Выставленный в true,
        ///он переводит всё приложение на прежнее оформление, и библиотека
        ///обязана следовать за ним.
        static let compatibilityInfoKey = "UIDesignRequiresCompatibility"
    }
    
    /// Поддерживает ли текущая версия системы Liquid Glass.
    static var isSupportedBySystem: Bool {
        if #available(iOS 26.0, *) {
            return true
        }
        return false
    }
    
    /// Нужно ли рисовать интерфейс в стиле Liquid Glass.
    ///
    /// Отдельного параметра у библиотеки нет: оформление следует за приложением-носителем.
    /// Система на iOS 26+ рисует новый дизайн, если приложение не запросило
    /// совместимость ключом `UIDesignRequiresCompatibility` — ровно это условие
    /// повторяется здесь.
    static var isEnabled: Bool {
        isSupportedBySystem && !isCompatibilityDesignRequested
    }
    
    /// Цвет иконок на стеклянных кнопках: чёрный в светлой теме, белый в тёмной.
    ///
    /// Стекло само подстраивается под фон, поэтому иконкам не нужны цвета кастомизации —
    /// только контраст со стеклом. Цвет динамический: при смене темы перекрашивать
    /// ничего не надо, достаточно, чтобы иконка была template-картинкой с tintColor.
    static var iconColor: UIColor {
        UIColor.themedColor(lightColor: .black, darkColor: .white)
    }
    
    ///Просит ли приложение-носитель прежнее оформление.
    ///`Bundle.main` в составе фреймворка — это бандл приложения, то есть его Info.plist.
    ///Значение кэшируется: plist не меняется за время жизни процесса, а `isEnabled`
    ///опрашивается в layout-путях.
    private static let isCompatibilityDesignRequested: Bool = {
        guard let value = Bundle.main.object(forInfoDictionaryKey: Constants.compatibilityInfoKey) else {
            return false
        }
        if let number = value as? NSNumber {
            return number.boolValue
        }
        //Ключ, вписанный в plist руками, может приехать строкой "YES"/"true".
        if let string = value as? String {
            return (string as NSString).boolValue
        }
        return false
    }()
}
