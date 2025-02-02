import Foundation

protocol PSDChatsDataServiceProtocol {
    func saveChatModels(with chatModels: [PSDChat])
    func saveChatModel(with chatModel: PSDChat)
//    func saveMessagesModels(with messageModels: [MessageModel], in channelModel: ChannelModel)
//    func saveMessageModel(with messageModel: MessageModel, in channelModel: ChannelModel)
    func deleteChats(chatModels: [PSDChat])
    func getAllChats() -> [PSDChat]
//    func getMessages(for channelUUID: String) -> [MessageModel]
    func deleteChannel(with chatId: Int)
}
