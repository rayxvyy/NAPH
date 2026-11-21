import SwiftUI
import AVFoundation
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            .preferredColorScheme(.dark)
    }
}

@main
struct NaphIOSApp: App {
    init() {
        // 1. Configure iOS AVAudioSession for Cinema Playback & AirPlay 2
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .moviePlayback, options: [.allowAirPlay, .allowBluetoothA2DP])
            try session.setActive(true)
        } catch {
            print("NAPH iOS AudioSession warning: \(error)")
        }

        // 2. Register Native iOS Sequential BitTorrent Engine Bridge with shared KMP PlatformP2PStreamer
        NaphIosTorrentEngine.registerWithSharedEngine()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
