import SwiftUI

struct SearchView: View {
    @State private var searchText = ""
    @State private var searchResults: [Video] = []
    @State private var isLoading = false
    
    // Filters
    @State private var genres: [Genre] = [Genre(id: "All", name: "Tất Cả")]
    @State private var selectedGenreId = "All"
    @State private var selectedYear = "All"
    
    let years = ["All"] + (1990...2025).map { String($0) }.reversed()
    
    let columns = [
        GridItem(.adaptive(minimum: 200, maximum: 250), spacing: 40)
    ]
    
    var body: some View {
        VStack(spacing: 20) {
            // Search Bar & Filters
            HStack(spacing: 20) {
                TextField("Tìm kiếm phim, diễn viên...", text: $searchText, onCommit: performSearch)
                    .textFieldStyle(PlainTextFieldStyle())
                    .padding(20)
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(15)
                
                Button(action: performSearch) {
                    Image(systemName: "magnifyingglass")
                        .padding()
                }
            }
            .padding(.horizontal, 50)
            .padding(.top, 20)
            
            // Filter Row
            HStack(spacing: 40) {
                Picker("Thể loại", selection: $selectedGenreId) {
                    ForEach(genres) { genre in
                        Text(genre.name).tag(genre.id)
                    }
                }
                .pickerStyle(.menu)
                .frame(width: 250)
                
                Picker("Năm", selection: $selectedYear) {
                    ForEach(years, id: \.self) { year in
                        Text(year).tag(year)
                    }
                }
                .pickerStyle(.menu)
                .frame(width: 150)
            }
            .padding(.horizontal, 50)
            
            // Results
            ScrollView {
                if isLoading {
                    ProgressView("Đang tìm kiếm...")
                        .padding(.top, 50)
                } else if searchResults.isEmpty && !searchText.isEmpty {
                    Text("Không tìm thấy kết quả nào.")
                        .foregroundColor(.gray)
                        .padding(.top, 50)
                } else {
                    LazyVGrid(columns: columns, spacing: 40) {
                        ForEach(searchResults) { video in
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
        .navigationTitle("Tìm Kiếm")
        .onAppear(perform: loadGenres)
    }
    
    func loadGenres() {
        NetworkManager.shared.fetchGenres { result in
            DispatchQueue.main.async {
                switch result {
                case .success(let data):
                    self.genres = [Genre(id: "All", name: "Tất Cả")] + data
                case .failure(let error):
                    print("Error loading genres: \(error)")
                }
            }
        }
    }
    
    func performSearch() {
        guard !searchText.isEmpty else { return }
        isLoading = true
        
        NetworkManager.shared.search(query: searchText, genreId: selectedGenreId, year: selectedYear) { (result: Result<SearchResponse, Error>) in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let data):
                    var combined: [Video] = []
                    if let movies = data.movie { combined.append(contentsOf: movies) }
                    if let series = data.tvseries { combined.append(contentsOf: series) }
                    self.searchResults = combined
                case .failure(let error):
                    print("Search error: \(error)")
                    self.searchResults = []
                }
            }
        }
    }
}
