import SwiftUI
import TVVLCKit

struct PlayerView: View {
    let videoUrl: URL
    let videoId: String? // Optional for history
    let nextEpisode: Episode? // Optional for next episode
    
    @State private var player: VLCMediaPlayer?
    @State private var isOverlayVisible = false
    @State private var audioTracks: [String] = []
    @State private var subtitleTracks: [String] = []
    @Environment(\.presentationMode) var presentationMode
    
    init(videoUrl: URL, videoId: String? = nil, nextEpisode: Episode? = nil) {
        self.videoUrl = videoUrl
        self.videoId = videoId
        self.nextEpisode = nextEpisode
    }
    
    var body: some View {
        ZStack {
            VLCPlayerWrapper(videoUrl: videoUrl, player: $player, onTimeChanged: { time in
                if let vid = videoId {
                    WatchHistoryManager.shared.saveProgress(videoId: vid, position: Double(time.intValue)/1000.0, duration: 0)
                }
            })
            .edgesIgnoringSafeArea(.all)
            .onTapGesture {
                withAnimation { isOverlayVisible.toggle() }
            }
            
            if isOverlayVisible {
                VStack {
                    HStack {
                        Button(action: { presentationMode.wrappedValue.dismiss() }) {
                            Image(systemName: "xmark.circle.fill").font(.title)
                        }
                        Spacer()
                    }
                    .padding()
                    
                    Spacer()
                    
                    // Controls
                    HStack(spacing: 40) {
                        Button(action: cycleAudio) {
                            VStack { Image(systemName: "waveform"); Text("Audio") }
                        }
                        
                        Button(action: cycleSubtitles) {
                            VStack { Image(systemName: "captions.bubble"); Text("Subtitles") }
                        }
                        
                        if let next = nextEpisode {
                            Button(action: { playNext(next) }) {
                                VStack { Image(systemName: "forward.end.fill"); Text("Next Ep") }
                            }
                        }
                    }
                    .padding(30)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(20)
                    .padding(.bottom, 50)
                }
            }
        }
    }
    
    func cycleAudio() {
        // Logic to cycle audio tracks
        // player?.currentAudioTrackIndex = ...
    }
    
    func cycleSubtitles() {
        // Logic to cycle subtitles
        // player?.currentVideoSubTitleIndex = ...
    }
    
    func playNext(_ episode: Episode) {
        // Logic to switch video
    }
}

struct VLCPlayerWrapper: UIViewControllerRepresentable {
    let videoUrl: URL
    @Binding var player: VLCMediaPlayer?
    var onTimeChanged: ((VLCTime) -> Void)?
    
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = UIViewController()
        let vlcPlayer = VLCMediaPlayer()
        vlcPlayer.drawable = controller.view
        
        let media = VLCMedia(url: videoUrl)
        media.addOptions(["--network-caching=1500", "--clock-jitter=0", "--clock-synchro=0"])
        
        vlcPlayer.media = media
        vlcPlayer.delegate = context.coordinator
        vlcPlayer.play()
        
        DispatchQueue.main.async {
            self.player = vlcPlayer
        }
        
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, VLCMediaPlayerDelegate {
        var parent: VLCPlayerWrapper
        
        init(_ parent: VLCPlayerWrapper) {
            self.parent = parent
        }
        
        func mediaPlayerTimeChanged(_ aNotification: Notification!) {
            if let player = aNotification.object as? VLCMediaPlayer {
                parent.onTimeChanged?(player.time)
            }
        }
    }
}
