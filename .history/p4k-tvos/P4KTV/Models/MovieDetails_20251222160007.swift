import Foundation

struct MovieDetails: Codable {
    let id: String
    let title: String
    let description: String?
    let posterUrl: String?
    let videoUrl: String?
    let releaseDate: String?
    let rating: String?
    let genre: String?
    let runtime: String?
    let director: String?
    let writer: String?
    let cast: String?
    let trailerUrl: String?
    let streamUrl: String?
    
    // TV Series specific
    let season: [Season]?
    let relatedMovies: [Video]?
    
    enum CodingKeys: String, CodingKey {
        case id = "videos_id"
        case title
        case description
        case posterUrl = "poster_url"
        case videoUrl = "video_url"
        case releaseDate = "release"
        case rating
        case genre
        case runtime
        case director
        case writer
        case cast
        case trailerUrl = "trailer_url"
        case streamUrl = "stream_url"
        case season
        case relatedMovies = "related_movie"
    }
}

struct Season: Identifiable, Codable {
    let id: String
    let name: String
    let episodes: [Episode]?
    
    enum CodingKeys: String, CodingKey {
        case id = "seasons_id"
        case name = "seasons_name"
        case episodes
    }
}

struct Episode: Identifiable, Codable {
    let id: String
    let title: String
    let imageUrl: String?
    let fileUrl: String?
    let streamUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "episodes_id"
        case title = "episodes_name"
        case imageUrl = "image_url"
        case fileUrl = "file_url"
        case streamUrl = "stream_url"
    }
}
