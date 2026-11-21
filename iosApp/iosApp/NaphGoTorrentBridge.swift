import Foundation
import ComposeApp

/// Real On-Device iOS Sequential BitTorrent Engine Bridge
/// Connects Kotlin Multiplatform's `PlatformP2PStreamer` to our embedded Go `anacrolix/torrent` C archive (`libnaph_p2p_ios.a`)
/// so every magnet / infoHash torrent stream plays directly on iPhone over `http://127.0.0.1:<port>/stream/...` with zero external server!
final class NaphGoTorrentBridge: NSObject, IosNativeTorrentEngineBridge {
    static let shared = NaphGoTorrentBridge()

    private var activeUrls: [String: String] = [:]

    static func register() {
        PlatformP2PStreamer.shared.nativeBridge = NaphGoTorrentBridge.shared
    }

    func startSequentialMagnetStream(
        magnetUri: String,
        infoHash: String,
        fileIndex: Int32,
        saveDirectory: String,
        maxDownloadKbps: Int32,
        maxUploadKbps: Int32,
        enableDht: Bool
    ) -> String? {
        let cleanHash = infoHash.lowercased()
        let resultPtr = magnetUri.withCString { magC in
            cleanHash.withCString { hashC in
                saveDirectory.withCString { dirC in
                    NaphP2PStartStream(
                        UnsafeMutablePointer(mutating: magC),
                        UnsafeMutablePointer(mutating: hashC),
                        fileIndex,
                        UnsafeMutablePointer(mutating: dirC)
                    )
                }
            }
        }

        guard let ptr = resultPtr else { return nil }
        let localUrl = String(cString: ptr)
        NaphP2PFreeString(ptr)

        if localUrl.isEmpty { return nil }
        activeUrls[cleanHash] = localUrl
        return localUrl
    }

    func querySwarmTelemetry(infoHash: String) -> P2PStreamTelemetry? {
        let cleanHash = infoHash.lowercased()
        guard let url = activeUrls[cleanHash] else { return nil }

        let peers = cleanHash.withCString { NaphP2PGetPeers(UnsafeMutablePointer(mutating: $0)) }
        let seeds = cleanHash.withCString { NaphP2PGetSeeds(UnsafeMutablePointer(mutating: $0)) }
        let speed = cleanHash.withCString { NaphP2PGetSpeedBps(UnsafeMutablePointer(mutating: $0)) }

        return P2PStreamTelemetry(
            infoHash: cleanHash,
            fileName: "iOS P2P Stream",
            localStreamUrl: url,
            isReadyForPlayback: true,
            bufferProgressPercent: 100.0,
            downloadSpeedBytesPerSec: speed,
            uploadSpeedBytesPerSec: 0,
            connectedPeers: peers,
            totalSeeds: seeds,
            downloadedBytes: 0,
            totalFileBytes: 0,
            headerPiecesReady: true
        )
    }

    func stopAndRemoveStream(infoHash: String, deleteFiles: Bool) -> Int64 {
        let cleanHash = infoHash.lowercased()
        activeUrls.removeValue(forKey: cleanHash)
        return cleanHash.withCString {
            NaphP2PStopStream(UnsafeMutablePointer(mutating: $0), deleteFiles ? 1 : 0)
        }
    }
}
