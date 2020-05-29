import Foundation
///The protocol for sending a notification that a new message has arrived
@objc public protocol NewReplySubscriber{
    ///The new message was send
    @objc func onNewReply()
}
