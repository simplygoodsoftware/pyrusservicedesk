import UIKit
import WebKit
import UniformTypeIdentifiers
import AVFoundation

// MARK: - Page VC (одна страница = одно вложение)
protocol AttachmentPageViewControllerDelegate: AnyObject {
    func pageDidUpdateShareURL(_ page: AttachmentPageViewController, shareURL: URL?)
    func pageDidRequestShare(_ page: AttachmentPageViewController, url: URL)
    func pageDidUpdateAppearance(_ page: AttachmentPageViewController, isMedia: Bool, title: String)
    func pageDidRequestDismiss(_ page: AttachmentPageViewController)
}

final class AttachmentPageViewController: UIViewController {

    // MARK: - Input
    private let item: PSDAnnouncementAttachment

    public var itemId: String { item.id }
    public var isMedia: Bool { item.media || item.isVideo }
    public var attachmentTitle: String {
        let name = item.name ?? ""
        return name.isEmpty ? "Attachment".localizedPSD() : name
    }

    // MARK: - State
    private var loadTask: Task<Void, Never>?
    private(set) var shareURL: URL? {
        didSet { delegate?.pageDidUpdateShareURL(self, shareURL: shareURL) }
    }
    private var shouldPlay = false

    weak var delegate: AttachmentPageViewControllerDelegate?

    // MARK: - UI
    private let spinner = UIActivityIndicatorView(style: .large)
    private let errorStack = UIStackView()
    private let retryButton = UIButton(type: .system)
    private let errorLabel = UILabel()

    // Zoomable image
    private let zoomScroll = UIScrollView()
    private let imageView = UIImageView()

    // Video (zoomable)
    private let videoScroll = UIScrollView()
    private let videoContainer = UIView()
    private var player: AVPlayer?
    private var playerLayer: AVPlayerLayer?
    private var timeObserver: Any?
    private var statusObservation: NSKeyValueObservation?

    // Video controls (bottom)
    private let controlsStack = UIStackView()
    private let playPauseButton = UIButton(type: .system)
    private let scrubber = UISlider()
    private let currentLabel = UILabel()
    private let durationLabel = UILabel()
    private var endObserver: NSObjectProtocol?

    // WebView
    private lazy var webView: WKWebView = {
        let config = WKWebViewConfiguration()
        let wv = WKWebView(frame: .zero, configuration: config)
        wv.scrollView.contentInsetAdjustmentBehavior = .always
        return wv
    }()

    // MARK: - Swipe-to-dismiss (long)
    private var panToDismiss: UIPanGestureRecognizer!
    private var panStartTime: CFTimeInterval = 0

    private let minSwipeDuration: CFTimeInterval = 0
    private let minSwipeDistance: CGFloat = 200

    // MARK: - Init
    init(item: PSDAnnouncementAttachment) {
        self.item = item
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        startLoading()
        setupLongSwipeToDismiss()
        delegate?.pageDidUpdateAppearance(self, isMedia: item.media || item.isVideo, title: attachmentTitle)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        // Останавливаем видео при перелистывании
        pauseVideoIfNeeded(resetToStart: false)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard let player else {
            shouldPlay = true
            return
        }
        player.play()
        self.playPauseButton.setImage(UIImage(systemName: "pause.fill"), for: .normal)
        self.updateTimeUI(current: player.currentTime().seconds, duration: player.currentItem?.duration.seconds ?? 0)
    }

    deinit {
        removePlayerObservers()
    }

    private func pauseVideoIfNeeded(resetToStart: Bool) {
        guard let player else { return }
        player.pause()
        playPauseButton.setImage(UIImage(systemName: "play.fill"), for: .normal)
    }
    
    private func setupLongSwipeToDismiss() {
        panToDismiss = UIPanGestureRecognizer(target: self, action: #selector(handleLongSwipe(_:)))
        panToDismiss.delegate = self
        view.addGestureRecognizer(panToDismiss)
    }

    @objc private func handleLongSwipe(_ gesture: UIPanGestureRecognizer) {
        guard isMedia else { return }
        switch gesture.state {
        case .began:
            panStartTime = CACurrentMediaTime()
        case .ended:
            let totalTime = CACurrentMediaTime() - panStartTime
            let translationY = gesture.translation(in: view).y
            let isDown = translationY > 0

            let isLongEnough = totalTime >= minSwipeDuration
            let isFarEnough  = translationY >= minSwipeDistance

            if isDown && isLongEnough && isFarEnough {
                delegate?.pageDidRequestDismiss(self)
            }
        default:
            break
        }
    }

    private func setupViews() {
        // Spinner
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.color = item.media || item.isVideo ? .white : .systemGray
        view.addSubview(spinner)

        // Error UI
        errorStack.axis = .vertical
        errorStack.alignment = .center
        errorStack.spacing = 12
        errorStack.translatesAutoresizingMaskIntoConstraints = false
        errorLabel.textColor = item.media || item.isVideo ? .white : .label
        errorLabel.font = .systemFont(ofSize: 16, weight: .medium)
        errorLabel.numberOfLines = 0
        errorLabel.textAlignment = .center
        retryButton.setTitle("RetryButton".localizedPSD(), for: .normal)
        retryButton.tintColor = item.media || item.isVideo ? .white : .label
        retryButton.addAction(UIAction { [weak self] _ in self?.startLoading() }, for: .touchUpInside)
        errorStack.addArrangedSubview(errorLabel)
        errorStack.addArrangedSubview(retryButton)
        errorStack.isHidden = true
        view.addSubview(errorStack)

        // Zoomable image
        zoomScroll.translatesAutoresizingMaskIntoConstraints = false
        zoomScroll.minimumZoomScale = 1.0
        zoomScroll.maximumZoomScale = 4.0
        zoomScroll.delegate = self
        zoomScroll.isHidden = true

        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        imageView.clipsToBounds = true
        imageView.backgroundColor = zoomScroll.backgroundColor

        zoomScroll.addSubview(imageView)
        view.addSubview(zoomScroll)

        // Video (zoomable)
        videoScroll.translatesAutoresizingMaskIntoConstraints = false
        videoScroll.minimumZoomScale = 1.0
        videoScroll.maximumZoomScale = 4.0
        videoScroll.delegate = self
        videoScroll.isHidden = true

        videoContainer.translatesAutoresizingMaskIntoConstraints = false
        videoContainer.backgroundColor = videoScroll.backgroundColor
        videoScroll.addSubview(videoContainer)
        view.addSubview(videoScroll)

        // Video controls (bottom)
        controlsStack.axis = .horizontal
        controlsStack.alignment = .center
        controlsStack.spacing = 8
        controlsStack.translatesAutoresizingMaskIntoConstraints = false
        controlsStack.isHidden = true

        // Style
        let primaryColor: UIColor = item.media || item.isVideo ? .white : .label
        playPauseButton.tintColor = primaryColor
        currentLabel.textColor = primaryColor
        durationLabel.textColor = primaryColor

        // Configure controls
        playPauseButton.setImage(UIImage(systemName: "play.fill"), for: .normal)
        playPauseButton.addAction(UIAction { [weak self] _ in self?.togglePlayPause() }, for: .touchUpInside)

        currentLabel.font = .monospacedDigitSystemFont(ofSize: 12, weight: .regular)
        durationLabel.font = .monospacedDigitSystemFont(ofSize: 12, weight: .regular)
        currentLabel.text = "00:00"
        durationLabel.text = "00:00"

        scrubber.minimumValue = 0
        scrubber.maximumValue = 1
        scrubber.minimumTrackTintColor = primaryColor
        scrubber.maximumTrackTintColor = primaryColor.withAlphaComponent(0.3)
        scrubber.thumbTintColor = primaryColor
        let thumbSize: CGFloat = 20
        let symbolConfig = UIImage.SymbolConfiguration(pointSize: thumbSize, weight: .regular, scale: .medium)
        let smallThumb = UIImage(systemName: "circle.fill", withConfiguration: symbolConfig)?
            .withRenderingMode(.alwaysTemplate).imageWith(color: primaryColor)

        scrubber.setThumbImage(smallThumb, for: .normal)
        scrubber.setThumbImage(smallThumb, for: .highlighted)
        scrubber.addTarget(self, action: #selector(scrubberChanged(_:)), for: .valueChanged)

        controlsStack.addArrangedSubview(playPauseButton)
        controlsStack.addArrangedSubview(currentLabel)
        controlsStack.addArrangedSubview(scrubber)
        controlsStack.addArrangedSubview(durationLabel)
        view.addSubview(controlsStack)
        
        playPauseButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            playPauseButton.widthAnchor.constraint(equalToConstant: 28),
            playPauseButton.heightAnchor.constraint(equalToConstant: 28),
        ])

        // WebView
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.isHidden = true
        view.addSubview(webView)

        // Layout
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),

            errorStack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            errorStack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            errorStack.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
            errorStack.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -24),

            // Image
            zoomScroll.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            zoomScroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            zoomScroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            imageView.topAnchor.constraint(equalTo: zoomScroll.topAnchor),
            imageView.bottomAnchor.constraint(equalTo: zoomScroll.bottomAnchor),
            imageView.leadingAnchor.constraint(equalTo: zoomScroll.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: zoomScroll.trailingAnchor),
            imageView.widthAnchor.constraint(equalTo: zoomScroll.widthAnchor),
            imageView.heightAnchor.constraint(equalTo: zoomScroll.heightAnchor),

            // Video scroll takes area above controls
            videoScroll.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            videoScroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            videoScroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            // Video container fills scroll view
            videoContainer.topAnchor.constraint(equalTo: videoScroll.topAnchor),
            videoContainer.bottomAnchor.constraint(equalTo: videoScroll.bottomAnchor),
            videoContainer.leadingAnchor.constraint(equalTo: videoScroll.leadingAnchor),
            videoContainer.trailingAnchor.constraint(equalTo: videoScroll.trailingAnchor),
            videoContainer.widthAnchor.constraint(equalTo: videoScroll.widthAnchor),
            videoContainer.heightAnchor.constraint(equalTo: videoScroll.heightAnchor),

            // Controls bottom
            controlsStack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 12),
            controlsStack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -12),
            controlsStack.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -8),

            // WebView covers all
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        NSLayoutConstraint.activate([
            zoomScroll.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -(navigationController?.navigationBar.frame.height ?? 0)),
        ])

        if controlsStack.superview != nil {
            if !(videoScroll.constraints.contains { $0.firstAnchor === videoScroll.bottomAnchor }) {
                videoScroll.bottomAnchor.constraint(equalTo: controlsStack.topAnchor, constant: -8).isActive = true
            }
        }

        playerLayer?.frame = videoContainer.bounds
    }

    // MARK: - Loading
    private func startLoading() {
        showLoading()
        loadTask?.cancel()
        loadTask = Task { [weak self] in
            guard let self else { return }
            do {
                let data = try await AnnouncementAttachmentsRepository.shared.data(for: item.id, authorId: PyrusServiceDesk.authorId ?? "")
                let fileURL = try await writeTempFile(data: data, preferredName: item.name ?? "")
                shareURL = fileURL
                await MainActor.run {
                    self.showContent(from: fileURL, data: data)
                }
            } catch {
                await MainActor.run {
                    self.showError(error)
                }
            }
        }
    }

    private func showLoading() {
        spinner.startAnimating()
        errorStack.isHidden = true
        webView.isHidden = true
        zoomScroll.isHidden = true
        videoScroll.isHidden = true
        controlsStack.isHidden = true
        cleanupPlayer()
    }

    private func showError(_ error: Error) {
        spinner.stopAnimating()
        webView.isHidden = true
        zoomScroll.isHidden = true
        videoScroll.isHidden = true
        controlsStack.isHidden = true
        cleanupPlayer()
        errorStack.isHidden = false

        let isOffline = (error as? URLError)?.code == .notConnectedToInternet
        errorLabel.text = isOffline ? "NoInternet".localizedPSD() : "FailedOpen".localizedPSD()
    }

    private func showContent(from url: URL, data: Data) {
        spinner.stopAnimating()
        errorStack.isHidden = true

        if isImage(url: url, data: data) {
            imageView.image = UIImage(data: data)
            zoomScroll.zoomScale = 1.0
            zoomScroll.isHidden = false
            webView.isHidden = true
            videoScroll.isHidden = true
            controlsStack.isHidden = true
            cleanupPlayer()
        } else if isVideo(url: url) {
            // Видео: показываем zoomable player и нижние контролы
            setupPlayer(with: url)
            videoScroll.zoomScale = 1.0
            videoScroll.isHidden = false
            controlsStack.isHidden = false
            zoomScroll.isHidden = true
            webView.isHidden = true
        } else {
            // Остальные файлы: через WebView
            webView.isHidden = false
            zoomScroll.isHidden = true
            videoScroll.isHidden = true
            controlsStack.isHidden = true
            cleanupPlayer()
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
    }

    // MARK: - Helpers
    private func isImage(url: URL, data: Data) -> Bool {
        if let type = UTType(filenameExtension: url.pathExtension.lowercased()),
           type.conforms(to: .image) {
            return true
        }
        return UIImage(data: data) != nil
    }

    private func isVideo(url: URL) -> Bool {
        guard let type = UTType(filenameExtension: url.pathExtension.lowercased()) else { return false }
        // учитываем фильм/аудиовизуальный контент
        return type.conforms(to: .movie) || type.conforms(to: .audiovisualContent)
    }

    private func writeTempFile(data: Data, preferredName: String) async throws -> URL {
        let ext = (preferredName as NSString).pathExtension
        let baseName = (preferredName as NSString).deletingPathExtension
        let safeBase = baseName.isEmpty ? "attachment" : baseName

        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("PSDAnnouncementsPreview", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        let filename = "\(safeBase)_\(UUID().uuidString)" + (ext.isEmpty ? "" : ".\(ext)")
        let url = dir.appendingPathComponent(filename)
        try data.write(to: url, options: .atomic)
        return url
    }

    // Прокси для шаринга в контейнер
    func triggerShare() {
        guard let url = shareURL else { return }
        delegate?.pageDidRequestShare(self, url: url)
    }

    // MARK: - Video player
    private func setupPlayer(with url: URL) {
        cleanupPlayer()

        let player = AVPlayer(url: url)
        player.actionAtItemEnd = .pause
        self.player = player

        let layer = AVPlayerLayer(player: player)
        layer.videoGravity = .resizeAspect
        playerLayer = layer
        videoContainer.layer.addSublayer(layer)
        layer.frame = videoContainer.bounds

        // Тайминги
        let interval = CMTime(seconds: 0.03, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self else { return }
            let duration = player.currentItem?.duration.seconds ?? 0
            self.updateTimeUI(current: time.seconds, duration: duration)
        }

        // Сброс UI
        updateTimeUI(current: 0, duration: player.currentItem?.duration.seconds ?? 0)

        // Готовность и автозапуск строго с начала
        if let item = player.currentItem {
            if item.status == .readyToPlay {
                startFromBeginningAndPlay(player)
            } else {
                // Снимем прошлое наблюдение (если было) и оформим новое
                statusObservation?.invalidate()
                statusObservation = item.observe(\.status, options: [.new]) { [weak self] observedItem, _ in
                    guard let self, let current = self.player?.currentItem, current === observedItem else { return }
                    if observedItem.status == .readyToPlay {
                        self.startFromBeginningAndPlay(player)
                        // наблюдение больше не нужно
                        self.statusObservation?.invalidate()
                        self.statusObservation = nil
                    }
                }
            }
        } else {
            // fallback
            startFromBeginningAndPlay(player)
        }

        // Окончание воспроизведения — перемотка в начало и кнопка "Play"
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.handlePlaybackEnded()
        }

        // Тап по видео — play/pause
        let tap = UITapGestureRecognizer(target: self, action: #selector(togglePlayPauseGesture))
        videoContainer.addGestureRecognizer(tap)
    }

    // Старт с нуля и воспроизведение в completion
    private func startFromBeginningAndPlay(_ player: AVPlayer) {
        player.seek(to: .zero, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
            guard let self else { return }
            if shouldPlay {
                player.play()
                self.playPauseButton.setImage(UIImage(systemName: "pause.fill"), for: .normal)
                self.updateTimeUI(current: player.currentTime().seconds, duration: player.currentItem?.duration.seconds ?? 0)
                shouldPlay = false
            }
        }
    }

    // KVO обработчик статуса item
    override func observeValue(forKeyPath keyPath: String?,
                               of object: Any?,
                               change: [NSKeyValueChangeKey : Any]?,
                               context: UnsafeMutableRawPointer?) {
        guard keyPath == "status",
              let item = object as? AVPlayerItem,
              item.status == .readyToPlay,
              let player, player.currentItem === item
        else { return }

        try? item.removeObserver(self, forKeyPath: "status")
        startFromBeginningAndPlay(player)
    }

    private func handlePlaybackEnded() {
        guard let player else { return }
        player.seek(to: .zero, toleranceBefore: .zero, toleranceAfter: .zero)
        playPauseButton.setImage(UIImage(systemName: "play.fill"), for: .normal)
        updateTimeUI(current: 0, duration: player.currentItem?.duration.seconds ?? 0)
    }

    private func cleanupPlayer() {
        if let obs = timeObserver, let player {
            player.removeTimeObserver(obs)
        }
        timeObserver = nil
        
        if let endObserver {
            NotificationCenter.default.removeObserver(endObserver)
            self.endObserver = nil
        }
        
        statusObservation?.invalidate()
        statusObservation = nil
        
        player?.pause()
        player = nil
        playerLayer?.removeFromSuperlayer()
        playerLayer = nil
    }


    private func removePlayerObservers() {
        cleanupPlayer()
    }

    // MARK: - Controls actions
    @objc private func togglePlayPauseGesture() {
        togglePlayPause()
    }

    private func togglePlayPause() {
        guard let player else { return }
        if player.timeControlStatus == .playing {
            player.pause()
            playPauseButton.setImage(UIImage(systemName: "play.fill"), for: .normal)
        } else {
            player.play()
            playPauseButton.setImage(UIImage(systemName: "pause.fill"), for: .normal)
        }
    }

    @objc private func scrubberChanged(_ sender: UISlider) {
        guard let player, let duration = player.currentItem?.duration.seconds, duration.isFinite, duration > 0 else { return }
        let target = Double(sender.value) * duration
        player.seek(to: CMTime(seconds: target, preferredTimescale: 600), toleranceBefore: .zero, toleranceAfter: .zero)
    }

    private func updateTimeUI(current: Double, duration: Double) {
        let dur = duration.isFinite ? max(duration, 0) : 0
        currentLabel.text = formatTime(current)
        durationLabel.text = formatTime(dur)
        if dur > 0 {
            scrubber.value = Float(current / dur)
        } else {
            scrubber.value = 0
        }
    }

    private func formatTime(_ seconds: Double) -> String {
        guard seconds.isFinite else { return "00:00" }
        let s = Int(seconds.rounded())
        let m = s / 60
        let sec = s % 60
        return String(format: "%02d:%02d", m, sec)
    }
}

// MARK: - Zoom
extension AttachmentPageViewController: UIScrollViewDelegate {
    func viewForZooming(in scrollView: UIScrollView) -> UIView? {
        if scrollView === zoomScroll { return imageView }
        if scrollView === videoScroll { return videoContainer }
        return nil
    }

    func scrollViewDidZoom(_ scrollView: UIScrollView) {
        let contentView: UIView
        if scrollView === zoomScroll {
            contentView = imageView
        } else if scrollView === videoScroll {
            contentView = videoContainer
        } else {
            return
        }

        let offsetX = max((scrollView.bounds.width - scrollView.contentSize.width) * 0.5, 0)
        let offsetY = max((scrollView.bounds.height - scrollView.contentSize.height) * 0.5, 0)
        contentView.center = CGPoint(x: scrollView.contentSize.width * 0.5 + offsetX,
                                     y: scrollView.contentSize.height * 0.5 + offsetY)
    }
}

// MARK: - Gesture delegate
extension AttachmentPageViewController: UIGestureRecognizerDelegate {
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard gestureRecognizer === panToDismiss else { return true }

        if !zoomScroll.isHidden {
            let atTop = zoomScroll.contentOffset.y <= -zoomScroll.adjustedContentInset.top + 0.5
            return zoomScroll.zoomScale <= 1.0001 && atTop
        }

        if !videoScroll.isHidden {
            let atTop = videoScroll.contentOffset.y <= -videoScroll.adjustedContentInset.top + 0.5
            return videoScroll.zoomScale <= 1.0001 && atTop
        }

        if !webView.isHidden {
            let s = webView.scrollView
            let atTop = s.contentOffset.y <= -s.adjustedContentInset.top + 0.5
            return atTop
        }

        return true
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer,
                           shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
}
