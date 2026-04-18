import SwiftUI
// IMPORTANT: You must install 'TVVLCKit' via CocoaPods or Swift Package Manager
// Podfile: pod 'TVVLCKit'
import TVVLCKit

struct PlayerView: UIViewControllerRepresentable {
    let videoUrl: URL
    
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = UIViewController()
        
        // Initialize VLC Player
        let player = VLCMediaPlayer()
        player.drawable = controller.view
        
        // Create Media
        let media = VLCMedia(url: videoUrl)
        
        // Add options for better streaming (similar to ExoPlayer config)
        // Network caching: 1500ms
        media.addOptions([
            "--network-caching=1500",
            "--clock-jitter=0",
            "--clock-synchro=0"
        ])
        
        player.media = media
        player.play()
        
        // Keep reference
        context.coordinator.player = player
        
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Update logic if needed
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    class Coordinator {
        var player: VLCMediaPlayer?
    }
}
