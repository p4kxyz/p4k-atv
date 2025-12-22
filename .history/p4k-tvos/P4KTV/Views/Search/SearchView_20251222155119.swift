import SwiftUI

struct SearchView: View {
    @State private var searchText = ""
    @State private var results: [Video] = []
    @State private var isSearching = false
    
    let columns = [
        GridItem(.adaptive(minimum: 300, maximum: 400), spacing: 40)
    ]
    
    var body: some View {
        VStack {
            HStack {
                Image(systemName: "magnifyingglass")
                TextField("Tìm kiếm phim, diễn viên...", text: $searchText)
                    .submitLabel(.search)
                    .onSubmit {
                        performSearch()
                    }
            }
            .padding()
            .background(Color.white.opacity(0.1))
            .cornerRadius(10)
            .padding(.horizontal, 50)
            .padding(.top, 20)
            
            if isSearching {
                ProgressView()
                    .padding(.top, 50)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 40) {
                        ForEach(results) { video in
                            NavigationLink(destination: VideoDetailView(video: video)) {
                                VideoCardView(video: video)
                            }
                            .buttonStyle(.card)
                        }
                    }
                    .padding(50)
                }
            }
        }
        .navigationTitle("Tìm kiếm")
    }
    
    func performSearch() {
        isSearching = true
        // Mock search delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            // Mock results
            self.results = [
                Video(id: "10", title: "Kết quả 1: \(searchText)", description: "", posterUrl: "https://via.placeholder.com/300x450", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: ""),
                Video(id: "11", title: "Kết quả 2: \(searchText)", description: "", posterUrl: "https://via.placeholder.com/300x450", videoUrl: "", videoType: "", category: "", releaseDate: "", rating: "")
            ]
            isSearching = false
        }
    }
}
