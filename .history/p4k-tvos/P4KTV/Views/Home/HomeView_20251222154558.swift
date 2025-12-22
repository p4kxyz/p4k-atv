import SwiftUI

struct HomeView: View {
    // Dummy Data
    let featuredVideo = Video(
        id: "1",
        title: "Phim 4K Demo",
        description: "Trải nghiệm chất lượng hình ảnh tuyệt đỉnh với Phim 4K trên Apple TV.",
        posterUrl: "https://image.tmdb.org/t/p/original/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg",
        videoUrl: "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        videoType: "mp4",
        category: "movie",
        releaseDate: "2023",
        rating: "9.0"
    )
    
    let sections = [
        Section(title: "Phim Mới Cập Nhật", videos: [
            Video(id: "2", title: "Movie 1", description: "", posterUrl: "https://via.placeholder.com/200x300", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: ""),
            Video(id: "3", title: "Movie 2", description: "", posterUrl: "https://via.placeholder.com/200x300", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: ""),
            Video(id: "4", title: "Movie 3", description: "", posterUrl: "https://via.placeholder.com/200x300", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: "")
        ]),
        Section(title: "Phim Hành Động", videos: [
            Video(id: "5", title: "Action 1", description: "", posterUrl: "https://via.placeholder.com/200x300", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: ""),
            Video(id: "6", title: "Action 2", description: "", posterUrl: "https://via.placeholder.com/200x300", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: "")
        ])
    ]
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 40) {
                HeroBannerView(video: featuredVideo)
                
                ForEach(sections) { section in
                    VStack(alignment: .leading) {
                        Text(section.title)
                            .font(.headline)
                            .padding(.leading, 50)
                        
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 30) {
                                ForEach(section.videos) { video in
                                    VideoCardView(video: video)
                                }
                            }
                            .padding(.horizontal, 50)
                            .padding(.vertical, 20)
                        }
                    }
                }
            }
        }
        .edgesIgnoringSafeArea(.top)
    }
}
