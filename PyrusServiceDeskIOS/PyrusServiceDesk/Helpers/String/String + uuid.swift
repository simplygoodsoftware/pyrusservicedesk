//
//  String + uuid.swift
//  PyrusServiceDesk
//
//  Created by  Галина Муравьева on 03/02/2020.
//  Copyright © 2020  Галина Муравьева. All rights reserved.
//

import Foundation
extension String {
    private static let uuidLength = 75
    static func getUiqueString()->String{
        guard let a = NSMutableData(length: String.uuidLength) else{
            return UUID().uuidString
        }
        let status = SecRandomCopyBytes(kSecRandomDefault, a.count, a.mutableBytes)
        if status ==  errSecSuccess {
            let c = a.base64EncodedString()
            return c
        }
        return UUID().uuidString
    }
}
