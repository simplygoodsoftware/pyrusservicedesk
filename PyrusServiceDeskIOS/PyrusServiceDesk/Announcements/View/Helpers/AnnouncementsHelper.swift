
final class AnnouncementsHelper {
    /// Форматирует объём данных с автоматическим выбором единиц (B, KB, MB, GB, TB, PB).
    /// - Parameters:
    ///   - bytes: величина в байтах.
    ///   - decimals: количество знаков после запятой.
    ///   - spacing: вставлять ли пробел между числом и единицей ("6.0 MB" vs "6.0MB").
    ///   - useBinaryBase: true — кратно 1024 (KiB, MiB по сути, но метим как KB/MB),
    ///                    false — кратно 1000 (SI).
    ///   - locale: локаль форматирования числа.
    /// - Returns: строка вида "6.0MB".
    static func formatDataSize(_ bytes: Int,
                               decimals: Int = 1,
                               spacing: Bool = false,
                               useBinaryBase: Bool = true,
                               locale: Locale = .current) -> String {
        let base = useBinaryBase ? 1024.0 : 1000.0
        let units = ["B", "KB", "MB", "GB", "TB", "PB"]

        // Минимальная единица — KB
        let minUnitIndex = 1 // 0: B, 1: KB, 2: MB, ...
        guard bytes > 0 else {
            return "0" + (spacing ? " " : "") + units[minUnitIndex]
        }

        // Нормализуем значение сразу в минимальную единицу (KB)
        var value = Double(bytes) / pow(base, Double(minUnitIndex))
        var idx = minUnitIndex

        // Дальше повышаем единицу только если значение >= base
        while value >= base, idx < units.count - 1 {
            value /= base
            idx += 1
        }

        let nf = NumberFormatter()
        nf.locale = locale
        nf.minimumFractionDigits = decimals
        nf.maximumFractionDigits = decimals
        nf.numberStyle = .decimal

        let numberStr = nf.string(from: NSNumber(value: value)) ?? "\(value)"
        return numberStr + (spacing ? " " : "") + units[idx]
    }

    
    
    /// Возвращает высоту изображения при масштабировании по максимальной доступной ширине.
    static func scaledHeight(originalWidth: Int, originalHeight: Int, maxWidth: CGFloat) -> CGFloat {
        guard originalWidth > 0, originalHeight > 0, maxWidth > 0 else { return 0 }
        let aspect = CGFloat(originalHeight) / CGFloat(originalWidth)
        return maxWidth * aspect
    }

}
