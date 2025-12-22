import SwiftUI

struct ContentView: View {
    @StateObject private var navigationManager = NavigationManager()
    @State private var selectedTab: Tab = .home
    
    enum Tab {
        case home, search, livetv, favorites, profile
    }
    
    var body: some View {
        NavigationView {
            TabView(selection: $selectedTab) {
                HomeView()
                    .tabItem {
                        Label("Trang Chủ", systemImage: "house.fill")
                    }
                    .tag(Tab.home)
                
                SearchView()
                    .tabItem {
                        Label("Tìm Kiếm", systemImage: "magnifyingglass")
                    }
                    .tag(Tab.search)
                
                LiveTvView()
                    .tabItem {
                        Label("Truyền Hình", systemImage: "tv.fill")
                    }
                    .tag(Tab.livetv)
                
                FavoritesView()
                    .tabItem {
                        Label("Yêu Thích", systemImage: "heart.fill")
                    }
                    .tag(Tab.favorites)
                
                ProfileView()
                    .tabItem {
                        Label("Tài Khoản", systemImage: "person.fill")
                    }
                    .tag(Tab.profile)
            }
        }
        .environmentObject(navigationManager)
    }
}
