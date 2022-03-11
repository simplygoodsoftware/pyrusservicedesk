
import Foundation
extension UIView {
    /**
     Add constraint to view superview. Attribute add to same superview's attribute
     - Parameter attributes: [NSLayoutConstraint.Attribute]
     - Parameter constant: CGFloat - The constant added to the multiplied attribute value on the right side of the constraint to yield the final modified attribute.
     */
    func addConstraint(_ attributes:[NSLayoutConstraint.Attribute], constant: CGFloat){
        for i in attributes{
            self.superview?.addConstraint(NSLayoutConstraint(
                item: self,
                attribute: i,
                relatedBy: .equal,
                toItem: self.superview,
                attribute: i,
                multiplier: 1,
                constant:constant))
        }
    }
    /**
     Add constraint to view superview, constant = 0. Attribute add to same superview's attribute
     - Parameter attributes: [NSLayoutConstraint.Attribute]
     */
    func addZeroConstraint(_ attributes:[NSLayoutConstraint.Attribute]){
        self.addConstraint(attributes,constant:0)
    }
    /**
     Add constraint to view superview. Only size attribute
     - Parameter attributes: [NSLayoutConstraint.Attribute]
     */
    func addSizeConstraint(_ attributes:[NSLayoutConstraint.Attribute], constant: CGFloat){
        for i in attributes{
            self.superview?.addConstraint(NSLayoutConstraint(
                item: self,
                attribute: i,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1,
                constant:constant))
        }
    }
}
