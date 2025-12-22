import SwiftUI

struct ContentView: View {
    @StateObject private var navigationManager = NavigationManager()
    
    var body: some View {
        NavigationView {
            HomeView()
        }
        .environmentObject(navigationManager)
    }
}
