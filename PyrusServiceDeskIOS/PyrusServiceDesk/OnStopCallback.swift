import Foundation
///The protocol for getting a notification that PyrusServiceDesk was closed
@objc public protocol OnStopCallback{
    ///The callback that PyrusServiceDesk was closed
    @objc func onStop()
}
