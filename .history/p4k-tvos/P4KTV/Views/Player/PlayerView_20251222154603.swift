import SwiftUI
import AVKit

struct PlayerView: UIViewControllerRepresentable {
    let videoUrl: URL
    
    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        let player = AVPlayer(url: videoUrl)
        controller.player = player
        player.play()
        return controller
    }
    
    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {
        // Update logic if needed
    }
}
