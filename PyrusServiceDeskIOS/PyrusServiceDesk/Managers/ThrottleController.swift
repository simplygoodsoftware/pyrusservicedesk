final class ThrottleController {

    private let queue = DispatchQueue(label: "sync.throttle.queue")
    private var interval: TimeInterval

    private var lastFireDate: Date?
    private var pending = false
    private var workItem: DispatchWorkItem?

    init(interval: TimeInterval) {
        self.interval = interval
    }
    
    func setInterval(_ interval: TimeInterval) {
        self.interval = interval
    }

    func execute(_ block: @escaping () -> Void) {
        queue.async {
            let now = Date()

            if let last = self.lastFireDate {
                let diff = now.timeIntervalSince(last)

                if diff < self.interval {
                    self.pending = true
                    self.workItem?.cancel()

                    let delay = self.interval - diff
                    let item = DispatchWorkItem { [weak self] in
                        guard let self else { return }
                        self.lastFireDate = Date()
                        self.pending = false
                        DispatchQueue.main.async {
                            block()
                        }
                    }

                    self.workItem = item
                    self.queue.asyncAfter(deadline: .now() + delay, execute: item)
                    return
                }
            }

            self.lastFireDate = now
            DispatchQueue.main.async {
                block()
            }
        }
    }
}
