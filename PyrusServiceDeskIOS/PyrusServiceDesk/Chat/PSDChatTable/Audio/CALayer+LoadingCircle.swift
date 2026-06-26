extension CALayer {
    private enum Constants {
        static let loadingContainerLayerName = "loadingContainer"
        static let loadingCircleLayerName = "loadingCircle"
        static let circleLineWidth: CGFloat = 2
        static let circleStrokeEndInitial: CGFloat = 0.1
        static let circleRadiusInset: CGFloat = 2
        static let rotationAnimationDuration: TimeInterval = 1
        static let progressUpdateAnimationDuration: TimeInterval = 0.2
        static let minProgressValue: Float = 0.1
        static let maxProgressValue: Float = 1.0
        static let centerPoint = CGPoint(x: 0.5, y: 0.5)
        static let transformKeyPath = "transform.rotation"
        static let rotationKey = "rotation"
        static let fromTransform = 0
        static let toTransform = 2 * CGFloat.pi
        static let startCircleAngle = 0.0
        static let endCircleAngle = 2 * CGFloat.pi
    }
    
    func addLoadingAnimation() {
        removeLoadingAnimation()
        
        let containerLayer = CALayer()
        containerLayer.name = Constants.loadingContainerLayerName
        containerLayer.frame = bounds
        containerLayer.position = CGPoint(x: bounds.midX, y: bounds.midY)
        containerLayer.anchorPoint = Constants.centerPoint
        
        let circleLayer = CAShapeLayer()
        circleLayer.name = Constants.loadingCircleLayerName
        circleLayer.frame = bounds
        
        let radius = min(bounds.width, bounds.height) / 2 - Constants.circleRadiusInset
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        
        let circlePath = UIBezierPath(arcCenter: center,
                                    radius: radius,
                                      startAngle: Constants.startCircleAngle,
                                      endAngle: Constants.endCircleAngle,
                                    clockwise: true)
        
        circleLayer.path = circlePath.cgPath
        circleLayer.strokeColor = UIColor.white.cgColor//UIColor.systemTeal.cgColor
        circleLayer.fillColor = UIColor.clear.cgColor
        circleLayer.lineWidth = Constants.circleLineWidth
        circleLayer.lineCap = .round
        circleLayer.strokeEnd = Constants.circleStrokeEndInitial
        
        containerLayer.addSublayer(circleLayer)
        addSublayer(containerLayer)
        
        let rotatingTransformAnimation = CABasicAnimation(keyPath: Constants.transformKeyPath)
        rotatingTransformAnimation.fromValue = Constants.fromTransform
        rotatingTransformAnimation.toValue = Constants.toTransform
        rotatingTransformAnimation.duration = Constants.rotationAnimationDuration
        rotatingTransformAnimation.repeatCount = .infinity
        rotatingTransformAnimation.timingFunction = CAMediaTimingFunction(name: .linear)
        
        containerLayer.add(rotatingTransformAnimation, forKey: Constants.rotationKey)
    }
    
    func updateLoadingProgress(_ progress: Float) {
        guard let circleLayer = getCircleLayer() else { return }
        
        let clampedProgress = min(Constants.maxProgressValue, max(Constants.minProgressValue, progress))
        CATransaction.begin()
        CATransaction.setAnimationDuration(Constants.progressUpdateAnimationDuration)
        circleLayer.strokeEnd = CGFloat(clampedProgress)
        CATransaction.commit()
    }
    
    func removeLoadingAnimation() {
        sublayers?
            .filter { $0.name == Constants.loadingContainerLayerName }
            .forEach { $0.removeFromSuperlayer() }
    }
    
    func haveCircleLayer() -> Bool {
        return getCircleLayer() != nil
    }
    
    private func getCircleLayer() -> CAShapeLayer? {
        guard let containerLayer = sublayers?.first(where: { $0.name == Constants.loadingContainerLayerName }),
              let circleLayer = containerLayer.sublayers?.first(where: { $0.name == Constants.loadingCircleLayerName }) as? CAShapeLayer
        else {
            return nil
        }
        return circleLayer
    }
}
