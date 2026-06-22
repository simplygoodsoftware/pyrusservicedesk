struct PSDRatingSettings: Codable {
    let size: Int
    let type: Int
    let ratingTextValues: [RatingTextValue]?
    var ratingText: String?
    
    enum CodingKeys: String, CodingKey {
        case size
        case type
        case ratingTextValues = "rating_text_values"
    }
}

struct RatingTextValue: Codable {
    let rating: Int
    let text: String
}

enum RatingType: Int {
    case smile = 1
    case like = 2
    case text = 3
    
    func rateArray(size: Int) -> [RatingTextValue] {
        switch self {
        case .smile:
            switch size {
            case 2:
                return [RatingTextValue(rating: 1, text: "😩"), RatingTextValue(rating: 5, text: "😄")]
            case 3:
                return [RatingTextValue(rating: 1, text: "😩"), RatingTextValue(rating: 3, text: "😐"), RatingTextValue(rating: 5, text: "😄")]
            default:
                return [RatingTextValue(rating: 1, text: "😩"), RatingTextValue(rating: 2, text: "🙁"), RatingTextValue(rating: 3, text: "😐"), RatingTextValue(rating: 4, text: "🙂"), RatingTextValue(rating: 5, text: "😄")]
            }
        case .like:
            return [RatingTextValue(rating: 1, text: "👎"), RatingTextValue(rating: 5, text: "👍")]
        case .text:
            return []
        }
    }
}
