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
        // 1. Configure AVAudioSession for continuous movie playback & AirPlay 2
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .moviePlayback, options: [.allowAirPlay])
            try session.setActive(true)
        } catch {
            print("NAPH audio session error: \(error)")
        }

        // 2. Register Nuvio's MPVKit (`libmpv` + `MoltenVK` + `VideoToolbox` + `libass`) as the iOS Video Player
        IosVideoEngineRegistry.shared.mpvControllerFactory = {
            NaphMpvSurfaceController()
        }

        // 3. Register Real On-Device iOS Sequential BitTorrent Engine (`anacrolix/torrent` HTTP Byte-Range Server)
        NaphGoTorrentBridge.register()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
