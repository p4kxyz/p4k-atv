import SwiftUI

struct LiveTvView: View {
    @State private var channels: [Video] = []
    @State private var isLoading = true
    
    let columns = [
        GridItem(.adaptive(minimum: 250, maximum: 300), spacing: 30)
    ]
    
    var body: some View {
        ScrollView {
            if isLoading {
                ProgressView("Đang tải kênh TV...")
                    .padding(.top, 100)
            } else {
                LazyVGrid(columns: columns, spacing: 30) {
                    ForEach(channels) { channel in
                        NavigationLink(destination: PlayerView(videoUrl: URL(string: channel.videoUrl ?? "")!, videoId: channel.id, videoTitle: channel.title).edgesIgnoringSafeArea(.all)) {
                            VStack {
                                AsyncImage(url: URL(string: channel.posterUrl ?? "")) { image in
                                    image.resizable().aspectRatio(contentMode: .fit)
                                } placeholder: {
                                    Color.gray.opacity(0.3)
                                }
                                .frame(height: 150)
                                .cornerRadius(10)
                                
                                Text(channel.title)
                                    .font(.caption)
                                    .lineLimit(1)
                            }
                            .padding()
                            .background(Color.white.opacity(0.05))
                            .cornerRadius(15)
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding(50)
            }
        }
        .navigationTitle("Truyền Hình Trực Tuyến")
        .onAppear(perform: loadChannels)
    }
    
    func loadChannels() {
        NetworkManager.shared.request(endpoint: "all_tv_channel_by_category") { (result: Result<[Video], Error>) in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let data):
                    self.channels = data
                case .failure(let error):
                    print("Error loading TV: \(error)")
                }
            }
        }
    }
}
