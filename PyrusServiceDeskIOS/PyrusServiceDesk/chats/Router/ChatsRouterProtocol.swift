//
//  ChatsRouterProtocol.swift
//  Helpy
//
//  Created by Станислава Бобрускина on 17.10.2024.
//  Copyright © 2024 Pyrus. All rights reserved.
//

import Foundation

protocol ChatsRouterProtocol: NSObjectProtocol {
    func route(to destination: ChatsRouterDestination)
}

enum ChatsRouterDestination {
    case chat(chat: PSDChat)
    case goBack
}
