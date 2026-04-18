import Foundation

struct Video: Identifiable, Codable {
    let id: String
    let title: String
    let description: String?
    let posterUrl: String?
    let videoUrl: String?
    let videoType: String? // "mp4", "mkv", "hls"
    let category: String? // "movie", "tvseries"
    let releaseDate: String?
    let rating: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "videos_id"
        case title
        case description
        case posterUrl = "poster_url"
        case videoUrl = "video_url"
        case videoType = "video_type"
        case category
        case releaseDate = "release"
        case rating
    }
}

struct Section: Identifiable {
    let id = UUID()
    let title: String
    let videos: [Video]
}
