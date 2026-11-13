import UIKit

///Вся работа с клавиатурой чата: инсеты перевёрнутой таблицы, офсеты
///и сдвиг кнопок скролла/стопа. Контроллер только пробрасывает события.
///
///Поведение по версиям iOS:
///- iOS < 26: willShow — общая ветка, willHide — простое восстановление инсета по высоте бара.
///- iOS 26: willShow — своя ветка (layoutIfNeeded, клэмп офсета), willHide — общая для 26+,
///  инсет покоя = высота бара + safe area.
///- iOS 27+: willShow — общая ветка (как до 26), willHide — общая для 26+,
///  но safe area добирается отдельно и устаревший инсет сбрасывается.
final class PSDChatKeyboardHandler: NSObject {
    ///Какой набор клавиатурных веток действует на текущей системе.
    private enum KeyboardEnvironment {
        ///iOS < 26.
        case legacy
        ///Только iOS 26.x.
        case ios26
        ///iOS 27 и новее.
        case ios27Plus
        
        static var current: KeyboardEnvironment {
            if #available(iOS 27.0, *) { return .ios27Plus }
            if #available(iOS 26.0, *) { return .ios26 }
            return .legacy
        }
    }
    
    private enum Constants {
        static let staleKeyboardInsetThreshold: CGFloat = 150
        static let safeAreaDetectTolerance: CGFloat = 10
        static let buttonsHiddenBottomOffset: CGFloat = -110
        static let inactiveChatInset: CGFloat = 90
        static let realHideHeightTolerance: CGFloat = 20
        static let restOffsetTolerance: CGFloat = 1
    }
    
    private let environment = KeyboardEnvironment.current
    
    private weak var tableView: PSDChatTableView?
    private weak var messageInputView: PSDMessageInputView?
    private weak var containerView: UIView?
    
    var bottomScrollButton: NSLayoutConstraint?
    var bottomStopButton: NSLayoutConstraint?
    var isChatActive = true
    
    private(set) var isKeyboardOpen = false
    private var isFirstKeyboardShow = true
    private var currentKeyboardHeight: CGFloat = 0
    private var defaultMessageInputViewHeight: CGFloat = 0
    private var hasAttachmentsInset = false
    private var oldInputViewHeight: CGFloat = 0
    private var isAddButtonTapped = false
    private var appliedRateInset: CGFloat = 0
    
    init(tableView: PSDChatTableView,
         messageInputView: PSDMessageInputView,
         containerView: UIView) {
        self.tableView = tableView
        self.messageInputView = messageInputView
        self.containerView = containerView
        super.init()
    }
    
    deinit {
        stopObserving()
    }
    
    // MARK: - Observing
    
    func startObserving() {
        stopObserving()
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        center.addObserver(self, selector: #selector(keyboardWillHide(_:)), name: UIResponder.keyboardWillHideNotification, object: nil)
        center.addObserver(self, selector: #selector(keyboardDidHide(_:)), name: UIResponder.keyboardDidHideNotification, object: nil)
    }
    
    func stopObserving() {
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - Public state
    
    ///Bottom of the flipped table is -contentInset.top; never pass less while keyboard is closed.
    var effectiveKeyboardHeight: CGFloat {
        guard let tableView = tableView else { return currentKeyboardHeight }
        guard !isKeyboardOpen else { return currentKeyboardHeight }
        return max(currentKeyboardHeight, tableView.contentInset.top)
    }
    
    func markAddButtonTapped() {
        isAddButtonTapped = true
    }
    
    // MARK: - Inset adjustments
    
    func handleAttachmentAdded() {
        guard let tableView = tableView else { return }
        hasAttachmentsInset = true
        logState("addAttachment")
        tableView.contentInset.top += PSDMessageInputView.attachmentsHeight
        tableView.contentOffset.y -= PSDMessageInputView.attachmentsHeight
    }
    
    func handleAllAttachmentsDeleted() {
        guard let tableView = tableView else { return }
        switch environment {
        case .ios27Plus:
            //Also called on send and on chat setup when no attachments were shown.
            guard hasAttachmentsInset else { return }
            hasAttachmentsInset = false
            //Exact inverse of handleAttachmentAdded(). Offset is assigned absolutely: shrinking
            //the inset clamps the out-of-range offset by itself, so a relative "+=" would double the shift.
            let newInset = tableView.contentInset.top - PSDMessageInputView.attachmentsHeight
            let newOffset = tableView.contentOffset.y + PSDMessageInputView.attachmentsHeight
            logState("deletedAllAttachments27")
            tableView.contentInset.top = newInset
            tableView.contentOffset.y = newOffset
        case .ios26:
            //Also called on send and on chat setup when no attachments were shown.
            guard hasAttachmentsInset else { return }
            hasAttachmentsInset = false
            if isKeyboardOpen {
                logState("deletedAllAttachments26 kbOpen")
                tableView.contentInset.top = currentKeyboardHeight
            } else {
                logState("deletedAllAttachments26")
                tableView.contentInset.top = defaultMessageInputViewHeight
            }
            //Behind a fullscreen modal UIKit does not re-clamp the offset to the new inset.
            clampOffsetToRestPosition()
        case .legacy:
            break
        }
    }
    
    func applyRateInset(_ rateHeight: CGFloat) {
        guard let tableView = tableView else { return }
        logState("rate inset")
        appliedRateInset += rateHeight
        tableView.contentInset.top += rateHeight
        tableView.contentOffset.y -= rateHeight
    }
    
    func applyInactiveInset() {
        guard let tableView = tableView, let containerView = containerView else { return }
        logState("inset=90+safe")
        tableView.contentInset.top = Constants.inactiveChatInset + containerView.safeAreaInsets.bottom
    }
    
    ///Keeps bottom gap in sync with input view when keyboard is closed and notifications were not delivered.
    func syncInsetWithInputViewIfNeeded() {
        guard let tableView = tableView,
              let messageInputView = messageInputView,
              let containerView = containerView
        else { return }
        //iOS 26: после фантомного willShow isKeyboardOpen может остаться true без
        //responder'а и без клавиатуры на экране — willHide/didHide уже не придут.
        if environment == .ios26,
           isKeyboardOpen,
           !messageInputView.inputTextView.isFirstResponder,
           let gap = inputViewGapBelow(),
           gap <= Constants.safeAreaDetectTolerance {
            logState("sync reset stuck kbOpen")
            isKeyboardOpen = false
        }
        guard isChatActive, !isKeyboardOpen, !messageInputView.inputTextView.isFirstResponder else { return }
        var barHeight = messageInputView.frame.size.height
        if messageInputView.window == nil {
            //Before the accessory is attached its frame carries a temporary layout height.
            let fitting = messageInputView.systemLayoutSizeFitting(
                CGSize(width: containerView.bounds.width, height: UIView.layoutFittingCompressedSize.height),
                withHorizontalFittingPriority: .required,
                verticalFittingPriority: .fittingSizeLevel
            ).height
            barHeight = max(barHeight, fitting)
        }
        var targetInset = barHeight
        if environment == .ios27Plus {
            //Accessory frame pinned to the screen edge already contains the safe area.
            //System alerts report a transient gap below the bar; track only the bar height then.
            let insetMatchesBar = tableView.contentInset.top >= targetInset - Constants.safeAreaDetectTolerance
            if !insetMatchesBar {
                let safeBottom = containerView.safeAreaInsets.bottom
                targetInset += min(inputViewGapBelow() ?? safeBottom, safeBottom)
            }
        }
        guard targetInset > 0 else { return }
        
        reconcileSettledStateOnIOS26(targetInset: targetInset)
        
        //iOS 26+: закрытие полноэкранной модалки может переиграть willShow с клавиатурным
        //фреймом при ещё не сброшенном isKeyboardOpen — инсет остаётся клавиатурным,
        //а willHide, который бы его починил, уже не придёт. На iOS 27+ это чинит
        //resetStaleKeyboardInsetIfNeeded, на iOS 26 такого пути нет — чиним на layout.
        //Порог отсекает переходную геометрию бара во время модальных анимаций.
        if environment != .legacy,
           tableView.contentInset.top - targetInset > Constants.staleKeyboardInsetThreshold {
            logState("syncInset staleShrink target=\(targetInset)")
            tableView.contentInset.top = targetInset
            if tableView.contentOffset.y < -targetInset {
                tableView.contentOffset.y = -targetInset
            }
            currentKeyboardHeight = targetInset
            return
        }
        
        guard tableView.contentInset.top < targetInset else { return }
        let delta = targetInset - tableView.contentInset.top
        //Rounding-sized corrections must not visibly push the content after the chat is on screen.
        guard delta >= Constants.safeAreaDetectTolerance else { return }
        logState("syncInset apply target=\(targetInset)")
        tableView.contentInset.top = targetInset
        tableView.contentOffset.y -= delta
    }
    
    ///Только iOS 26: аналог resetStaleKeyboardInsetIfNeeded с iOS 27+.
    ///Закрытие полноэкранной модалки может проиграть willShow с мгновенно
    ///восстановленным responder'ом — инсет/офсет остаются клавиатурными,
    ///а willHide уже не придёт. Чиним пару в покое: ужимаем лишний инсет
    ///и клэмпим офсет к позиции покоя.
    private func reconcileSettledStateOnIOS26(targetInset: CGFloat) {
        guard environment == .ios26,
              let tableView = tableView,
              !isScrollInteractionActive(tableView)
        else { return }
        //В переходной геометрии модальных анимаций бар не прижат к низу экрана.
        guard (inputViewGapBelow() ?? .greatestFiniteMagnitude) <= Constants.safeAreaDetectTolerance else { return }
        
        let expectedRest = targetInset + restingInsetExtras()
        if tableView.contentInset.top > expectedRest + Constants.safeAreaDetectTolerance {
            logState("settle26 shrink expected=\(expectedRest)")
            tableView.contentInset.top = expectedRest
            currentKeyboardHeight = expectedRest
        }
        let restOffset = -tableView.contentInset.top
        if tableView.contentOffset.y < restOffset - Constants.restOffsetTolerance {
            logState("settle26 clampOffset")
            tableView.contentOffset.y = restOffset
        }
    }
    
    ///Легитимные добавки поверх инсета покоя.
    private func restingInsetExtras() -> CGFloat {
        var extras = appliedRateInset
        if hasAttachmentsInset {
            extras += PSDMessageInputView.attachmentsHeight
        }
        return extras
    }
    
    private func isScrollInteractionActive(_ tableView: UITableView) -> Bool {
        tableView.isTracking || tableView.isDragging || tableView.isDecelerating
    }
    
    //TEMP diagnostics, remove after the open-jump bug is found.
    func logState(_ context: String) {
        guard let tableView = tableView, let messageInputView = messageInputView else { return }
        let adjusted = tableView.adjustedContentInset
        print("[PSDChat] \(context) | inset.top=\(tableView.contentInset.top) inset.bottom=\(tableView.contentInset.bottom) adj.top=\(adjusted.top) adj.bottom=\(adjusted.bottom) offset.y=\(tableView.contentOffset.y) inputH=\(messageInputView.frame.size.height) gapBelow=\(String(describing: inputViewGapBelow())) kbOpen=\(isKeyboardOpen) firstResp=\(messageInputView.inputTextView.isFirstResponder)")
    }
    
    // MARK: - keyboardWillShow (общая точка входа)
    
    @objc private func keyboardWillShow(_ notification: NSNotification) {
        guard let endFrame = keyboardEndFrame(from: notification), endFrame.height != 0 else { return }
        switch environment {
        case .ios26:
            handleKeyboardWillShowOnIOS26(notification, endFrame: endFrame)
        case .legacy, .ios27Plus:
            handleKeyboardWillShowLegacy(notification, endFrame: endFrame)
        }
    }
    
    // MARK: - keyboardWillShow: только iOS 26
    
    private func handleKeyboardWillShowOnIOS26(_ notification: NSNotification, endFrame: CGRect) {
        guard let tableView = tableView,
              let messageInputView = messageInputView,
              let containerView = containerView
        else { return }
        
        let keyboardHeight = endFrame.height
        
        //Dismissing a fullscreen modal replays a keyboard-sized willShow with no responder.
        if isStaleWillShow(keyboardHeight: keyboardHeight) {
            clampOffsetToRestPosition()
            logState("willShow26 skipped, no responder kbH=\(keyboardHeight)")
            return
        }
        
        //Оторванную (плавающую) клавиатуру при уже открытой не трогаем вовсе.
        let detachedWhileOpen = isKeyboardDetached(notification, endFrame: endFrame) && isKeyboardOpen
        let oldInset = tableView.contentInset.top
        let oldOffset = tableView.contentOffset.y
        let wasKeyboardOpen = isKeyboardOpen
        let wasFirstKeyboardShow = isFirstKeyboardShow
        let oldDefaultInputHeight = defaultMessageInputViewHeight
        //Дельта — от текущего инсета, а не от currentKeyboardHeight: ниже инсет
        //ставится абсолютно, и офсет должен сдвинуться ровно на его изменение.
        //currentKeyboardHeight отстаёт от инсета после addAttachment/needShowRate
        //и после keyboardWillHide, и офсет по нему уезжал на эту разницу.
        let delta = keyboardHeight - oldInset
        currentKeyboardHeight = keyboardHeight
        
        let shouldShiftContent = (messageInputView.inputTextView.isFirstResponder || !isKeyboardOpen) && !detachedWhileOpen
        if shouldShiftContent {
            if messageInputView.inputTextView.isFirstResponder {
                liftBottomButtons(by: delta)
                containerView.layoutIfNeeded()
            }
            
            if isFirstKeyboardShow {
                defaultMessageInputViewHeight = messageInputView.frame.height
                if PyrusServiceDesk.startWithPush {
                    UIView.performWithoutAnimation {
                        tableView.contentOffset.y -= delta
                    }
                } else {
                    tableView.contentOffset.y -= delta
                }
                isFirstKeyboardShow = false
            } else if delta > 0 {
                tableView.contentOffset.y -= delta
            }
        }
        
        if !detachedWhileOpen {
            //Responder исчез внутри вызова (диссмисс полноэкранной модалки): клавиатурный
            //willShow фантомный, клавиатура не появится и willHide не придёт.
            //Откатываем сдвиг и не применяем клавиатурный инсет.
            let isPhantomShow = !messageInputView.inputTextView.isFirstResponder
                && !wasKeyboardOpen
                && keyboardHeight >= Constants.staleKeyboardInsetThreshold
            if isPhantomShow {
                logState("willShow26 phantom revert")
                if shouldShiftContent {
                    tableView.contentOffset.y = oldOffset
                }
                isFirstKeyboardShow = wasFirstKeyboardShow
                defaultMessageInputViewHeight = oldDefaultInputHeight
                currentKeyboardHeight = tableView.contentInset.top
                restBottomButtons()
                clampOffsetToRestPosition()
                return
            }
            logState("willShow26 set inset")
            tableView.contentInset.top = keyboardHeight
            isKeyboardOpen = messageInputView.inputTextView.isFirstResponder
            if !messageInputView.inputTextView.isFirstResponder {
                clampOffsetToRestPosition()
            }
        } else {
            logState("willShow26 detached skip")
        }
    }
    
    // MARK: - keyboardWillShow: iOS < 26 и iOS 27+
    
    private func handleKeyboardWillShowLegacy(_ notification: NSNotification, endFrame: CGRect) {
        guard let tableView = tableView,
              let messageInputView = messageInputView
        else { return }
        
        let keyboardHeight = endFrame.height
        
        //Dismissing a fullscreen modal replays a keyboard-sized willShow with no responder.
        if isStaleWillShow(keyboardHeight: keyboardHeight) {
            clampOffsetToRestPosition()
            logState("willShowLegacy skipped, no responder kbH=\(keyboardHeight)")
            return
        }
        
        //Оторванную (плавающую) клавиатуру при уже открытой не трогаем вовсе.
        let detachedWhileOpen = isKeyboardDetached(notification, endFrame: endFrame) && isKeyboardOpen
        currentKeyboardHeight = keyboardHeight
        
        if defaultMessageInputViewHeight == 0 {
            defaultMessageInputViewHeight = messageInputView.frame.height
        }
        
        let oldInset = tableView.contentInset.top
        let delta = keyboardHeight - oldInset
        
        let shouldShiftContent = (messageInputView.inputTextView.isFirstResponder || !isKeyboardOpen) && !detachedWhileOpen
        if shouldShiftContent {
            if messageInputView.inputTextView.isFirstResponder {
                liftBottomButtons(by: delta)
            }
            
            if isFirstKeyboardShow && (PyrusServiceDesk.multichats || PyrusServiceDesk.startWithPush) {
                UIView.performWithoutAnimation {
                    tableView.contentOffset.y -= delta
                }
                isFirstKeyboardShow = false
            } else {
                tableView.contentOffset.y -= delta
            }
        }
        
        logState("willShowLegacy set inset")
        if !detachedWhileOpen {
            tableView.contentInset.top = keyboardHeight
            isKeyboardOpen = messageInputView.inputTextView.isFirstResponder
        }
    }
    
    // MARK: - keyboardWillHide (все версии, ветвление внутри)
    
    @objc private func keyboardWillHide(_ notification: NSNotification) {
        guard let tableView = tableView,
              let messageInputView = messageInputView,
              let containerView = containerView,
              let endFrame = keyboardEndFrame(from: notification)
        else { return }
        
        let keyboardHeight = endFrame.height
        let duration = keyboardAnimationDuration(notification)
        currentKeyboardHeight = keyboardHeight
        
        UIView.animate(withDuration: duration, delay: 0, animations: {
            self.restBottomButtons()
            containerView.layoutIfNeeded()
            
            if !self.isChatActive {
                self.logState("willHide !isActive inset=90")
                tableView.contentInset.top = Constants.inactiveChatInset
                return
            }
            
            //The bar-transition guard must not swallow a real hide: its frame is keyboard-sized.
            let isBarTransitionOnly = (self.oldInputViewHeight - messageInputView.frame.size.height).rounded() == containerView.safeAreaInsets.bottom
                && keyboardHeight < Constants.staleKeyboardInsetThreshold
            guard !isBarTransitionOnly else { return }
            
            switch self.environment {
            case .legacy:
                self.logState("willHide pre26 set inset")
                tableView.contentInset.top = messageInputView.frame.size.height
            case .ios26, .ios27Plus:
                self.restoreRestingInsetOnIOS26AndLater(keyboardHeight: keyboardHeight)
            }
            self.isAddButtonTapped = false
        })
        
        oldInputViewHeight = messageInputView.frame.size.height
    }
    
    // MARK: - keyboardWillHide: iOS 26+
    
    ///Восстановление инсета покоя после скрытия клавиатуры на iOS 26 и новее.
    private func restoreRestingInsetOnIOS26AndLater(keyboardHeight: CGFloat) {
        guard let tableView = tableView else { return }
        
        let keyboardInsetAbsent = tableView.contentInset.top <= settledInsetCeiling()
        //Attach button dismisses the keyboard, but the first willHide carries only the accessory frame.
        let isRealHide = abs(keyboardHeight - defaultMessageInputViewHeight) > Constants.realHideHeightTolerance
            || isAddButtonTapped
        
        if isRealHide {
            //Late willHide with a keyboard-sized frame after the inset already settled.
            guard !keyboardInsetAbsent else { return }
            applyRealHideInset()
        } else {
            resetStaleKeyboardInsetIfNeeded()
        }
    }
    
    ///Потолок «инсет уже в покое»: iOS 26 — высота бара + safe area, iOS 27+ — только высота бара.
    private func settledInsetCeiling() -> CGFloat {
        guard let messageInputView = messageInputView, let containerView = containerView else { return 0 }
        var ceiling = messageInputView.frame.size.height + Constants.safeAreaDetectTolerance
        if environment == .ios26 {
            //On iOS 26 the resting inset is inputH + safe area.
            ceiling += containerView.safeAreaInsets.bottom
        }
        return ceiling
    }
    
    private func applyRealHideInset() {
        guard let tableView = tableView, let messageInputView = messageInputView else { return }
        let extra = realHideSafeAreaExtra()
        logState("willHide realHide extra=\(extra)")
        let newInset = messageInputView.frame.size.height + extra
        tableView.contentInset.top = newInset
        tableView.contentOffset.y -= extra
        //Behind a fullscreen modal UIKit does not re-clamp the offset to the new inset.
        if tableView.contentOffset.y < -newInset {
            tableView.contentOffset.y = -newInset
        }
        //Stale keyboard height makes effectiveKeyboardHeight overscroll on next focus.
        currentKeyboardHeight = newInset
    }
    
    ///Добавка safe area к инсету покоя. Нужна только на iOS 27+:
    ///на iOS 26 safe area уже входит в высоту бара.
    private func realHideSafeAreaExtra() -> CGFloat {
        guard environment == .ios27Plus, let containerView = containerView else { return 0 }
        //The bar's target height already contains the safe area; the momentary gap
        //is unreliable while the keyboard or a modal is still on screen.
        if inputViewFrameIncludesSafeArea() {
            return 0
        }
        return min(inputViewGapBelow() ?? containerView.safeAreaInsets.bottom,
                   containerView.safeAreaInsets.bottom)
    }
    
    ///Только iOS 27+. Repeated willHide with accessory-only frame must not strip safe area inset.
    private func resetStaleKeyboardInsetIfNeeded() {
        guard environment == .ios27Plus,
              let tableView = tableView,
              let messageInputView = messageInputView,
              let containerView = containerView
        else { return }
        
        let gapBelow = inputViewGapBelow()
        let safeBottom = containerView.safeAreaInsets.bottom
        let accessoryExtra = min(gapBelow ?? safeBottom, safeBottom)
        var expectedInset = messageInputView.frame.size.height + accessoryExtra
        if defaultMessageInputViewHeight > 0 {
            //The bar may still be in transient geometry (no safe area in height, gap 0).
            expectedInset = max(expectedInset, defaultMessageInputViewHeight + safeBottom)
        }
        //gapBelow ~ keyboard height means the keyboard is actually on screen.
        let keyboardReallyGone = (gapBelow ?? 0) <= safeBottom + Constants.realHideHeightTolerance
        guard !isKeyboardOpen,
              keyboardReallyGone,
              tableView.contentInset.top - expectedInset > Constants.staleKeyboardInsetThreshold
        else { return }
        
        //Fullscreen modal dismissed the keyboard, but its willHide was skipped by the guards.
        logState("willHide staleReset")
        tableView.contentInset.top = expectedInset
        if tableView.contentOffset.y < -expectedInset {
            tableView.contentOffset.y = -expectedInset
        }
        currentKeyboardHeight = expectedInset
    }
    
    @objc private func keyboardDidHide(_ notification: NSNotification) {
        isKeyboardOpen = false
        logState("didHide")
    }
    
    // MARK: - Helpers (все версии)
    
    private func keyboardEndFrame(from notification: NSNotification) -> CGRect? {
        (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue
    }
    
    private func keyboardAnimationDuration(_ notification: NSNotification) -> TimeInterval {
        if let duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSValue {
            return duration as? TimeInterval ?? 0
        }
        return 0
    }
    
    ///Dismissing a fullscreen modal replays a keyboard-sized willShow with no responder.
    private func isStaleWillShow(keyboardHeight: CGFloat) -> Bool {
        guard let messageInputView = messageInputView else { return false }
        return !messageInputView.inputTextView.isFirstResponder
            && !isKeyboardOpen
            && keyboardHeight >= Constants.staleKeyboardInsetThreshold
    }
    
    ///Detects detached (floating/undocked) keyboard. Falls back to end frame when private key is missing (iOS 27+).
    private func isKeyboardDetached(_ notification: NSNotification, endFrame: CGRect) -> Bool {
        if let center = (notification.userInfo?["UIKeyboardCenterBeginUserInfoKey"] as? NSValue)?.cgPointValue {
            return center.y > (containerView?.frame.maxY ?? 0)
        }
        return endFrame.minY >= UIScreen.main.bounds.maxY
    }
    
    ///In the flipped table offset below -inset.top is never a valid rest position.
    private func clampOffsetToRestPosition() {
        guard let tableView = tableView else { return }
        let restOffset = -tableView.contentInset.top
        if tableView.contentOffset.y < restOffset {
            tableView.contentOffset.y = restOffset
        }
    }
    
    private func liftBottomButtons(by delta: CGFloat) {
        bottomScrollButton?.constant -= delta
        bottomStopButton?.constant -= delta
    }
    
    private func restBottomButtons() {
        bottomScrollButton?.constant = Constants.buttonsHiddenBottomOffset
        bottomStopButton?.constant = Constants.buttonsHiddenBottomOffset
    }
    
    // MARK: - Helpers (только iOS 27+)
    
    ///Gap between the accessory's bottom and its window's bottom; nil before it is attached.
    private func inputViewGapBelow() -> CGFloat? {
        guard let messageInputView = messageInputView,
              let inputWindow = messageInputView.window
        else { return nil }
        let frameInWindow = inputWindow.convert(messageInputView.bounds, from: messageInputView)
        return max(0, inputWindow.bounds.maxY - frameInWindow.maxY)
    }
    
    ///True when the accessory frame is pinned to the screen edge and already contains the safe area.
    private func inputViewFrameIncludesSafeArea() -> Bool {
        guard let messageInputView = messageInputView,
              let containerView = containerView,
              defaultMessageInputViewHeight > 0
        else { return false }
        var diff = messageInputView.frame.size.height - defaultMessageInputViewHeight
        let attachmentsHeight = PSDMessageInputView.attachmentsHeight
        while attachmentsHeight > 0, diff >= attachmentsHeight {
            diff -= attachmentsHeight
        }
        return abs(diff - containerView.safeAreaInsets.bottom) < Constants.safeAreaDetectTolerance
    }
    
    // MARK: - Legacy helpers (не используются)
    
    private func needChangeInset(_ newInset: CGFloat) -> Bool {
        guard let tableView = tableView else { return true }
        if
            let presentedViewController = tableView.findViewController()?.presentedViewController,
            presentedViewController.isBeingDismissed
        {
            return tableView.contentInset.bottom <= newInset
        }
        return true
    }
    
    private func newOffset(delta: CGFloat, bottomInset: CGFloat) -> CGFloat {
        guard let tableView = tableView else { return 0 }
        var newOffsetY = max(0, tableView.contentOffset.y + delta)//block too little offset
        newOffsetY = min(tableView.contentSize.height - (tableView.frame.size.height - bottomInset), newOffsetY)//block too big offset
        return newOffsetY
    }
    
    private func needChangeOffset(keyboardHeight: CGFloat) -> Bool {
        guard let tableView = tableView else { return false }
        return tableView.contentSize.height > (tableView.frame.size.height - keyboardHeight) && !tableView.isDragging
    }
}
