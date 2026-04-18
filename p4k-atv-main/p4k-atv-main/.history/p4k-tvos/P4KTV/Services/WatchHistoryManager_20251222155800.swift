import Foundation

struct WatchProgress: Codable {
    let videoId: String
    let position: Double // Seconds
    let duration: Double
    let timestamp: Date
}

class WatchHistoryManager {
    static let shared = WatchHistoryManager()
    private let key = "watch_history"
    
    func saveProgress(videoId: String, position: Double, duration: Double) {
        var history = loadHistory()
        let progress = WatchProgress(videoId: videoId, position: position, duration: duration, timestamp: Date())
        history[videoId] = progress
        
        if let data = try? JSONEncoder().encode(history) {
            UserDefaults.standard.set(data, forKey: key)
        }
        
        // Sync with API if logged in (Placeholder)
        syncWithApi(videoId: videoId, position: position)
    }
    
    func getProgress(videoId: String) -> Double {
        let history = loadHistory()
        return history[videoId]?.position ?? 0
    }
    
    private func loadHistory() -> [String: WatchProgress] {
        if let data = UserDefaults.standard.data(forKey: key),
           let history = try? JSONDecoder().decode([String: WatchProgress].self, from: data) {
            return history
        }
        return [:]
    }
    
    private func syncWithApi(videoId: String, position: Double) {
        guard let userId = UserManager.shared.currentUser?.userId else { return }
        // NetworkManager.shared.request(...)
    }
}
