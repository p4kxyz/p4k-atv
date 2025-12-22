import SwiftUI

struct HeroBannerView: View {
    let video: Video
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background Image
            AsyncImage(url: URL(string: video.posterUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.gray
            }
            .frame(height: 500)
            .clipped()
            .overlay(
                LinearGradient(
                    gradient: Gradient(colors: [.clear, .black.opacity(0.8)]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            
            // Content
            VStack(alignment: .leading, spacing: 10) {
                Text(video.title)
                    .font(.system(size: 60, weight: .bold))
                    .foregroundColor(.white)
                
                if let desc = video.description {
                    Text(desc)
                        .font(.body)
                        .foregroundColor(.white.opacity(0.8))
                        .lineLimit(3)
                        .frame(maxWidth: 800, alignment: .leading)
                }
                
                HStack(spacing: 20) {
                    NavigationLink(destination: VideoDetailView(video: video)) {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("Xem ngay")
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.card)
                    
                    NavigationLink(destination: VideoDetailView(video: video)) {
                        Text("Chi tiết")
                    }
                    .buttonStyle(.card)
                }
                .padding(.top, 20)
            }
            .padding(50)
        }
    }
}
