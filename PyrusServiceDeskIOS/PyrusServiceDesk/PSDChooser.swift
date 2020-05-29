import Foundation
///Interface with label(String) and chooserDelegate(FileChooserDelegate).
@objc public protocol FileChooser{
    ///Is name that need show to user
    @objc var label : String { get set}
    ///Protocol to send status messages of completion.
    @objc weak var chooserDelegate : FileChooserDelegate? { get set }
}
///Protocol to send status messages of completion.
@objc public protocol FileChooserDelegate{
    ///Send succes status
    ///- parameter data: Data that need to send attachment to chat. If empty or nil will show error alert.
    @objc func didEndWithSuccess(_ data : Data?, url : URL?)
    ///Send cancel status. Will do nothing. Just close.
    @objc func didEndWithCancel()
}

