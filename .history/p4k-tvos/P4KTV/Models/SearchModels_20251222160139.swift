import Foundation

struct Genre: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    
    enum CodingKeys: String, CodingKey {
        case id = "genre_id"
        case name
    }
}

struct SearchResponse: Codable {
    let movie: [Video]?
    let tvseries: [Video]?
    let tvChannels: [Video]?
    
    enum CodingKeys: String, CodingKey {
        case movie
        case tvseries
        case tvChannels = "tv_channels"
    }
}
