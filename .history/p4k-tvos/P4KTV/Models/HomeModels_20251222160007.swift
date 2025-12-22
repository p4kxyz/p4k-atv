import Foundation

struct HomeResponse: Codable {
    let slider: SliderData?
    let latestMovies: [Video]?
    let latestTvSeries: [Video]?
    let featuresGenreAndMovie: [GenreWithMovies]?
    
    enum CodingKeys: String, CodingKey {
        case slider
        case latestMovies = "latest_movies"
        case latestTvSeries = "latest_tvseries"
        case featuresGenreAndMovie = "features_genre_and_movie"
    }
}

struct SliderData: Codable {
    let slide: [SliderItem]?
}

struct SliderItem: Identifiable, Codable {
    let id: String
    let title: String
    let description: String?
    let imageUrl: String?
    let actionUrl: String?
    let actionId: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case description
        case imageUrl = "image_link"
        case actionUrl = "action_url"
        case actionId = "action_id"
    }
}

struct GenreWithMovies: Identifiable, Codable {
    let id: String
    let name: String
    let videos: [Video]?
    
    enum CodingKeys: String, CodingKey {
        case id = "genre_id"
        case name = "genre_name"
        case videos
    }
}

// Updating Video struct to match API response better
extension Video {
    // Add any extra computed properties or helper methods here if needed
}
