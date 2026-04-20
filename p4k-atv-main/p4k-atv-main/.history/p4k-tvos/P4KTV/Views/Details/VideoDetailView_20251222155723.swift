import SwiftUI

struct VideoDetailView: View {
    let video: Video
    @State private var details: MovieDetails?
    @State private var isLoading = true
    @State private var isPlayerPresented = false
    @State private var selectedSeasonIndex = 0
    @State private var selectedEpisode: Episode?
    
    var body: some View {
        ZStack {
            // Background Backdrop
            AsyncImage(url: URL(string: video.posterUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .blur(radius: 20)
            } placeholder: {
                Color.black
            }
            .edgesIgnoringSafeArea(.all)
            .overlay(Color.black.opacity(0.6))
            
            if isLoading {
                ProgressView("Đang tải thông tin...")
            } else if let details = details {
                ScrollView {
                    VStack(alignment: .leading, spacing: 40) {
                        // Top Section: Poster & Info
                        HStack(alignment: .top, spacing: 50) {
                            // Poster
                            AsyncImage(url: URL(string: details.posterUrl ?? "")) { image in
                                image.resizable().aspectRatio(contentMode: .fit)
                            } placeholder: { Color.gray }
                            .frame(width: 350, height: 525)
                            .cornerRadius(15)
                            .shadow(radius: 10)
                            
                            // Info
                            VStack(alignment: .leading, spacing: 20) {
                                Text(details.title)
                                    .font(.system(size: 50, weight: .bold))
                                    .foregroundColor(.white)
                                
                                HStack(spacing: 20) {
                                    if let date = details.releaseDate {
                                        Text(date).padding(8).background(Color.white.opacity(0.2)).cornerRadius(5)
                                    }
                                    if let rating = details.rating {
                                        HStack { Image(systemName: "star.fill").foregroundColor(.yellow); Text(rating) }
                                    }
                                    Text("4K HDR").padding(8).background(Color.yellow).foregroundColor(.black).cornerRadius(5).bold()
                                }
                                .font(.headline)
                                .foregroundColor(.white)
                                
                                Text(details.description ?? "Không có mô tả.")
                                    .font(.body)
                                    .foregroundColor(.white.opacity(0.9))
                                    .lineLimit(6)
                                
                                // Action Buttons
                                HStack(spacing: 30) {
                                    if let seasons = details.season, !seasons.isEmpty {
                                        // TV Series: Play First Episode or Resume
                                        Button(action: {
                                            if let firstEp = seasons.first?.episodes?.first {
                                                playEpisode(firstEp)
                                            }
                                        }) {
                                            HStack { Image(systemName: "play.fill"); Text("Xem Tập 1") }.padding()
                                        }
                                    } else {
                                        // Movie: Play
                                        Button(action: {
                                            isPlayerPresented = true
                                        }) {
                                            HStack { Image(systemName: "play.fill"); Text("Xem Phim") }.padding()
                                        }
                                        .fullScreenCover(isPresented: $isPlayerPresented) {
                                            if let urlString = details.streamUrl ?? details.videoUrl, let url = URL(string: urlString) {
                                                PlayerView(videoUrl: url).edgesIgnoringSafeArea(.all)
                                            } else {
                                                Text("Lỗi: Không có link phim")
                                            }
                                        }
                                    }
                                    
                                    Button(action: addToFavorites) {
                                        HStack {
                                            Image(systemName: isFavorite ? "checkmark" : "plus")
                                            Text(isFavorite ? "Đã thêm" : "Danh sách")
                                        }
                                        .padding()
                                    }
                                }
                                .padding(.top, 20)
                            }
                        }
                        
                        // TV Series: Seasons & Episodes
                        if let seasons = details.season, !seasons.isEmpty {
                            VStack(alignment: .leading, spacing: 20) {
                                Text("Danh Sách Tập").font(.title2).bold()
                                
                                // Season Selector
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 20) {
                                        ForEach(0..<seasons.count, id: \.self) { index in
                                            Button(action: { selectedSeasonIndex = index }) {
                                                Text(seasons[index].name)
                                                    .padding()
                                                    .background(selectedSeasonIndex == index ? Color.yellow : Color.white.opacity(0.1))
                                                    .foregroundColor(selectedSeasonIndex == index ? .black : .white)
                                                    .cornerRadius(10)
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }
                                
                                // Episodes Grid
                                if let episodes = seasons[selectedSeasonIndex].episodes {
                                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 250))], spacing: 20) {
                                        ForEach(episodes) { episode in
                                            Button(action: { playEpisode(episode) }) {
                                                VStack {
                                                    AsyncImage(url: URL(string: episode.imageUrl ?? "")) { image in
                                                        image.resizable().aspectRatio(contentMode: .fill)
                                                    } placeholder: {
                                                        ZStack {
                                                            Color.gray.opacity(0.3)
                                                            Image(systemName: "play.circle")
                                                        }
                                                    }
                                                    .frame(height: 140)
                                                    .cornerRadius(10)
                                                    
                                                    Text(episode.title)
                                                        .lineLimit(1)
                                                        .font(.caption)
                                                }
                                                .padding()
                                                .background(Color.white.opacity(0.05))
                                                .cornerRadius(15)
                                            }
                                            .buttonStyle(.card)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Related Movies
                        if let related = details.relatedMovies, !related.isEmpty {
                            VStack(alignment: .leading) {
                                Text("Có Thể Bạn Thích").font(.title2).bold()
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 30) {
                                        ForEach(related) { item in
                                            NavigationLink(destination: VideoDetailView(video: item)) {
                                                VideoCardView(video: item)
                                            }
                                            .buttonStyle(.card)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(50)
                }
            }
        }
        .onAppear(perform: loadDetails)
        .fullScreenCover(item: $selectedEpisode) { episode in
            if let urlString = episode.streamUrl ?? episode.fileUrl, let url = URL(string: urlString) {
                PlayerView(videoUrl: url).edgesIgnoringSafeArea(.all)
            }
        }
    }
    
    func loadDetails() {
        // Determine type: "movie" or "tvseries"
        // If video.category is not set, guess or default to movie
        let type = video.category == "tvseries" ? "tvseries" : "movie"
        
        NetworkManager.shared.fetchMovieDetails(id: video.id, type: type) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let data):
                    self.details = data
                case .failure(let error):
                    print("Error loading details: \(error)")
                }
            }
        }
        checkFavoriteStatus()
    }
    
    func playEpisode(_ episode: Episode) {
        self.selectedEpisode = episode
    }
    
    @State private var isFavorite = false
    
    func addToFavorites() {
        guard let userId = UserManager.shared.currentUser?.userId else { return }
        isFavorite.toggle()
        let params = ["user_id": userId, "movie_id": video.id] as [String : Any]
        NetworkManager.shared.request(endpoint: "add_favorite", method: "POST", parameters: params) { (result: Result<String, Error>) in }
    }
    
    func checkFavoriteStatus() {
        // Implement check logic
    }
}
