import SwiftUI

struct VideoDetailView: View {
    let video: Video
    @State private var isPlayerPresented = false
    
    var body: some View {
        ZStack {
            // Background Backdrop
            AsyncImage(url: URL(string: video.posterUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .blur(radius: 20) // Blur effect for background
            } placeholder: {
                Color.black
            }
            .edgesIgnoringSafeArea(.all)
            .overlay(Color.black.opacity(0.6))
            
            HStack(alignment: .top, spacing: 50) {
                // Poster
                AsyncImage(url: URL(string: video.posterUrl ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                } placeholder: {
                    Color.gray
                }
                .frame(width: 400, height: 600)
                .cornerRadius(20)
                .shadow(radius: 20)
                
                // Info
                VStack(alignment: .leading, spacing: 20) {
                    Text(video.title)
                        .font(.system(size: 60, weight: .bold))
                        .foregroundColor(.white)
                    
                    HStack(spacing: 20) {
                        if let date = video.releaseDate {
                            Text(date)
                                .padding(8)
                                .background(Color.white.opacity(0.2))
                                .cornerRadius(5)
                        }
                        
                        if let rating = video.rating {
                            HStack {
                                Image(systemName: "star.fill")
                                    .foregroundColor(.yellow)
                                Text(rating)
                            }
                        }
                        
                        Text("4K HDR")
                            .padding(8)
                            .background(Color.yellow)
                            .foregroundColor(.black)
                            .cornerRadius(5)
                            .bold()
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    
                    Text(video.description ?? "Không có mô tả.")
                        .font(.body)
                        .foregroundColor(.white.opacity(0.9))
                        .lineLimit(6)
                    
                    HStack(spacing: 30) {
                        Button(action: {
                            isPlayerPresented = true
                        }) {
                            HStack {
                                Image(systemName: "play.fill")
                                Text("Xem Phim")
                            }
                            .padding()
                        }
                        .fullScreenCover(isPresented: $isPlayerPresented) {
                            if let urlString = video.videoUrl, let url = URL(string: urlString) {
                                PlayerView(videoUrl: url)
                                    .edgesIgnoringSafeArea(.all)
                            } else {
                                Text("Lỗi: Không có link phim")
                            }
                        }
                        
                        Button(action: {
                            addToFavorites()
                        }) {
                            HStack {
                                Image(systemName: isFavorite ? "checkmark" : "plus")
                                Text(isFavorite ? "Đã thêm" : "Danh sách")
                            }
                            .padding()
                        }
                    }
                    .padding(.top, 30)
                    
                    Spacer()
                }
            }
            .padding(50)
        }
        .onAppear(perform: checkFavoriteStatus)
    }
    
    @State private var isFavorite = false
    
    func addToFavorites() {
        guard let userId = UserManager.shared.currentUser?.userId else { return }
        isFavorite.toggle()
        
        // API call placeholder
        let params = ["user_id": userId, "movie_id": video.id] as [String : Any]
        // NetworkManager.shared.request(...) 
    }
    
    func checkFavoriteStatus() {
        // Check status logic
    }
}
