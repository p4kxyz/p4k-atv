import SwiftUI

struct FavoritesView: View {
    @State private var favorites: [Video] = []
    @State private var isLoading = true
    @ObservedObject var userManager = UserManager.shared
    
    let columns = [
        GridItem(.adaptive(minimum: 200, maximum: 250), spacing: 40)
    ]
    
    var body: some View {
        ScrollView {
            if !userManager.isLoggedIn {
                Text("Vui lòng đăng nhập để xem danh sách yêu thích")
                    .font(.title3)
                    .foregroundColor(.gray)
                    .padding(.top, 100)
            } else if isLoading {
                ProgressView()
                    .padding(.top, 100)
            } else if favorites.isEmpty {
                Text("Chưa có phim yêu thích nào")
                    .font(.title3)
                    .foregroundColor(.gray)
                    .padding(.top, 100)
            } else {
                LazyVGrid(columns: columns, spacing: 40) {
                    ForEach(favorites) { video in
                        NavigationLink(destination: VideoDetailView(video: video)) {
                            VideoCardView(video: video)
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding(50)
            }
        }
        .navigationTitle("Danh Sách Yêu Thích")
        .onAppear(perform: loadFavorites)
    }
    
    func loadFavorites() {
        guard let userId = userManager.currentUser?.userId else {
            isLoading = false
            return
        }
        
        let params = ["user_id": userId, "page": 1] as [String : Any]
        NetworkManager.shared.request(endpoint: "favorite", parameters: params) { (result: Result<[Video], Error>) in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let data):
                    self.favorites = data
                case .failure(let error):
                    print("Error loading favorites: \(error)")
                }
            }
        }
    }
}
