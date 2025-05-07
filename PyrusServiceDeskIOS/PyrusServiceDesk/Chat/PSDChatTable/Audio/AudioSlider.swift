//
//  AudioSlider.swift
//  Helpy
//
//  Created by Станислава Бобрускина on 23.04.2025.
//  Copyright © 2025 Pyrus. All rights reserved.
//

class AudioCellSlider: UISlider {
    
    enum Constants {
        static let sliderMinimumTrackColor = UIColor.white//UIColor(red: 0, green: 153/255, blue: 153/255, alpha: 1)
        static let sliderMaximumTrackColor = UIColor(hex: "#E3E5E84D")?.withAlphaComponent(0.3)//UIColor(red: 214/255, green: 217/255, blue: 220/255, alpha: 1)
        static let sliderThumbSystemImageName = "circle.fill"
        static let expandConstant = 25.0
        static let trackTouchExpansion = 20.0
    }
    
    private var initialTouchPoint: CGPoint = .zero
    private var initialValue: Float = 0
    private var touchedOnTrack: Bool = false
    
    // Флаг для предотвращения параллельных обновлений
    private var isAdjusting: Bool = false
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupSlider()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupSlider()
    }
    
    private func setupSlider() {
        minimumTrackTintColor = Constants.sliderMinimumTrackColor
        maximumTrackTintColor = Constants.sliderMaximumTrackColor
        setThumbImage(UIImage(systemName: Constants.sliderThumbSystemImageName), for: .normal)
        translatesAutoresizingMaskIntoConstraints = false
        isContinuous = false
    }
    
    private func expandedTrackRectForTouch(originalTrackRect: CGRect) -> CGRect {
        var expandedTrackRect = originalTrackRect
        expandedTrackRect.origin.y -= Constants.trackTouchExpansion
        expandedTrackRect.size.height += Constants.trackTouchExpansion * 2
        return expandedTrackRect
    }
    
    override func beginTracking(_ touch: UITouch, with event: UIEvent?) -> Bool {
        if isAdjusting {
            return false
        }
        
        isAdjusting = true
        initialTouchPoint = touch.location(in: self)
        
        let actualTrackRect = self.trackRect(forBounds: bounds)
        let expandedTrackRect = expandedTrackRectForTouch(originalTrackRect: actualTrackRect)
        let thumbRect = self.thumbRect(forBounds: bounds, trackRect: actualTrackRect, value: value)
        
        touchedOnTrack = expandedTrackRect.contains(initialTouchPoint) && !thumbRect.contains(initialTouchPoint)
        
        if thumbRect.contains(initialTouchPoint) {
            initialValue = value
            return true
        } else if touchedOnTrack {
            let limitedX = min(max(initialTouchPoint.x, actualTrackRect.minX), actualTrackRect.maxX)
            let percentage = (limitedX - actualTrackRect.minX) / actualTrackRect.width
            let newValue = minimumValue + Float(percentage) * (maximumValue - minimumValue)
            
            setValue(newValue, animated: false)
            
            isAdjusting = false
            
            initialValue = newValue
            return true
        }
        
        isAdjusting = false
        return false
    }
    
    override func continueTracking(_ touch: UITouch, with event: UIEvent?) -> Bool {
        if touchedOnTrack {
            return true
        }
        
        let currentPoint = touch.location(in: self)
        let deltaX = currentPoint.x - initialTouchPoint.x
        let trackRect = self.trackRect(forBounds: bounds)
        let trackWidth = trackRect.width
        
        let deltaValue = Float(deltaX / trackWidth) * (maximumValue - minimumValue)
        let newValue = initialValue + deltaValue
        let clampedValue = min(maximumValue, max(minimumValue, newValue))
        
        setValue(clampedValue, animated: false)
        
        return true
    }
    
    override func endTracking(_ touch: UITouch?, with event: UIEvent?) {
        touchedOnTrack = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            self.isAdjusting = false
        }
        super.endTracking(touch, with: event)
    }
    
    override func cancelTracking(with event: UIEvent?) {
        touchedOnTrack = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            self.isAdjusting = false
        }
        super.cancelTracking(with: event)
    }
    
    override func thumbRect(forBounds bounds: CGRect, trackRect rect: CGRect, value: Float) -> CGRect {
        let originalRect = super.thumbRect(forBounds: bounds, trackRect: rect, value: value)
        let expandBy: CGFloat = Constants.expandConstant
        return originalRect.insetBy(dx: -expandBy, dy: -expandBy)
    }
    
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        let trackRect = self.trackRect(forBounds: bounds)
        let expandedTrackRect = expandedTrackRectForTouch(originalTrackRect: trackRect)
        let thumbRect = self.thumbRect(forBounds: bounds, trackRect: trackRect, value: value)
        
        if thumbRect.contains(point) || expandedTrackRect.contains(point) || super.point(inside: point, with: event) {
            return true
        }
        return false
    }
}
