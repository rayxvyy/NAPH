import Foundation
import UIKit
import QuartzCore
import ComposeApp

/// Metal-backed UIView for MPVKit (`gpu` + `MoltenVK` + `VideoToolbox` hardware acceleration)
/// Properly synchronizes `contentsScale` & `drawableSize` across portrait <-> landscape rotations.
final class NaphMetalVideoView: UIView {
    override class var layerClass: AnyClass {
        return CAMetalLayer.self
    }

    var metalLayer: CAMetalLayer {
        return self.layer as! CAMetalLayer
    }

    var onBoundsResized: ((CGSize) -> Void)?
    private var lastSyncedPixelSize: CGSize = .zero

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isUserInteractionEnabled = false
        clipsToBounds = true
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
        let scale = UIScreen.main.nativeScale
        metalLayer.contentsScale = scale
        metalLayer.framebufferOnly = true
        metalLayer.pixelFormat = .bgra8Unorm
        metalLayer.isOpaque = true
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func syncDrawableSize() {
        guard bounds.width > 1, bounds.height > 1 else { return }
        let scale = window?.screen.nativeScale ?? UIScreen.main.nativeScale
        let pixelSize = CGSize(
            width: floor(bounds.width * scale),
            height: floor(bounds.height * scale)
        )
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        metalLayer.contentsScale = scale
        if metalLayer.drawableSize != pixelSize {
            metalLayer.drawableSize = pixelSize
        }
        CATransaction.commit()

        if lastSyncedPixelSize != pixelSize {
            lastSyncedPixelSize = pixelSize
            onBoundsResized?(pixelSize)
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        syncDrawableSize()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        syncDrawableSize()
    }
}

/// Clipping container view so portrait <-> landscape rotation pins the Metal layer to exact bounds
final class NaphVideoContainerView: UIView {
    let metalView = NaphMetalVideoView(frame: .zero)

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        clipsToBounds = true
        isUserInteractionEnabled = false
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
        metalView.frame = bounds
        addSubview(metalView)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        metalView.frame = bounds
        metalView.syncDrawableSize()
        CATransaction.commit()
    }
}

/// Real Hardware-Accelerated iOS MPVKit Controller (`vo=gpu` + `MoltenVK` + `VideoToolbox` + `libass` + `libdovi`):
/// - Uses `vo=gpu` + `moltenvk` so portrait <-> landscape rotation dynamically rebuilds the swapchain centered (zero bottom-left crop!)
/// - Uses `hwdec=videotoolbox` + `hwdec-codecs=all` for full Apple Silicon hardware decoding & crisp scaling
final class NaphMpvSurfaceController: NSObject, IosMpvSurfaceController {
    private let containerView = NaphVideoContainerView(frame: UIScreen.main.bounds)
    private var metalView: NaphMetalVideoView { containerView.metalView }
    private var mpv: OpaquePointer?
    private let queue = DispatchQueue(label: "com.naph.ios.mpv", qos: .userInitiated)
    private var isInitialized = false
    private var pendingStreamUrl: String?
    private var pendingPlaying: Bool = true
    private var currentAspectOverride: String = "-1"

    var view: UIView {
        return containerView
    }

    override init() {
        super.init()
        metalView.onBoundsResized = { [weak self] _ in
            self?.nudgeViewportResize()
        }
        setupMpv()
    }

    private func nudgeViewportResize() {
        queue.async { [weak self] in
            guard let self = self, let ctx = self.mpv else { return }
            // Nudge libmpv VO to re-query CAMetalLayer drawableSize immediately on portrait/landscape rotation
            mpv_set_property_string(ctx, "video-aspect-override", self.currentAspectOverride)
            mpv_set_property_string(ctx, "video-align-x", "0")
            mpv_set_property_string(ctx, "video-align-y", "0")
        }
    }

    private func setupMpv() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.metalView.syncDrawableSize()
            guard let ctx = mpv_create() else { return }

            var layerPtr = Unmanaged.passUnretained(self.metalView.metalLayer).toOpaque()
            mpv_set_option(ctx, "wid", MPV_FORMAT_INT64, &layerPtr)

            // Use classic `vo=gpu` + `moltenvk` + `videotoolbox` for rock-solid portrait/landscape rotation & Apple hardware decode
            mpv_set_option_string(ctx, "vo", "gpu")
            mpv_set_option_string(ctx, "gpu-api", "vulkan")
            mpv_set_option_string(ctx, "gpu-context", "moltenvk")
            mpv_set_option_string(ctx, "hwdec", "videotoolbox")
            mpv_set_option_string(ctx, "hwdec-codecs", "all")
            mpv_set_option_string(ctx, "vd-lavc-software-fallback", "yes")
            mpv_set_option_string(ctx, "video-sync", "audio")
            mpv_set_option_string(ctx, "scale", "spline36")
            mpv_set_option_string(ctx, "cscale", "spline36")
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
            mpv_set_option_string(ctx, "video-align-x", "0")
            mpv_set_option_string(ctx, "video-align-y", "0")

            if mpv_initialize(ctx) >= 0 {
                self.mpv = ctx
                self.isInitialized = true
            } else {
                mpv_terminate_destroy(ctx)
                return
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
            guard let self = self, let ctx = self.mpv else { return }
            switch mode.lowercased() {
            case "fill":
                self.currentAspectOverride = "-1"
                mpv_set_property_string(ctx, "keepaspect", "yes")
                mpv_set_property_string(ctx, "video-aspect-override", "-1")
                mpv_set_property_string(ctx, "panscan", "1.0")
            case "16:9":
                self.currentAspectOverride = "16:9"
                mpv_set_property_string(ctx, "keepaspect", "yes")
                mpv_set_property_string(ctx, "panscan", "0.0")
                mpv_set_property_string(ctx, "video-aspect-override", "16:9")
            case "stretch":
                self.currentAspectOverride = "-1"
                mpv_set_property_string(ctx, "keepaspect", "no")
                mpv_set_property_string(ctx, "panscan", "0.0")
                mpv_set_property_string(ctx, "video-aspect-override", "-1")
            default:
                self.currentAspectOverride = "-1"
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
