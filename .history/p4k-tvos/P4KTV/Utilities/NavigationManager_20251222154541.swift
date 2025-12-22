import Foundation

class NavigationManager: ObservableObject {
    @Published var selectedTab: String = "home"
    @Published var currentVideo: Video?
    @Published var isPlayerActive: Bool = false
}
