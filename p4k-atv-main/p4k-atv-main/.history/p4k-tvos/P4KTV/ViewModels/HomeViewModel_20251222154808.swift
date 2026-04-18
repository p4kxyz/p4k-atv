import Foundation
import Combine

class HomeViewModel: ObservableObject {
    @Published var featuredVideo: Video?
    @Published var sections: [Section] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    func loadData() {
        self.isLoading = true
        self.errorMessage = nil
        
        NetworkManager.shared.fetchHomeContent { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let response):
                    self?.processResponse(response)
                case .failure(let error):
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    private func processResponse(_ response: HomeResponse) {
        var newSections: [Section] = []
        
        // 1. Process Slider (Featured)
        if let slides = response.slider?.slide, let firstSlide = slides.first {
            // Convert SliderItem to Video for HeroBanner
            self.featuredVideo = Video(
                id: firstSlide.actionId ?? "0",
                title: firstSlide.title,
                description: firstSlide.description,
                posterUrl: firstSlide.imageUrl,
                videoUrl: nil, // Will be fetched in detail
                videoType: nil,
                category: "movie", // Default
                releaseDate: nil,
                rating: nil
            )
        }
        
        // 2. Latest Movies
        if let movies = response.latestMovies, !movies.isEmpty {
            newSections.append(Section(title: "Phim Mới Cập Nhật", videos: movies))
        }
        
        // 3. Latest TV Series
        if let series = response.latestTvSeries, !series.isEmpty {
            newSections.append(Section(title: "Phim Bộ Mới", videos: series))
        }
        
        // 4. Genres
        if let genres = response.featuresGenreAndMovie {
            for genre in genres {
                if let videos = genre.videos, !videos.isEmpty {
                    newSections.append(Section(title: genre.name, videos: videos))
                }
            }
        }
        
        self.sections = newSections
    }
}
