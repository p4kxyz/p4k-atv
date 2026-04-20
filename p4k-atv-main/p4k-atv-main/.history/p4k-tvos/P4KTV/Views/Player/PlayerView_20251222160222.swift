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
        guard let player = player else { return }
        // Get current index
        let currentIndex = player.currentAudioTrackIndex
        // Get all indexes (VLC returns [Any]?, need to cast)
        if let tracks = player.audioTrackIndexes as? [Int], !tracks.isEmpty {
            // Find next index
            if let currentPos = tracks.firstIndex(of: Int(currentIndex)) {
                let nextPos = (currentPos + 1) % tracks.count
                player.currentAudioTrackIndex = Int32(tracks[nextPos])
            } else {
                player.currentAudioTrackIndex = Int32(tracks[0])
            }
            
            // Show toast/overlay with track name
            if let names = player.audioTrackNames as? [String] {
                 let index = Int(player.currentAudioTrackIndex)
                 if let pos = tracks.firstIndex(of: index), pos < names.count {
                     print("Switched Audio to: \(names[pos])")
                 }
            }
        }
    }
    
    func cycleSubtitles() {
        guard let player = player else { return }
        let currentIndex = player.currentVideoSubTitleIndex
        if let tracks = player.videoSubTitlesIndexes as? [Int], !tracks.isEmpty {
            if let currentPos = tracks.firstIndex(of: Int(currentIndex)) {
                let nextPos = (currentPos + 1) % tracks.count
                player.currentVideoSubTitleIndex = Int32(tracks[nextPos])
            } else {
                player.currentVideoSubTitleIndex = Int32(tracks[0])
            }
            
            if let names = player.videoSubTitlesNames as? [String] {
                 let index = Int(player.currentVideoSubTitleIndex)
                 if let pos = tracks.firstIndex(of: index), pos < names.count {
                     print("Switched Subtitle to: \(names[pos])")
                 }
            }
        }
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
