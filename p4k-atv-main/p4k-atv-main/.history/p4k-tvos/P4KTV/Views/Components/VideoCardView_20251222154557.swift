import SwiftUI

struct VideoCardView: View {
    let video: Video
    @FocusState private var isFocused: Bool
    
    var body: some View {
        VStack {
            AsyncImage(url: URL(string: video.posterUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.gray
            }
            .frame(width: 200, height: 300)
            .cornerRadius(10)
            .scaleEffect(isFocused ? 1.1 : 1.0)
            .animation(.spring(), value: isFocused)
            
            Text(video.title)
                .font(.caption)
                .lineLimit(1)
                .opacity(isFocused ? 1 : 0.6)
        }
        .focusable(true)
        .focused($isFocused)
    }
}
