import Foundation
import UIKit
import QuartzCore
import ComposeApp

/// Metal-backed UIView required by Nuvio's MPVKit (`gpu-next` + `MoltenVK` + `VideoToolbox`)
final class NaphMetalVideoView: UIView {
    override class var layerClass: AnyClass {
        return CAMetalLayer.self
    }

    var metalLayer: CAMetalLayer {
        return self.layer as! CAMetalLayer
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isUserInteractionEnabled = false
        metalLayer.contentsScale = UIScreen.main.scale
        metalLayer.framebufferOnly = true
        metalLayer.pixelFormat = .bgra8Unorm
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        metalLayer.frame = bounds
        metalLayer.drawableSize = CGSize(
            width: max(1, bounds.width * metalLayer.contentsScale),
            height: max(1, bounds.height * metalLayer.contentsScale)
        )
    }
}

/// Real Nuvio MPVKit (`libmpv` + `MoltenVK` + `VideoToolbox` + `libass` + `libdovi`) iOS Player Controller
/// Plays MKV, HEVC, Dolby Vision, HDR10, AV1, TrueHD, EAC3, DTS, ASS/SSA subtitles, and local P2P streams natively on iPhone!
final class NaphMpvSurfaceController: NSObject, IosMpvSurfaceController {
    private let metalView = NaphMetalVideoView(frame: UIScreen.main.bounds)
    private var mpv: OpaquePointer?
    private let queue = DispatchQueue(label: "com.naph.ios.mpv", qos: .userInitiated)
    private var isInitialized = false
    private var pendingStreamUrl: String?
    private var pendingPlaying: Bool = true

    var view: UIView {
        return metalView
    }

    override init() {
        super.init()
        setupMpv()
    }

    private func setupMpv() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            guard let ctx = mpv_create() else { return }

            var layerPtr = Unmanaged.passUnretained(self.metalView.metalLayer).toOpaque()
            mpv_set_option(ctx, "wid", MPV_FORMAT_INT64, &layerPtr)

            // NuvioMedia/MPVKit iOS Metal / MoltenVK + VideoToolbox hardware decoding configuration
            mpv_set_option_string(ctx, "vo", "gpu-next")
            mpv_set_option_string(ctx, "gpu-api", "vulkan")
            mpv_set_option_string(ctx, "gpu-context", "moltenvk")
            mpv_set_option_string(ctx, "hwdec", "videotoolbox")
            mpv_set_option_string(ctx, "vd-lavc-software-fallback", "yes")
            mpv_set_option_string(ctx, "keep-open", "yes")
            mpv_set_option_string(ctx, "idle", "yes")
            mpv_set_option_string(ctx, "osc", "no")
            mpv_set_option_string(ctx, "osd-level", "0")
            mpv_set_option_string(ctx, "subs-with-matching-audio", "yes")
            mpv_set_option_string(ctx, "sub-auto", "fuzzy")
            mpv_set_option_string(ctx, "cache", "yes")
            mpv_set_option_string(ctx, "demuxer-max-bytes", "200M")
            mpv_set_option_string(ctx, "demuxer-readahead-secs", "45")
            mpv_set_option_string(ctx, "force-seekable", "yes")
            mpv_set_option_string(ctx, "volume-max", "200")

            if mpv_initialize(ctx) < 0 {
                // Fallback to classic gpu vo if gpu-next fails on older GPUs
                mpv_terminate_destroy(ctx)
                guard let fallbackCtx = mpv_create() else { return }
                var fbLayerPtr = Unmanaged.passUnretained(self.metalView.metalLayer).toOpaque()
                mpv_set_option(fallbackCtx, "wid", MPV_FORMAT_INT64, &fbLayerPtr)
                mpv_set_option_string(fallbackCtx, "vo", "gpu")
                mpv_set_option_string(fallbackCtx, "gpu-api", "vulkan")
                mpv_set_option_string(fallbackCtx, "gpu-context", "moltenvk")
                mpv_set_option_string(fallbackCtx, "hwdec", "videotoolbox")
                mpv_set_option_string(fallbackCtx, "keep-open", "yes")
                mpv_set_option_string(fallbackCtx, "idle", "yes")
                mpv_set_option_string(fallbackCtx, "cache", "yes")
                if mpv_initialize(fallbackCtx) >= 0 {
                    self.mpv = fallbackCtx
                    self.isInitialized = true
                } else {
                    mpv_terminate_destroy(fallbackCtx)
                    return
                }
            } else {
                self.mpv = ctx
                self.isInitialized = true
            }

            if let url = self.pendingStreamUrl {
                self.pendingStreamUrl = nil
                self.loadStream(url: url)
                self.setPlaying(playing: self.pendingPlaying)
            }
        }
    }

    private func sendCommand(_ args: [String]) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            var cArgs: [UnsafeMutablePointer<CChar>?] = args.map { strdup($0) }
            cArgs.append(nil)
            cArgs.withUnsafeMutableBufferPointer { buf in
                var constPtrs: [UnsafePointer<CChar>?] = buf.map { UnsafePointer($0) }
                constPtrs.withUnsafeMutableBufferPointer { cBuf in
                    _ = mpv_command(ctx, cBuf.baseAddress)
                }
            }
            for ptr in cArgs {
                if let p = ptr { free(p) }
            }
        }
    }

    func loadStream(url: String) {
        if !isInitialized {
            pendingStreamUrl = url
            return
        }
        sendCommand(["loadfile", url, "replace"])
    }

    func setPlaying(playing: Bool) {
        pendingPlaying = playing
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            mpv_set_property_string(ctx, "pause", playing ? "no" : "yes")
        }
    }

    func seekToMs(ms: Int64) {
        let sec = Double(ms) / 1000.0
        sendCommand(["seek", String(format: "%.3f", sec), "absolute"])
    }

    func setPlaybackSpeed(speed: Float) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            mpv_set_property_string(ctx, "speed", String(format: "%.2f", speed))
        }
    }

    func setVolume(volume: Float) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            let pct = Int(max(0, min(150, volume * 100.0)))
            mpv_set_property_string(ctx, "volume", "\(pct)")
        }
    }

    func setAspectMode(mode: String) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            switch mode.lowercased() {
            case "fill":
                mpv_set_property_string(ctx, "keepaspect", "yes")
                mpv_set_property_string(ctx, "video-aspect-override", "-1")
                mpv_set_property_string(ctx, "panscan", "1.0")
            case "16:9":
                mpv_set_property_string(ctx, "keepaspect", "yes")
                mpv_set_property_string(ctx, "panscan", "0.0")
                mpv_set_property_string(ctx, "video-aspect-override", "16:9")
            case "stretch":
                mpv_set_property_string(ctx, "keepaspect", "no")
                mpv_set_property_string(ctx, "panscan", "0.0")
                mpv_set_property_string(ctx, "video-aspect-override", "-1")
            default:
                mpv_set_property_string(ctx, "keepaspect", "yes")
                mpv_set_property_string(ctx, "panscan", "0.0")
                mpv_set_property_string(ctx, "video-aspect-override", "-1")
            }
        }
    }

    func addSubtitleTrack(url: String) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            mpv_set_property_string(ctx, "sub-visibility", "yes")
        }
        sendCommand(["sub-add", url, "select"])
    }

    func setSubtitlesEnabled(enabled: Bool) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            mpv_set_property_string(ctx, "sub-visibility", enabled ? "yes" : "no")
        }
    }

    func setSubtitleDelayMs(delayMs: Int64) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            let sec = Double(delayMs) / 1000.0
            mpv_set_property_string(ctx, "sub-delay", String(format: "%.3f", sec))
        }
    }

    func setSubtitleFontSize(sizeSp: Float) {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            let mpvSize = Int(max(24, min(90, sizeSp * 2.2)))
            mpv_set_property_string(ctx, "sub-font-size", "\(mpvSize)")
        }
    }

    func cycleAudioTrack() {
        sendCommand(["cycle", "audio"])
    }

    func cycleEmbeddedSubtitleTrack() {
        queue.async { [weak self] in
            guard let ctx = self?.mpv else { return }
            mpv_set_property_string(ctx, "sub-visibility", "yes")
        }
        sendCommand(["cycle", "sub"])
    }

    func currentPositionMs() -> Int64 {
        guard let ctx = mpv else { return 0 }
        var pos: Double = 0
        if mpv_get_property(ctx, "time-pos", MPV_FORMAT_DOUBLE, &pos) >= 0, pos.isFinite, pos >= 0 {
            return Int64(pos * 1000.0)
        }
        return 0
    }

    func currentDurationMs() -> Int64 {
        guard let ctx = mpv else { return 0 }
        var dur: Double = 0
        if mpv_get_property(ctx, "duration", MPV_FORMAT_DOUBLE, &dur) >= 0, dur.isFinite, dur > 0 {
            return Int64(dur * 1000.0)
        }
        return 0
    }

    func destroy() {
        let ctx = mpv
        mpv = nil
        isInitialized = false
        if let c = ctx {
            queue.async {
                mpv_terminate_destroy(c)
            }
        }
    }
}
