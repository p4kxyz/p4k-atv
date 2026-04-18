import SwiftUI

struct GridView: View {
    let title: String
    let videos: [Video]
    
    let columns = [
        GridItem(.adaptive(minimum: 300, maximum: 400), spacing: 40)
    ]
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 40) {
                ForEach(videos) { video in
                    NavigationLink(destination: VideoDetailView(video: video)) {
                        VideoCardView(video: video)
                    }
                    .buttonStyle(.card)
                }
            }
            .padding(50)
        }
        .navigationTitle(title)
    }
}
