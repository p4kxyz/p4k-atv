import SwiftUI

struct SearchView: View {
    @State private var searchText = ""
    @State private var searchResults: [Video] = []
    @State private var isLoading = false
    
    // Filters
    @State private var selectedGenre = "All"
    @State private var selectedYear = "All"
    let genres = ["All", "Hành Động", "Hài Hước", "Tình Cảm", "Kinh Dị", "Viễn Tưởng"]
    let years = ["All", "2024", "2023", "2022", "2021", "2020"]
    
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
                Picker("Thể loại", selection: $selectedGenre) {
                    ForEach(genres, id: \.self) { genre in
                        Text(genre).tag(genre)
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
    }
    
    func performSearch() {
        guard !searchText.isEmpty else { return }
        isLoading = true
        
        // Add filter params
        var params = ["q": searchText]
        if selectedGenre != "All" { params["genre"] = selectedGenre }
        if selectedYear != "All" { params["year"] = selectedYear }
        
        NetworkManager.shared.request(endpoint: "search", parameters: params) { (result: Result<[Video], Error>) in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let data):
                    self.searchResults = data
                case .failure(let error):
                    print("Search error: \(error)")
                    self.searchResults = []
                }
            }
        }
    }
}
